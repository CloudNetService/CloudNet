package de.dytanic.cloudnet.common.concurrent;

public interface IWorkableThread extends AutoCloseable {

    <V> IWorkableThread setTask(IScheduledTask<V> scheduledTask);

    <V> IScheduledTask<V> getTask();

    boolean isEmpty();

    int getTasksCount();

    void stop();

    void interrupt();

}