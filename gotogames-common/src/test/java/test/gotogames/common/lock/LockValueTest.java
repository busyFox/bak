package test.gotogames.common.lock;

import com.gotogames.common.lock.LockValue;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockValueTest {
    public static void stop(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("termination interrupted");
        }
        finally {
            if (!executor.isTerminated()) {
                System.err.println("killing non-finished tasks");
            }
            executor.shutdownNow();
        }
    }

    public static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        LockValue lockString = new LockValue(20);
        Runnable task = () -> {
            int nbSeconds = 2+ new Random().nextInt(5);
            ReentrantLock l = lockString.getLockForString("toto"+nbSeconds);
            l.lock();
            try {
                System.out.println("Start sleeping " + nbSeconds+" seconds");
                sleep(nbSeconds);
                System.out.println("End sleeping "+nbSeconds+" seconds");
            } catch (Exception e) {
            } finally {
                l.unlock();
            }
        };

        ReentrantLock lock = lockString.getLockForString("5ba2cb900eb6827ae5eacb2e");

        for (int i = 0; i < 5; i++) {
            executor.submit(task);
        }

        stop(executor);
    }
}
