package in.xnnyygn.concurrent.transaction;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MyTransactionSystem {
    interface WriteAction {
        void apply(long newVersion);
    }

    static class Transaction {
        static final ThreadLocal<Transaction> current = new ThreadLocal<>();

        static Transaction current() {
            Transaction t = current.get();
            if (t == null) {
                throw new IllegalStateException("no transaction");
            }
            return t;
        }

        final long version;
        final Set<TObject> readSet = new HashSet<>();
        final Map<TObject, WriteAction> writeSet = new HashMap<>();

        Transaction(long version) {
            this.version = version;
        }

        boolean hasWrite() {
            return !writeSet.isEmpty();
        }

        void commit() {
            validateVersion(readSet);
        }

        void commit(long newVersion) {
            validateVersion(writeSet.keySet());
            validateVersion(readSet);
            LinkedList<TObject> lockedTValues = new LinkedList<>();
            try {
                for (TObject tObject : writeSet.keySet()) {
                    if (tObject.tryLock()) {
                        lockedTValues.add(tObject);
                    } else {
                        throw new ConflictException();
                    }
                }
                for (TObject tObject : writeSet.keySet()) {
                    writeSet.get(tObject).apply(newVersion);
                }
            } finally {
                for (TObject tObject : lockedTValues) {
                    tObject.unlock();
                }
            }
        }

        private void validateVersion(Collection<TObject> tObjects) {
            for (TObject tValue : tObjects) {
                if (tValue.hasConflict(version)) {
                    throw new ConflictException();
                }
            }
        }
    }

    static class ConflictException extends RuntimeException {
    }

    static class TValueSet<T> implements WriteAction {
        final TValue<T> tValue;
        final T newValue;

        TValueSet(TValue<T> tValue, T newValue) {
            this.tValue = tValue;
            this.newValue = newValue;
        }

        @Override
        public void apply(long newVersion) {
            tValue.value = newValue;
            tValue.setVersion(newVersion);
        }
    }

    interface TObject {
        boolean hasConflict(long version);

        boolean tryLock();

        void unlock();

        long getVersion();

        void setVersion(long newVersion);
    }

    static abstract class AbstractTObject implements TObject {
        volatile long version = 0;

        void checkConflict(long version) {
            if (hasConflict(version)) {
                throw new ConflictException();
            }
        }

        @Override
        public long getVersion() {
            return version;
        }

        @Override
        public void setVersion(long newVersion) {
            version = newVersion;
        }
    }

    static class TValue<T> extends AbstractTObject {
        private final ReentrantLock lock = new ReentrantLock();
        private volatile T value;

        TValue(T initial) {
            value = initial;
            version = 0L;
        }

        @SuppressWarnings("unchecked")
        T get() {
            Transaction t = Transaction.current();
            checkConflict(t.version);
            if (t.writeSet.containsKey(this)) {
                TValueSet<T> action = (TValueSet<T>) t.writeSet.get(this);
                return action.newValue;
            }
            T v = value;
            t.readSet.add(this);
            return v;
        }

        void set(T newValue) {
            Transaction t = Transaction.current();
            checkConflict(t.version);
            t.writeSet.put(this, new TValueSet<>(this, newValue));
        }

        @Override
        public boolean hasConflict(long version) {
            return lock.isLocked() || this.version > version;
        }

        @Override
        public boolean tryLock() {
            return lock.tryLock();
        }

        @Override
        public void unlock() {
            lock.unlock();
        }
    }

    static class TBufferAppend<T> implements WriteAction {
        private final T item;
        private final TBuffer<T> buffer;

        TBufferAppend(T item, TBuffer<T> buffer) {
            this.item = item;
            this.buffer = buffer;
        }

        @Override
        public void apply(long newVersion) {
            buffer.list.add(item);
            buffer.setVersion(newVersion);
        }
    }

    static class TBuffer<T> extends AbstractTObject {
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final LinkedList<T> list;

        TBuffer() {
            version = 0;
            list = new LinkedList<>();
        }

        void append(T item) {
            Transaction t = Transaction.current();
            checkConflict(t.version);
            t.writeSet.put(this, new TBufferAppend<>(item, this));
        }

        List<T> subList(int fromIndex) {
            Transaction t = Transaction.current();
            checkConflict(t.version);
            if (!readWriteLock.readLock().tryLock()) {
                throw new ConflictException();
            }
            t.readSet.add(this);
            try {
                Iterator<T> iterator = list.iterator();
                List<T> result = new ArrayList<>();
                T item;
                while (iterator.hasNext()) {
                    item = iterator.next();
                    result.add(item);
                }
                return result;
            } finally {
                readWriteLock.readLock().unlock();
            }
        }

        @Override
        public boolean tryLock() {
            return readWriteLock.writeLock().tryLock();
        }

        @Override
        public void unlock() {
            readWriteLock.writeLock().unlock();
        }

        @Override
        public boolean hasConflict(long version) {
            return readWriteLock.isWriteLocked() || this.version > version;
        }
    }

    static class Snapshot {
        final int lastIncludedIndex;
        final int lastIncludedTerm;

        Snapshot(int lastIncludedIndex, int lastIncludedTerm) {
            this.lastIncludedIndex = lastIncludedIndex;
            this.lastIncludedTerm = lastIncludedTerm;
        }
    }

    static class TSnapshot extends TValue<Snapshot> {
        TSnapshot() {
            this(new Snapshot(0, 0));
        }

        TSnapshot(Snapshot initial) {
            super(initial);
        }
    }

    static class TEntriesFile extends AbstractTObject {
        final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        LinkedList<Object> entries;

        TEntriesFile() {
            version = 0;
            entries = new LinkedList<>();
        }

        @Override
        public boolean hasConflict(long version) {
            return false;
        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public void unlock() {

        }
    }

    static class TransactionManager {
        // version overflow
        final AtomicLong version = new AtomicLong(1L);
        final Random random = new Random();
        volatile int threadLimit;
        final BlockingQueue<Runnable>[] queues;
        final Thread[] threads;
        final CountDownLatch shutdownLatch;

        @SuppressWarnings("unchecked")
        TransactionManager(int threadCount) {
            if (threadCount <= 0) {
                throw new IllegalArgumentException("thread count > 0");
            }
            threadLimit = threadCount;
            queues = (BlockingQueue<Runnable>[]) new BlockingQueue[threadCount];
            threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                int threadId = i;
                queues[threadId] = new LinkedBlockingQueue<>();
                threads[threadId] = new Thread(() -> worker(threadId), "worker-" + threadId);
            }
            shutdownLatch = new CountDownLatch(threadCount);
        }

        void start() {
            for (Thread t : threads) {
                t.start();
            }
        }

        private void worker(int threadId) {
            try {
                while (true) {
                    Runnable runnable = queues[threadId].take();
                    runnable.run();
                }
            } catch (InterruptedException ignored) {
            }
            shutdownLatch.countDown();
        }

        void resetVersions(Collection<TObject> tObjects) {
            threadLimit = 1;
            queues[0].offer(() -> doResetVersion(tObjects));
        }

        private void doResetVersion(Collection<TObject> tObjects) {
            LinkedList<TObject> lockedObjects = new LinkedList<>();
            for (TObject tObject : tObjects) {
                if (!tObject.tryLock()) {
                    unlockAll(lockedObjects);
                    resetVersions(tObjects);
                    return;
                }
                lockedObjects.add(tObject);
            }
            for (TObject tObject : tObjects) {
                tObject.setVersion(0L);
            }
            version.set(1L);
            unlockAll(tObjects);
            threadLimit = queues.length;
        }

        private void unlockAll(Collection<TObject> tObjects) {
            for (TObject tObject : tObjects) {
                tObject.unlock();
            }
        }

        void run(Runnable action) {
            int threadId = random.nextInt(threadLimit);
            run(threadId, action);
        }

        private void run(int threadId, Runnable action) {
            queues[threadId].offer(() -> doRun(threadId, action));
        }

        private void doRun(int threadId, Runnable action) {
            Transaction t = new Transaction(version.get());
            Transaction.current.set(t);
            int tries = 0;
            while (tries++ < 5) {
                try {
                    action.run();
                    if (t.hasWrite()) {
                        t.commit(version.incrementAndGet());
                    } else {
                        t.commit();
                    }
                    Transaction.current.set(null);
                    return;
                } catch (ConflictException ignored) {
                }
            }
            run(threadId / 2, action);
        }

        void shutdown() throws InterruptedException {
            for (Thread t : threads) {
                t.interrupt();
            }
            shutdownLatch.await();
        }
    }

    public static void main(String[] args) throws Exception {
        TransactionManager transactionManager = new TransactionManager(4);
        transactionManager.start();

        TBuffer<String> buffer = new TBuffer<>();
        TSnapshot snapshot = new TSnapshot();
        transactionManager.run(() -> {
            buffer.append("foo");
            System.out.println(snapshot.get().lastIncludedIndex);
        });
        transactionManager.run(()->{
            System.out.println(buffer.subList(0));
        });

        Thread.sleep(1000L);
        transactionManager.shutdown();
    }
}
