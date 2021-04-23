package de.dytanic.cloudnet.common.concurrent;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@NotNull
@Deprecated
@ApiStatus.ScheduledForRemoval
public interface IScheduledTask<V> extends ITask<V> {

    long getTaskId();

    boolean isRepeatable();

    long getDelayedTimeStamp();

    long getDelayMillis();

    IScheduledTask<V> setDelayMillis(long delayMillis);

    long getRepeatMillis();

    IScheduledTask<V> setRepeatMillis(long repeatMillis);

    IScheduledTask<V> cancel();

}