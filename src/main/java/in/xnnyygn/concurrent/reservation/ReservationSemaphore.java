package in.xnnyygn.concurrent.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ReservationSemaphore implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ReservationSemaphore.class);
    private final ConcurrentLinkedQueue<UserRequest> queue = new ConcurrentLinkedQueue<>();
    private int permits; // only accessed by worker
    private final AtomicInteger reservedPermits = new AtomicInteger(0);

    ReservationSemaphore(int permits) {
        this.permits = permits;
    }

    void acquire(UserRequest request) {
        queue.offer(request);
    }

    void release() {
        UserRequest next = queue.poll();
        if (next != null) {
            next.signal();
        } else {
            reservedPermits.incrementAndGet();
        }
    }

    @Override
    public void run() {
        while (availablePermits() > 0) {
            UserRequest next = queue.poll();
            if (next == null) break;
            permits--;
            next.signal();
        }
    }

    private int availablePermits() {
        if (permits == 0) {
            int rp;
            while ((rp = reservedPermits.get()) > 0) {
                // move permits
                if (reservedPermits.compareAndSet(rp, 0)) {
                    logger.debug("move reserved permits {} to available permits", rp);
                    permits += rp;
                }
            }
        }
        return permits;
    }
}
