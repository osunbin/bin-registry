package com.bin.registry.server.core.timer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class DelayedOperation extends TimerTask {

    private Lock lock = new ReentrantLock();

    private AtomicBoolean tryCompletePending = new AtomicBoolean(false);
    private AtomicBoolean completed = new AtomicBoolean(false);

    public DelayedOperation(long delayMs) {
        super(delayMs);
    }

    /**
     * 强制完成延迟的操作（如果尚未完成）。
     *
     * 1.该操作已在tryComplete（）内验证为可完成
     * 2.操作已过期，因此需要立即完成
     * 如果操作由调用方完成，则返回true：请注意
     * 并发线程可以尝试完成相同的操作，但只能
     * 第一个线程将成功完成操作并返回true，其他线程仍将返回false
     */
    public boolean forceComplete() {
        if (completed.compareAndSet(false, true)) {
            // cancel the timeout timer
            cancel();
            onComplete();
            return true;
        } else {
            return false;
        }
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public abstract void onExpiration();

    public  abstract void onComplete();

    public abstract boolean tryComplete();


    public boolean maybeTryComplete() {
        boolean retry = false;
        boolean done = false;
        do {
            if (lock.tryLock()) {
                try {
                    tryCompletePending.set(false);
                    done = tryComplete();
                } finally {
                    lock.unlock();
                }
                // While we were holding the lock, another thread may have invoked `maybeTryComplete` and set
                // `tryCompletePending`. In this case we should retry.
                // 当我们持有锁时，另一个线程可能调用了“maybeTryComplete”
                // 并设置了“tryCompletePending”。在这种情况下，我们应该重试
                retry = tryCompletePending.get();
            } else {
                // Another thread is holding the lock. If `tryCompletePending` is already set and this thread failed to
                // acquire the lock, then the thread that is holding the lock is guaranteed to see the flag and retry.
                // Otherwise, we should set the flag and retry on this thread since the thread holding the lock may have
                // released the lock and returned by the time the flag is set.
                // 另一根线固定着锁。如果已经设置了“tryCompletePending”，
                // 并且此线程未能获取锁，则保证持有锁的线程能够看到该标志并重试。否则，
                // 我们应该设置标志并在该线程上重试，因为持有锁的线程可能已经释放了锁，并在设置标志时返回。
                retry = !tryCompletePending.getAndSet(true);
            }
        } while (!isCompleted() && retry);
        return done;
    }

    @Override
    public void run() {
        if (forceComplete())
            onExpiration();
    }
}
