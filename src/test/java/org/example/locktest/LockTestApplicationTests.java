package org.example.locktest;

import org.example.locktest.book.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LockTestApplicationTests {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OptimisticLockService optimisticLockService;

    @Autowired
    private PessimisticLockService pessimisticLockService;

    private Long bookId;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        Book saved = bookRepository.save(new Book("JPA", 10));
        bookId = saved.getId();
    }

    @Test
    void optimisticLocking_serviceHoldsLockAndSecondCommitFails() throws Exception {
        // Thread 1: acquire OPTIMISTIC lock, sleep 3s, then update and flush (will commit at method end)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Throwable> future = executor.submit(() -> {
            try {
                optimisticLockService.lockReadSleepAndUpdateTitle(bookId, "-t1");
                return null; // no exception
            } catch (Throwable t) {
                return t;
            }
        });

        // Give thread1 a head start to acquire the lock
        Thread.sleep(200);

        // Main thread updates and commits quickly -> should succeed first
        Book b = bookRepository.findById(bookId).orElseThrow();
        b.setTitle(b.getTitle() + "-main");
        bookRepository.saveAndFlush(b);

        // Now wait for thread1 result; it should fail with optimistic locking exception on flush/commit
        Throwable t = future.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertNotNull(t, "Expected an optimistic locking exception from service call");
        assertTrue(t instanceof ObjectOptimisticLockingFailureException,
                "Unexpected exception: " + t);
    }

    @Test
    void pessimisticLocking_serviceHoldsLock_otherTxTimesOutOrFails() throws Exception {
        // Thread 1: acquire PESSIMISTIC_WRITE lock and hold for 3 seconds
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> f1 = executor.submit(() -> pessimisticLockService.lockReadAndSleep(bookId));

        // Give thread1 a moment to acquire the lock
        Thread.sleep(200);

        // Main thread tries to acquire same lock and update -> should time out/fail (configured timeout 1000ms)
        Throwable thrown;
        try {
            pessimisticLockService.tryAcquireAndUpdateTitle(bookId);
            thrown = null;
        } catch (Throwable t) {
            thrown = t;
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertNotNull(thrown, "Expected an exception due to lock timeout or failure");
        boolean acceptable = thrown instanceof PessimisticLockingFailureException
                || thrown instanceof CannotAcquireLockException
                || thrown.getClass().getName().contains("PessimisticLockException")
                || thrown.getClass().getName().contains("LockTimeoutException");
        assertTrue(acceptable, "Unexpected exception type: " + thrown);
    }
}
