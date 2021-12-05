package in.xnnyygn.concurrent.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.*;

public class ReservationSemaphoreExample {
    private static final Logger logger = LoggerFactory.getLogger(ReservationSemaphore.class);

    public static void main(String[] args) throws InterruptedException {
        int requests = 20;
        Random random = new Random();
        CountDownLatch latch = new CountDownLatch(requests);

        ReservationSemaphore semaphore = new ReservationSemaphore(2);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "semaphore"));
        scheduledExecutorService.scheduleAtFixedRate(semaphore, 0, 500, TimeUnit.MILLISECONDS);

        ScheduledExecutorService userRequestExecutorService1 = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "sender"));
        ExecutorService userRequestExecutorService2 = Executors.newFixedThreadPool(6);
        for (int i = 0; i < requests; i++) {
            final int id = i + 1;
            userRequestExecutorService1.schedule(() -> {
                long startedAt = System.currentTimeMillis();
                semaphore.acquire(new UserRequest(id, semaphore, userRequestExecutorService2, latch));
                logger.info("acquire permit, user request {}, time {}ms", id, System.currentTimeMillis() - startedAt);
            }, random.nextInt(100) + 100, TimeUnit.MILLISECONDS);
        }

        latch.await();

        userRequestExecutorService1.shutdown();
        userRequestExecutorService1.awaitTermination(3, TimeUnit.SECONDS);

        userRequestExecutorService2.shutdown();
        userRequestExecutorService2.awaitTermination(3, TimeUnit.SECONDS);

        scheduledExecutorService.shutdown();
        scheduledExecutorService.awaitTermination(3, TimeUnit.SECONDS);
    }
}
