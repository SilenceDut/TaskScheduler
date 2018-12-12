package com.silencedut.taskscheduler;

import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.os.Handler;

/**
 * @author SilenceDut
 * @date 2018/11/26
 */
public class LifecycleRunnableDelegate implements Runnable {
    private Runnable mOriginRunnable;
    private LifecycleOwner mLifecycleOwner;
    private GenericLifecycleObserver mLifecycleObserver;


    LifecycleRunnableDelegate(LifecycleOwner lifecycleOwner, final Handler handler, final Lifecycle.Event targetEvent, final Runnable originRunnable) {
        if(originRunnable == null || lifecycleOwner == null) {
            return;
        }
        this.mLifecycleOwner = lifecycleOwner;
        this.mOriginRunnable = originRunnable;
        mLifecycleObserver = new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {

                if(event == targetEvent) {
                    if(mLifecycleOwner!=null ) {
                        mLifecycleOwner.getLifecycle().removeObserver(this);
                    }
                    handler.removeCallbacks(LifecycleRunnableDelegate.this);
                }
            }
        };
        if(TaskScheduler.isMainThread()) {
            mLifecycleOwner.getLifecycle().addObserver(mLifecycleObserver);
        }else {
            TaskScheduler.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mLifecycleOwner.getLifecycle().addObserver(mLifecycleObserver);
                }
            });
        }

    }


    @Override
    public void run() {
        if(mOriginRunnable!=null && mLifecycleOwner!=null) {
            mOriginRunnable.run();
            mLifecycleOwner.getLifecycle().removeObserver(mLifecycleObserver);
        }

    }
}
