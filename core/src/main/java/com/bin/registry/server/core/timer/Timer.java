package com.bin.registry.server.core.timer;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.bin.registry.server.core.timer.TimerTaskList.TimerTaskEntry;
public interface Timer {

    /**
     * Add a new task to this executor. It will be executed after the task's delay
     * (beginning from the time of submission)
     * @param timerTask the task to add
     */
    void add(TimerTask timerTask);

    /**
     * Advance the internal clock, executing any tasks whose expiration has been
     * reached within the duration of the passed timeout.
     * @param timeoutMs
     * @return whether or not any tasks were executed
     */
    boolean advanceClock(long timeoutMs);



    int size();



    void shutdown();



    class SystemTimer implements Timer{
        /**
         *  System.nanoTime() / 1 000 000
         */
       // static long hiResClockMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        static long hiResClockMs =   System.currentTimeMillis();
        private String executorName;


        private ExecutorService taskExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r,"executor-" + executorName);
                thread.setDaemon(false);
                return thread;
            }
        });

        private DelayQueue<TimerTaskList> delayQueue = new DelayQueue<>();

        private  AtomicInteger taskCounter = new AtomicInteger(0);

       private TimingWheel timingWheel;


       private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

       private  ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
       private  ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();


        public SystemTimer(String executorName) {
            this(executorName,1,40,hiResClockMs);

        }

        public SystemTimer(String executorName, long tickMs, int wheelSize, long startMs) {
            this.executorName = executorName;
            timingWheel = new TimingWheel(tickMs, wheelSize, startMs, taskCounter, delayQueue);

        }



        @Override
        public void add(TimerTask timerTask) {
            readLock.lock();
            try {
                addTimerTaskEntry(new TimerTaskEntry(timerTask, timerTask.getDelayMs() + hiResClockMs));
            } finally {
                readLock.unlock();
            }
        }

        private void addTimerTaskEntry(TimerTaskEntry timerTaskEntry) {
            if (!timingWheel.add(timerTaskEntry)) {
                // Already expired or cancelled
                // 被取消
                if (!timerTaskEntry.cancelled())
                    taskExecutor.submit(timerTaskEntry.getTimerTask());
            }
        }

        public void reinsert(TimerTaskEntry timerTaskEntry) {
            addTimerTaskEntry(timerTaskEntry);
        }

        /**
         *
         * @param timeoutMs  200l
         * @return
         */
        @Override
        public boolean advanceClock(long timeoutMs) {
            TimerTaskList bucket = null;
            try {
                bucket = delayQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (bucket != null) {
                writeLock.lock();
                try {
                    while (bucket != null) {
                        timingWheel.advanceClock(bucket.getExpiration());
                        bucket.flush(new Consumer<TimerTaskEntry>() {
                            @Override
                            public void accept(TimerTaskEntry timerTaskEntry) {
                                reinsert(timerTaskEntry);
                            }
                        });
                        bucket = delayQueue.poll();
                    }
                } finally {
                    writeLock.unlock();
                }
                return true;
            } else {
               return false;
            }
        }

        @Override
        public int size() {
            return taskCounter.get();
        }

        @Override
        public void shutdown() {
            taskExecutor.shutdown();
        }
    }

}
