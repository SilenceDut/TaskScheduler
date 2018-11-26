package com.silencedut.sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silencedut.taskscheduler.TaskScheduler;

/**
 * @author SilenceDut
 * @date 2018/11/26
 */
public class LifeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_test,container,false );
        rootView.findViewById(R.id.remove_fragment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeThis();
            }
        });
        runTask();
        return rootView;
    }

    private void removeThis() {
        if(getFragmentManager()!=null) {
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }

    private void runTask() {
        TaskScheduler.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                removeThis();
                Log.i("LifeFragment","runTask no life");
            }
        },5000);

        TaskScheduler.runOnUIThread(this,new Runnable() {
            @Override
            public void run() {
                Log.i("LifeFragment","runTask with life");
            }
        },5000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("LifeFragment","onDestroy");
    }
}
