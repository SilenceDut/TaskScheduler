package com.silencedut.taskscheduler;

/**
 * @author SilenceDut
 * @date 2019/3/25
 * 外部提供日志输出接口
 */
public interface ILog {

    void info(String info);
    void error(String error);
}
