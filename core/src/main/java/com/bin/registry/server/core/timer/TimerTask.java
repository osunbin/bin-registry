package com.bin.registry.server.core.timer;


import com.bin.registry.server.core.timer.TimerTaskList.TimerTaskEntry;


public abstract class TimerTask implements   Runnable{

    private long delayMs; // timestamp in millisecond

    private TimerTaskEntry timerTaskEntry;

    public TimerTask(long delayMs) {
        this.delayMs = delayMs;
    }

    public synchronized void cancel() {
        if (timerTaskEntry != null) timerTaskEntry.remove();
        timerTaskEntry = null;
    }


    public synchronized void setTimerTaskEntry(TimerTaskEntry entry) {
        // if this timerTask is already held by an existing timer task entry,
        // we will remove such an entry first.
        if (timerTaskEntry != null && timerTaskEntry != entry)
            timerTaskEntry.remove();

        timerTaskEntry = entry;
    }


    public TimerTaskEntry getTimerTaskEntry() {
        return timerTaskEntry;
    }

    public long getDelayMs() {
        return delayMs;
    }
}
