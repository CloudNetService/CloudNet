package de.dytanic.cloudnet.common.concurrent;

import org.jetbrains.annotations.ApiStatus;

@Deprecated
@ApiStatus.ScheduledForRemoval
public interface IScheduledTaskInvoker {

    <V> IScheduledTask<V> offerTask(IScheduledTask<V> scheduledTask);

    IScheduledTaskInvoker cancelAll();

}