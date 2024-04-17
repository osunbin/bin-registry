package com.bin.registry.server.core.timer;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class TimerTaskList implements Delayed {

    private AtomicInteger taskCounter;

    private TimerTaskEntry root = new TimerTaskEntry(null, -1);

    private AtomicLong expiration = new AtomicLong(-1L);

    public TimerTaskList(AtomicInteger taskCounter) {
        this.taskCounter = taskCounter;
        root.next = root;
        root.prev = root;
    }


    public boolean setExpiration(long expirationMs) {
        return expiration.getAndSet(expirationMs) != expirationMs;
    }


    public long getExpiration() {
        return expiration.get();
    }


    public synchronized void foreach(Consumer<TimerTask> f) {
        TimerTaskEntry entry = root.next;
        while (entry != root) {
            TimerTaskEntry nextEntry = entry.next;

            if (!entry.cancelled())
                f.accept(entry.timerTask);

            entry = nextEntry;
        }
    }


    public void add(TimerTaskEntry timerTaskEntry) {
        boolean done = false;
        while (!done) {
            // Remove the timer task entry if it is already in any other list
            // We do this outside of the sync block below to avoid deadlocking.
            // We may retry until timerTaskEntry.list becomes null.
            timerTaskEntry.remove();

            synchronized (timerTaskEntry) {

                if (timerTaskEntry.list == null) {
                    // put the timer task entry to the end of the list. (root.prev points to the tail entry)
                    TimerTaskEntry tail = root.prev;
                    timerTaskEntry.next = root;
                    timerTaskEntry.prev = tail;
                    timerTaskEntry.list = this;
                    tail.next = timerTaskEntry;
                    root.prev = timerTaskEntry;
                    taskCounter.incrementAndGet();
                    done = true;
                }
            }
        }
    }


    public void remove(TimerTaskEntry timerTaskEntry) {
        synchronized (timerTaskEntry) {
            if (timerTaskEntry.list == this) {
                timerTaskEntry.next.prev = timerTaskEntry.prev;
                timerTaskEntry.prev.next = timerTaskEntry.next;
                timerTaskEntry.next = null;
                timerTaskEntry.prev = null;
                timerTaskEntry.list = null;
                taskCounter.decrementAndGet();
            }
        }
    }



    public synchronized void flush(Consumer<TimerTaskEntry> f) {
        TimerTaskEntry head = root.next;
        while (head != root) {
            remove(head);
            f.accept(head);
            head = root.next;
        }
        expiration.set(-1L);
    }


    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(getExpiration() -
                System.currentTimeMillis(), 0), TimeUnit.MILLISECONDS);
    }



    @Override
    public int compareTo(Delayed d) {
        TimerTaskList other = (TimerTaskList) d;

        if(getExpiration() < other.getExpiration())
            return -1;
        else if(getExpiration() > other.getExpiration())
            return 1;
        else
            return 0;
    }


    public static class TimerTaskEntry implements Comparable<TimerTaskEntry> {
        private TimerTask timerTask;
        private long expirationMs;

        private TimerTaskList list;

        private TimerTaskEntry next;

        private TimerTaskEntry prev;

        public TimerTaskEntry(TimerTask timerTask, long expirationMs) {
            this.timerTask = timerTask;
            this.expirationMs = expirationMs;
            if (timerTask != null) timerTask.setTimerTaskEntry(TimerTaskEntry.this);
        }

        public boolean cancelled() {
            return timerTask.getTimerTaskEntry() != this;
        }


        public void remove() {
            TimerTaskList currentList = list;
            // If remove is called when another thread is moving the entry from a task entry list to another,
            // this may fail to remove the entry due to the change of value of list. Thus, we retry until the list becomes null.
            // In a rare case, this thread sees null and exits the loop, but the other thread insert the entry to another list later.
            while (currentList != null) {
                currentList.remove(TimerTaskEntry.this);
                currentList = list;
            }
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public TimerTask getTimerTask() {
            return timerTask;
        }

        @Override
        public int compareTo(TimerTaskEntry that) {
            return (int) (this.expirationMs - that.expirationMs);
        }
    }
}
