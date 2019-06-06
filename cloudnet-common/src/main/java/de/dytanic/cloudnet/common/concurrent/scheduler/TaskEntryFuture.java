package de.dytanic.cloudnet.common.concurrent.scheduler;

import de.dytanic.cloudnet.common.annotation.UnsafeClass;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class, will be removed in the future, and exchanged
 * for a new TaskScheduler with the support of the ITask class.
 *
 * @see de.dytanic.cloudnet.common.concurrent.ITask
 * @see TaskScheduler
 */
@Deprecated
@UnsafeClass
public class TaskEntryFuture<T> implements Future<T> {

    protected volatile boolean waits;
    private TaskEntry<T> entry;

    public TaskEntryFuture(boolean waits, TaskEntry<T> entry) {
        this.waits = waits;
        this.entry = entry;
    }

    public boolean isWaits() {
        return waits;
    }

    public TaskEntry<T> getEntry() {
        return entry;
    }

    @Override
    public boolean cancel(boolean pMayInterruptIfRunning) {

        if (pMayInterruptIfRunning) {
            entry.task = null;
            entry.repeat = 0;
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return entry.task == null;
    }


    @Override
    public boolean isDone() {
        return entry.completed;
    }


    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        waits = true;
        while (!isDone()) this.wait();

        return entry.value;
    }


    @Override
    public synchronized T get(long pTimeout, TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {

        waits = true;
        /*
        long timeout = System.currentTimeMillis() + (pUnit.toMillis(pTimeout));

        while (!isDone()) {
            if (timeout < System.currentTimeMillis()) Thread.sleep(0, 200000);
            else throw new TimeoutException();
        }*/

        while (!isDone()) this.wait(pUnit.toMillis(pTimeout));

        return entry.value;
    }

}