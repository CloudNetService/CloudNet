package de.dytanic.cloudnet.common.concurrent;

public interface IScheduledTaskInvoker {

    <V> IScheduledTask<V> offerTask(IScheduledTask<V> scheduledTask);

    IScheduledTaskInvoker cancelAll();

}