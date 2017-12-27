package com.silencedut.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.silencedut.taskscheduler.Task;
import com.silencedut.taskscheduler.TaskScheduler;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG ="MainActivity" ;
    private Task<String> mDemoTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start_task).setOnClickListener(this);
        findViewById(R.id.cancel_task).setOnClickListener(this);



        mDemoTask = new Task<String>() {
            @Override
            public String doInBackground()  {
                final long startMillis = System.currentTimeMillis();
                cut(10000000L);
                Log.i(TAG,"withResultTask doInBackground, current thread is main ? "+TaskScheduler.isMainThread());
                return "休眠"+(System.currentTimeMillis() - startMillis)/1000+"秒";
            }

            @Override
            public void onSuccess(String result) {
                Log.i(TAG,"withResultTask onSuccess, current thread is main ? "
                        +TaskScheduler.isMainThread()+", result : "+result);
            }

            @Override
            public void onFail(Throwable throwable) {
                super.onFail(throwable);
                Log.i(TAG,"onFail : "+throwable);
            }

            @Override
            public void onCancel() {
                super.onCancel();
                Log.i(TAG,"onCancel");
            }
        };
    }

    private void noResultTask() {
        TaskScheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i(TAG,"noResultTask current thread is main ? "+TaskScheduler.isMainThread());
            }
        });
    }
    
    private void withResultTask() {
        TaskScheduler.execute(mDemoTask);
    }

    static void cut(long n) {
        double y=1.0;
        for(int i=0;i<=n;i++){
            double π=3*Math.pow(2, i)*y;
            y=Math.sqrt(2-Math.sqrt(4-y*y));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDemoTask.cancel();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.start_task:
                Log.i(TAG,"startTask");
                noResultTask();
                withResultTask();
                break;
            case R.id.cancel_task:
                Log.i(TAG,"cancelTask");
                mDemoTask.cancel();
                break;
            default:break;
        }
    }
}
