package com.silencedut.taskscheduler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author SilenceDut
 * @date 2018/6/22
 */
public abstract class SchedulerTask implements Runnable {


    long startDelayMillisecond;
    long periodMillisecond;
    boolean mainThread = true;
    AtomicBoolean canceled = new AtomicBoolean(false);

    protected SchedulerTask(long periodMillisecond) {
        this.periodMillisecond = periodMillisecond;
    }

    protected SchedulerTask(long periodMillisecond,boolean mainThread) {
        this.periodMillisecond = periodMillisecond;
        this.mainThread = mainThread;
    }

    protected SchedulerTask(long periodMillisecond,boolean mainThread,long startDelayMillisecond) {
        this.periodMillisecond = periodMillisecond;
        this.mainThread = mainThread;
        this.startDelayMillisecond = startDelayMillisecond;
    }

    public abstract void onSchedule();

    @Override
    public void run() {
        if(!canceled.get()) {
            onSchedule();
        }
    }

}
