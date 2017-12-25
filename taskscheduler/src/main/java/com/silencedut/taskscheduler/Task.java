package com.silencedut.taskscheduler;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by SilenceDut on 2017/12/25 .
 */

public abstract class Task<R> implements Runnable {

    private static final String TAG = "Task";
    private AtomicBoolean mCanceledAtomic = new AtomicBoolean(false);
    private AtomicBoolean mDone = new AtomicBoolean(false);
    private AtomicReference<Thread> mTaskThread = new AtomicReference<>();

    /**
     * 异步线程处理任务，在非主线程执行
     * @return 处理后的结果
     */
    public abstract R doInBackground() ;

    /**
     * 异步线程处理后返回的结果，在主线程执行
     * @param result 结果
     */
    public abstract void onSuccess(R result);

    /**
     * 异步线程处理出现异常的回调，按需处理，未置成抽象，主线程执行
     * @param throwable 异常
     */
    public void onFail(Throwable throwable){

    }

    /**
     * 任务被取消的回调，主线程执行
     *
     */
    public void onCancel(){

    }

    /**
     * 将任务标记为取消，没法真正取消正在执行的任务，只是结果不在onSuccess里回调
     *
     *
     */

    public void cancel(boolean mayInterruptIfRunning) {

        if(mDone.get()) {
            return;
        }

        this.mCanceledAtomic.set(true);

        if (mayInterruptIfRunning ) {
            Thread t = mTaskThread.get();

            if(t!=null) {
                Log.d(TAG,"Task cancel: "+t.getName());
                t.interrupt();
            }

        }

        TaskScheduler.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                onCancel();
            }
        });
    }

    /**
     * 任务是已取消
     * @return 任务是否已被取消
     */
    public boolean isCanceled() {

        return mCanceledAtomic.get();
    }

    @Override
    public void run() {
        final R result;
        try {
            Log.d(TAG,"Task : "+Thread.currentThread().getName());
            mTaskThread.compareAndSet(null,Thread.currentThread());
            result = doInBackground();
            TaskScheduler.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if(!isCanceled()){
                        mDone.set(true);
                        onSuccess(result);
                    }
                }
            });
        } catch (final Throwable throwable) {
            Log.e(TAG,"handle background Task  error " +throwable);
            TaskScheduler.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if(!isCanceled()){
                        onFail(throwable);
                    }
                }
            });
        }
    }

}
