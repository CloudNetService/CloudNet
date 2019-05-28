package de.dytanic.cloudnet.common.concurrent;

public interface IScheduledTask<V> extends ITask<V> {

  long getTaskId();

  boolean isRepeatable();

  long getDelayedTimeStamp();

  IScheduledTask<V> setDelayMillis(long delayMillis);

  long getDelayMillis();

  IScheduledTask<V> setRepeatMillis(long repeatMillis);

  long getRepeatMillis();

  IScheduledTask<V> cancel();

}