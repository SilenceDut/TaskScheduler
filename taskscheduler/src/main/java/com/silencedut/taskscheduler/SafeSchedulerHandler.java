package com.silencedut.taskscheduler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


/**
 *
 * @author SilenceDut
 * @date 17/04/18
 *
 * a safe Handler avoid crash
 */
class SafeSchedulerHandler extends Handler{

    private static final String TAG = "SafeSchedulerHandler";
    public SafeSchedulerHandler(Looper looper) {
        super(looper);
    }

    public SafeSchedulerHandler(Looper looper, Callback callback) {
        super(looper, callback);
    }

    public SafeSchedulerHandler() {
        super();
    }

    public SafeSchedulerHandler(Callback callback) {
        super(callback);
    }

    @Override
    public void dispatchMessage(Message msg) {
        try {
            super.dispatchMessage(msg);
        } catch (Exception e) {
            Log.d(TAG, "dispatchMessage Exception " + msg + " , " + e);
        } catch (Error error) {
            Log.d(TAG, "dispatchMessage error " + msg + " , " + error);
        }
    }
}
