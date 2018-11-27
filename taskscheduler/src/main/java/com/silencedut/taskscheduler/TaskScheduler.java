package com.silencedut.taskscheduler;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author SilenceDut
 * @date 17/04/18
 *
 */
public class TaskScheduler {


    private volatile static TaskScheduler sTaskScheduler;
    private static final String TAG = "TaskScheduler";

    private Executor mParallelExecutor ;
    private ExecutorService mTimeOutExecutor ;
    private Handler mIOHandler;
    private SafeSchedulerHandler mMainHandler = new SafeSchedulerHandler(Looper.getMainLooper());


    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 60L;
    private static final BlockingQueue<Runnable> POOL_WORK_QUEUE =
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

        /*
          mParallelExecutor  直接使用AsyncTask的线程，减少新线程创建带来的资源消耗
          */
        mParallelExecutor = new ThreadPoolExecutor(CPU_COUNT,MAXIMUM_POOL_SIZE,
                KEEP_ALIVE,TimeUnit.SECONDS,POOL_WORK_QUEUE,ThreadFactory.TASKSCHEDULER_FACTORY);



        /*
          没有核心线程的线程池要用 SynchronousQueue 而不是LinkedBlockingQueue，SynchronousQueue是一个只有一个任务的队列，
          这样每次就会创建非核心线程执行任务,因为线程池任务放入队列的优先级比创建非核心线程优先级大.
         */
        mTimeOutExecutor = new ThreadPoolExecutor(0,MAXIMUM_POOL_SIZE,
                KEEP_ALIVE,TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),ThreadFactory.TIME_OUT_THREAD_FACTORY);

        mIOHandler = provideHandler("IoHandler");

    }

    /**
     * 获取回调到handlerName线程的handler.一般用于在一个后台线程执行同一种任务，避免线程安全问题。如数据库，文件操作
     * @param handlerName 线程名
     * @return 异步任务handler
     */
    public static Handler provideHandler(String handlerName) {


        HandlerThread handlerThread = new HandlerThread(handlerName,Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        return new SafeSchedulerHandler(handlerThread.getLooper());
    }

    /**
     * 提供一个公用的异步handler
     */
    public static Handler ioHandler() {
        return getInstance().mIOHandler;
    }

    /**
     * 主线程周期性执行任务，默认立刻执行，之后间隔period执行，不需要时注意取消,每次执行时如果有相同的任务，默认会先取消
     * @param task 执行的任务
     */
    public static void scheduleTask(final SchedulerTask task) {

        task.canceled.compareAndSet(true,false);

        final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1,ThreadFactory.SCHEDULER_THREAD_FACTORY);

        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(task.canceled.get()) {
                    service.shutdownNow();
                }else {
                    if(task.mainThread) {
                        runOnUIThread(task);
                    }else {
                        task.run();
                    }
                }
            }
        }, task.startDelayMillisecond, task.periodMillisecond, TimeUnit.MILLISECONDS);
    }

    /**
     * 取消周期性任务
     * @param schedulerTask 任务对象
     */
    public static void stopScheduleTask(final SchedulerTask schedulerTask) {
        schedulerTask.canceled.compareAndSet(false,true);
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

    /**
     * 取消一个任务
     * @param task 被取消的任务
     */
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
                    runOnUIThread(new Runnable()  {
                        @Override
                        public void run() {
                            if(!timeOutTask.isCanceled()) {
                                timeOutTask.cancel();
                            }
                        }
                    });
                }

            }
        });
    }


    public static Handler mainHandler() {
        return getInstance().mMainHandler;
    }

    /**
     *use {@link #mainHandler}
     */
    @Deprecated
    public static Handler getMainHandler() {
        return getInstance().mMainHandler;
    }

    public static void runOnUIThread(Runnable runnable) {

        getInstance().mMainHandler.post(runnable);
    }

    /**
     * 执行有生命周期的任务
     */
    public static void runOnUIThread(LifecycleOwner lifecycleOwner,Runnable runnable) {
        LifecycleRunnableDelegate lifecycleRunnableDelegate = new LifecycleRunnableDelegate(lifecycleOwner,getInstance().mMainHandler,Lifecycle.Event.ON_DESTROY,runnable);
        getInstance().mMainHandler.post(lifecycleRunnableDelegate);
    }


    /**
     * 执行有生命周期的任务,指定Lifecycle.Event
     */
    public static void runOnUIThread(LifecycleOwner lifecycleOwner,Lifecycle.Event targetEvent,Runnable runnable) {
        LifecycleRunnableDelegate lifecycleRunnableDelegate = new LifecycleRunnableDelegate(lifecycleOwner,getInstance().mMainHandler,targetEvent,runnable);
        getInstance().mMainHandler.post(lifecycleRunnableDelegate);
    }

    public static void runOnUIThread(Runnable runnable,long delayed) {
        getInstance().mMainHandler.postDelayed(runnable,delayed);
    }

    public static void runOnUIThread(LifecycleOwner lifecycleOwner,Runnable runnable,long delayed) {
        LifecycleRunnableDelegate lifecycleRunnableDelegate = new LifecycleRunnableDelegate(lifecycleOwner,getInstance().mMainHandler,Lifecycle.Event.ON_DESTROY,runnable);
        getInstance().mMainHandler.postDelayed(lifecycleRunnableDelegate,delayed);
    }

    public static void runOnUIThread(LifecycleOwner lifecycleOwner,Lifecycle.Event targetEvent,Runnable runnable,long delayed) {
        LifecycleRunnableDelegate lifecycleRunnableDelegate = new LifecycleRunnableDelegate(lifecycleOwner,getInstance().mMainHandler,targetEvent,runnable);
        getInstance().mMainHandler.postDelayed(lifecycleRunnableDelegate,delayed);
    }

    /**
     * 外部提供执行任务的Handler
     */


    public static void runLifecycleRunnable(LifecycleOwner lifecycleOwner,Handler anyThreadHandler,Runnable runnable,long delayed) {
        LifecycleRunnableDelegate lifecycleRunnableDelegate = new LifecycleRunnableDelegate(lifecycleOwner,anyThreadHandler,Lifecycle.Event.ON_DESTROY,runnable);
        anyThreadHandler.postDelayed(lifecycleRunnableDelegate,delayed);
    }

    /**
     * 外部提供执行任务的Handler,指定移除的Lifecycle.Event
     */

    public static void runLifecycleRunnable(LifecycleOwner lifecycleOwner,Handler anyThreadHandler,Lifecycle.Event targetEvent,Runnable runnable,long delayed) {
        LifecycleRunnableDelegate lifecycleRunnableDelegate = new LifecycleRunnableDelegate(lifecycleOwner,anyThreadHandler,targetEvent,runnable);
        anyThreadHandler.postDelayed(lifecycleRunnableDelegate,delayed);
    }


    public static void removeUICallback(Runnable runnable) {
        if(runnable!=null) {
            mainHandler().removeCallbacks(runnable);
        }
    }

    public static boolean isMainThread() {
        return Thread.currentThread()== getInstance().mMainHandler.getLooper().getThread();
    }


}
