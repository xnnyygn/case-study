package in.xnnyygn.concurrent.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class UserRequest {
    private static final Logger logger = LoggerFactory.getLogger(ReservationSemaphore.class);

    private final long createdAt = System.currentTimeMillis();
    private final int id;

    private final ReservationSemaphore semaphore;
    private final ExecutorService userRequestExecutorService;
    private final CountDownLatch latch;

    UserRequest(int id, ReservationSemaphore semaphore,
                ExecutorService userRequestExecutorService, CountDownLatch latch) {
        this.id = id;

        this.semaphore = semaphore;
        this.userRequestExecutorService = userRequestExecutorService;
        this.latch = latch;
    }

    void signal() {
        logger.debug("acquired permit, user request {}, delay {}ms", id, System.currentTimeMillis() - createdAt);

        userRequestExecutorService.submit(() -> {
            try {
                Thread.sleep(new Random().nextInt(100) + 100);
            } catch (InterruptedException ignored) {
            }
            logger.debug("release permit, user request {}", id);
            semaphore.release();
            latch.countDown();
        });
    }
}
