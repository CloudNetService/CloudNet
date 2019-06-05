package de.dytanic.cloudnet.common.concurrent.scheduler;

import de.dytanic.cloudnet.common.annotation.UnsafeClass;
import de.dytanic.cloudnet.common.concurrent.IVoidCallback;

import java.util.concurrent.Callable;

/**
 * This class, will be removed in the future, and exchanged
 * for a new TaskScheduler with the support of the ITask class.
 *
 * @see de.dytanic.cloudnet.common.concurrent.ITask
 * @see TaskScheduler
 */
@Deprecated
@UnsafeClass
public class TaskEntry<T> {

    private final TaskEntryFuture<T> future;
    protected volatile Callable<T> task;
    protected volatile T value = null;
    protected IVoidCallback<T> callback;
    protected long delayTimeOut, repeat, delay;
    protected boolean completed = false;

    public TaskEntry(Callable<T> task, IVoidCallback<T> complete, long delay, long repeat) {

        this.task = task;
        this.callback = complete;
        this.delay = delay;
        this.delayTimeOut = System.currentTimeMillis() + delay;
        this.repeat = repeat;
        this.future = new TaskEntryFuture<>(false, this);
    }


    protected void invoke() throws Exception {

        if (task == null)
            return;

        T val = task.call();

        value = val;

        if (callback != null)
            callback.call(val);

        if (repeat != -1 && repeat != 0) repeat--;

        if (repeat != 0)
            this.delayTimeOut = System.currentTimeMillis() + delay;
        else {
            completed = true;

            if (future.waits) {
                synchronized (future) {
                    future.notifyAll();
                }
            }
        }
    }


    public IVoidCallback<T> getCallback() {
        return callback;
    }


    public long getDelayTimeOut() {
        return delayTimeOut;
    }


    public long getRepeat() {
        return repeat;
    }


    protected TaskEntryFuture<T> drop() {
        return future;
    }


    public boolean isCompleted() {
        return completed;
    }

}