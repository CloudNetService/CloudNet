package de.dytanic.cloudnet.common.concurrent;

public interface IWorkableThread extends AutoCloseable {

    <V> IScheduledTask<V> getTask();

    <V> IWorkableThread setTask(IScheduledTask<V> scheduledTask);

    boolean isEmpty();

    int getTasksCount();

    void stop();

    void interrupt();

}