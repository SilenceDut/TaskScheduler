package com.silencedut.taskscheduler;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by SilenceDut on 17/04/18.
 *
 */
public class TaskScheduler {

    private volatile static TaskScheduler sTaskScheduler;
    private static final String TAG = "TaskScheduler";

    private Executor mParallelExecutor ;
    private ExecutorService mTimeOutExecutor ;
    private Handler mMainHandler = new SafeDispatchHandler(Looper.getMainLooper());

    private Map<String,Handler> mHandlerMap = new ConcurrentHashMap<>();

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = 0;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final BlockingQueue<Runnable> TIMEOUT_POOL_WORK_QUEUE =
            new LinkedBlockingQueue<>(128);


    private static TaskScheduler getInstance() {
        if(sTaskScheduler==null) {
            synchronized (TaskScheduler.class) {
                if(sTaskScheduler==null) {
                    sTaskScheduler = new TaskScheduler();
                }
            }
        }
        return sTaskScheduler;
    }

    private TaskScheduler() {

        // mParallelExecutor  直接使用AsyncTask的线程，减少新线程创建带来的资源消耗
        mParallelExecutor = AsyncTask.THREAD_POOL_EXECUTOR;

        mTimeOutExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,
                KEEP_ALIVE,TimeUnit.SECONDS,TIMEOUT_POOL_WORK_QUEUE, TIME_OUT_THREAD_FACTORY);
    }

    private static  Handler provideHandler(String handlerName) {

        if(getInstance().mHandlerMap.containsKey(handlerName)) {
            return getInstance().mHandlerMap.get(handlerName);
        }

        HandlerThread handlerThread = new HandlerThread(handlerName,Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        Handler handler = new SafeDispatchHandler(handlerThread.getLooper());
        getInstance().mHandlerMap.put(handlerName,handler);
        return handler;
    }

    /**
     *执行一个后台任务，无回调
     * **/
    public static void execute(Runnable task) {
        getInstance().mParallelExecutor.execute(task);
    }


    /**
     *执行一个后台任务，如果不需回调
     * @see #execute(Runnable)
     **/
    public static <R> void execute(Task<R> task) {
        getInstance().mParallelExecutor.execute(task);
    }

    public static void cancelTask(Task task) {
        if(task!=null) {
            task.cancel();
        }
    }


    /**
     * 使用一个单独的线程池来执行超时任务，避免引起他线程不够用导致超时
     *  @param timeOutMillis  超时时间，单位毫秒
     ** 通过实现error(Exception) 判断是否为 TimeoutException 来判断是否超时,
     *                        不能100%保证实际的超时时间就是timeOutMillis，但一般没必要那么精确
     * */
    public static <R> void executeTimeOutTask(final long timeOutMillis, final Task<R> timeOutTask) {
        final Future future =getInstance().mTimeOutExecutor.submit(timeOutTask);
        getInstance().mTimeOutExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get(timeOutMillis,TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e ) {
                    runOnUIThread(() -> {
                        if(!timeOutTask.isCanceled()) {
                            timeOutTask.onFail(e);
                            timeOutTask.cancel();
                        }
                    });
                }

            }
        });
    }


    /*
    **  默认只必须实现doInBackground，onSuccess接口
    *   按需实现需要的接口
    **/

    public  abstract static class Task<R> implements Runnable {

        private AtomicBoolean mCanceledAtomic = new AtomicBoolean(false);

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
         * @param exception 异常
         */
        public void onFail(Exception exception){

        }

        /**
         * 任务被取消的回调，主线程执行
         *
         */
        public void onCancel(){

        }

        /**
         * 将任务标记为取消，没法真正取消正在执行的任务，只是结果不在onSuccess里回调
         */
        public void cancel() {
            this.mCanceledAtomic.set(true);
            runOnUIThread(new Runnable() {
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
                result = doInBackground();

                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!isCanceled()){
                            onSuccess(result);
                        }
                    }
                });
            } catch (final Exception e) {
                Log.e(TAG,"handle background Task  error " +e);
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onFail(e);
                    }
                });

            }
        }
    }

    public static void runOnUIThread(@NonNull Runnable runnable) {

        getInstance().mMainHandler.post(runnable);
    }

    public static void removeHandlerCallback(String threadName,Runnable runnable) {
        if(isMainThread()) {
            getInstance().mMainHandler.removeCallbacks(runnable);
        }else if(getInstance().mHandlerMap.get(threadName)!=null){
            getInstance().mHandlerMap.get(threadName).removeCallbacks(runnable);
        }
    }

    public static Handler getMainHandler() {
        return getInstance().mMainHandler;
    }

    public static void runOnUIThread(Runnable runnable,long delayed) {
        getInstance().mMainHandler.postDelayed(runnable,delayed);
    }


    public static void removeUICallback(Runnable runnable) {
        removeHandlerCallback("main",runnable);
    }


    public static boolean isMainThread() {
        return Thread.currentThread()== getInstance().mMainHandler.getLooper().getThread();
    }

    private static final ThreadFactory TIME_OUT_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, "werewolf timeoutThread #" + mCount.getAndIncrement());
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return thread;
        }
    };


}
