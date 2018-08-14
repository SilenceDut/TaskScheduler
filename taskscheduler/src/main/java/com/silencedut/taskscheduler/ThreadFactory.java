package com.silencedut.taskscheduler;

import android.os.Process;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author SilenceDut
 * @date 2018/7/5
 */
class ThreadFactory {

    static final java.util.concurrent.ThreadFactory TASKSCHEDULER_FACTORY = new java.util.concurrent.ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "TaskScheduler  #" + mCount.getAndIncrement());
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return thread;
        }
    };

     static final java.util.concurrent.ThreadFactory TIME_OUT_THREAD_FACTORY = new java.util.concurrent.ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "TaskScheduler timeoutThread #" + mCount.getAndIncrement());
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return thread;
        }
    };

    static final java.util.concurrent.ThreadFactory SCHEDULER_THREAD_FACTORY = new java.util.concurrent.ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread( Runnable r) {
            Thread thread = new Thread(r, "TaskScheduler scheduler #" + mCount.getAndIncrement());
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return thread;
        }
    };
}
