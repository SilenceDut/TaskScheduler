package com.silencedut.taskscheduler;


import com.silencedut.taskscheduler.exception.ErrorBundle;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by liushuai on 17/04/18.
 */

public interface TaskCallback {

    abstract class Callback<R> {
        public Type rType = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        public abstract void  onSuccess(R response);
        public abstract void  onError(ErrorBundle error);
    }

    abstract class Success<R> extends Callback<R> {
        @Override
        public void  onError(ErrorBundle error) {

        }
    }

}