package com.silencedut.taskscheduler.exception;
/**
 * Created by SilenceDut on 17/04/18.
 *
 * Interface to represent a wrapper around an {@link Exception} to manage errors.
 */
public class ErrorBundle {

  public static final String NETWORKERROR = "NETWORKERROR";
  public static final String FILEERROR = "FILEERROR";
  public static final String TIMEOUTERROR = "TIMEOUTERROR";
  public static final String TASKERROR = "TASKERROR";
  private String mDefaultErrorMsg = "UnCare error";
  private String mErrorMsg;

  private final Exception exception;

  public ErrorBundle(String msg) {
    exception = null;
    this.mErrorMsg = msg;
  }

  public ErrorBundle(Exception exception) {
    this.exception = exception;
  }

  public ErrorBundle(Exception exception, String msg) {
    this.exception = exception;
    this.mErrorMsg = msg;
  }

  public Exception getException() {
    return exception;
  }

  public String getErrorMessage() {

    if (exception != null) {
      mDefaultErrorMsg = exception.getMessage();
    }
    return (mErrorMsg != null) ? mErrorMsg : mDefaultErrorMsg;
  }
}
