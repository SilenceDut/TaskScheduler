package com.silencedut.taskscheduler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author SilenceDut
 * @date 2018/6/22
 */
public abstract class SchedulerTask implements Runnable {

    long periodSecond;
    AtomicBoolean canceled = new AtomicBoolean(false);

    public SchedulerTask(long periodSecond) {
        this.periodSecond = periodSecond;
    }

    public abstract void onSchedule();

    @Override
    public void run() {
        if(!canceled.get()) {
            onSchedule();
        }
    }

}
