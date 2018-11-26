package com.silencedut.taskscheduler;

import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.os.Handler;

/**
 * @author SilenceDut
 * @date 2018/11/26
 */
public class LifecycleRunnableWrapper implements Runnable {
    private Runnable mOriginRunnable;
    private LifecycleOwner mLifecycleOwner;
    private GenericLifecycleObserver mLifecycleObserver;

    LifecycleRunnableWrapper(LifecycleOwner lifecycleOwner, final Handler handler,final Runnable originRunnable) {
        if(originRunnable == null || lifecycleOwner == null) {
            return;
        }
        this.mLifecycleOwner = lifecycleOwner;
        this.mOriginRunnable = originRunnable;
        mLifecycleObserver = new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {

                if(event == Lifecycle.Event.ON_DESTROY) {
                    if(mLifecycleOwner!=null ) {
                        mLifecycleOwner.getLifecycle().removeObserver(this);
                    }
                    handler.removeCallbacks(LifecycleRunnableWrapper.this);
                }
            }
        };
        mLifecycleOwner.getLifecycle().addObserver(mLifecycleObserver);
    }


    @Override
    public void run() {
        if(mOriginRunnable!=null && mLifecycleOwner!=null) {
            mOriginRunnable.run();
            mLifecycleOwner.getLifecycle().removeObserver(mLifecycleObserver);
        }

    }
}
