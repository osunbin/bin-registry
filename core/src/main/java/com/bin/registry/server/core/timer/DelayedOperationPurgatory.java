package com.bin.registry.server.core.timer;


import com.bin.registry.server.common.utils.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bin.registry.server.core.timer.Timer.SystemTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DelayedOperationPurgatory<T extends DelayedOperation> {

    static final Logger logger = LoggerFactory.getLogger(DelayedOperationPurgatory.class);

    /**
     * 时间轮线程名
     */
    private String purgatoryName;
    private Timer timeoutTimer;





    private Pool<WatchKey, Watchers> watchersForKey = new Pool<>(k -> new Watchers(k));


    private ReentrantReadWriteLock removeWatchersLock = new ReentrantReadWriteLock();

    private AtomicInteger estimatedTotalOperations = new AtomicInteger(0);


    private ExpiredOperationReaper expirationReaper = new ExpiredOperationReaper();

    public DelayedOperationPurgatory(String purgatoryName) {
        this(purgatoryName, new Timer.SystemTimer(purgatoryName));
    }



    public DelayedOperationPurgatory(String purgatoryName, Timer timeoutTimer) {
        this.purgatoryName = purgatoryName;
        this.timeoutTimer = timeoutTimer;

            expirationReaper.start();
    }

    /**
     * 多个key 监听一个任务
     *
     * @param operation
     * @param watchKeys
     * @return
     */
    public void tryCompleteElseWatch(T operation, List<? extends WatchKey> watchKeys) {

        boolean watchCreated = false;
        // 一个任务通知多个 对象
        for (WatchKey key : watchKeys) {

            watchForOperation(key, operation);

            if (!watchCreated) {
                watchCreated = true;
                estimatedTotalOperations.incrementAndGet();
            }
        }
        timeoutTimer.add(operation);
    }


    public int checkAndComplete(WatchKey key) {
        ReentrantReadWriteLock.ReadLock readLock = removeWatchersLock.readLock();
        Watchers watchers = null;
        readLock.lock();
        try {
            watchers = watchersForKey.get(key);
        } finally {
            readLock.unlock();
        }

        if (watchers == null)
            return 0;
        else
            return watchers.tryCompleteWatched();
    }

    public T pollByKey(WatchKey key) {
        ReentrantReadWriteLock.ReadLock readLock = removeWatchersLock.readLock();
        Watchers watchers = null;
        readLock.lock();
        try {
            watchers = watchersForKey.get(key);
        } finally {
            readLock.unlock();
        }

        if (watchers == null)
            return null;
        else
            return watchers.poll();
    }

    public int check(WatchKey key) {
        ReentrantReadWriteLock.ReadLock readLock = removeWatchersLock.readLock();
        Watchers watchers = null;
        readLock.lock();
        try {
            watchers = watchersForKey.get(key);
        } finally {
            readLock.unlock();
        }

        if (watchers == null)
            return 0;
        else
            return watchers.size();
    }



    public int watched() {
        int sum = 0;
        Iterator<Watchers> iterator = allWatchers().iterator();
        while (iterator.hasNext()) {
            Watchers next = iterator.next();
            sum += next.countWatched();
        }
        return sum;
    }

    public int delayed() {
        return timeoutTimer.size();
    }

    public List<T> cancelForKey(WatchKey key) {
        ReentrantReadWriteLock.WriteLock writeLock = removeWatchersLock.writeLock();
        writeLock.lock();
        try {
            Watchers watchers = watchersForKey.remove(key);
            if (watchers != null)
                return watchers.cancel();
            else
                return Collections.emptyList();
        } finally {
            writeLock.unlock();
        }
    }

    public Iterable<Watchers> allWatchers() {
        ReentrantReadWriteLock.ReadLock readLock = removeWatchersLock.readLock();
        readLock.lock();
        try {
            return watchersForKey.values();
        } finally {
            readLock.unlock();
        }
    }

    public void watchForOperation(WatchKey key, T operation) {
        ReentrantReadWriteLock.ReadLock readLock = removeWatchersLock.readLock();
        readLock.lock();
        try {
            Watchers watcher = watchersForKey.getAndMaybePut(key);
            watcher.watch(operation);
        } finally {
            readLock.unlock();
        }
    }

    public void removeKeyIfEmpty(WatchKey key, Watchers watchers) {
        ReentrantReadWriteLock.WriteLock writeLock = removeWatchersLock.writeLock();
        writeLock.lock();
        try {
            // if the current key is no longer correlated to the watchers to remove, skip
            if (watchersForKey.get(key) != watchers)
                return;

            if (watchers != null && watchers.isEmpty()) {
                watchersForKey.remove(key);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void shutdown() {
            timeoutTimer.shutdown();
    }


    public void advanceClock(long timeoutMs) {
        timeoutTimer.advanceClock(timeoutMs);
    }

    private class ExpiredOperationReaper extends Thread {

        public ExpiredOperationReaper() {
            setName("ExpirationReaper-" + purgatoryName);
        }

        @Override
        public void run() {
            advanceClock(200L);
        }
    }


    public class Watchers {
        private WatchKey key;

        public Watchers(WatchKey key) {
            this.key = key;
        }

        private ConcurrentLinkedQueue<T> operations = new ConcurrentLinkedQueue();

        public int countWatched() {
            return operations.size();
        }

        public boolean isEmpty() {
            return operations.isEmpty();
        }

        public boolean watch(T t) {
            return operations.add(t);
        }

        public int tryCompleteWatched() {
            int completed = 0;

            Iterator<T> iter = operations.iterator();
            while (iter.hasNext()) {
                T curr = iter.next();
                if (curr.isCompleted()) {
                    // another thread has completed this operation, just remove it
                    iter.remove();
                } else if (curr.maybeTryComplete()) {
                    iter.remove();
                    completed += 1;
                }
            }

            if (operations.isEmpty())
                removeKeyIfEmpty(key, this);

            return completed;
        }

        public List<T> cancel() {
            Iterator<T> iter = operations.iterator();
            List<T> cancelled = new ArrayList<>();
            while (iter.hasNext()) {
                T curr = iter.next();
                curr.cancel();
                iter.remove();
                cancelled.add(curr);
            }
            return cancelled;
        }

        public int size() {
            return operations.size();
        }

        public T poll() {
            if (size() > 0) {
                return operations.poll();
            }
            return null;
        }

        public int purgeCompleted() {
            int purged = 0;

            Iterator<T> iter = operations.iterator();
            while (iter.hasNext()) {
                T curr = iter.next();
                if (curr.isCompleted()) {
                    iter.remove();
                    purged += 1;
                }
            }

            if (operations.isEmpty()) ;
            removeKeyIfEmpty(key, this);

            return purged;
        }
    }
}
