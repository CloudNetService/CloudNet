package de.dytanic.cloudnet.common.concurrent;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class DefaultScheduledTask<V> implements IScheduledTask<V> {

    private static final AtomicLong TASK_ID_COUNTER = new AtomicLong();

    private final long taskId = TASK_ID_COUNTER.incrementAndGet();

    private Collection<ITaskListener<V>> listeners;

    private volatile V value;

    private volatile boolean wait, done, cancelled;
    private long delay, repeat, repeats, delayedTimeStamp;
    private Callable<V> callable;

    public DefaultScheduledTask(Callable<V> callable, long delay, long repeat, long repeats, TimeUnit timeUnit) {
        this.callable = callable;

        this.delay = delay > 0 ? timeUnit.toMillis(delay) : -1;
        this.repeat = repeat > 0 ? timeUnit.toMillis(repeat) : -1;

        this.repeats = repeats < 1 ? 1 : repeats;
        this.delayedTimeStamp = System.currentTimeMillis() + this.delay;
    }

    @Override
    @NotNull
    public final ITask<V> addListener(ITaskListener<V> listener) {
        if (listener == null) {
            return this;
        }

        initListenersCollectionIfNotExists();

        this.listeners.add(listener);

        return this;
    }

    @Override
    public long getTaskId() {
        return this.taskId;
    }

    @Override
    public Collection<ITaskListener<V>> getListeners() {
        return this.listeners;
    }

    public V getValue() {
        return this.value;
    }

    public boolean isWait() {
        return this.wait;
    }

    public void setWait(boolean wait) {
        this.wait = wait;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public long getDelay() {
        return this.delay;
    }

    public long getRepeat() {
        return this.repeat;
    }

    public long getRepeats() {
        return this.repeats;
    }

    @Override
    public long getDelayedTimeStamp() {
        return this.delayedTimeStamp;
    }

    @Override
    public Callable<V> getCallable() {
        return this.callable;
    }

    @Override
    @NotNull
    public ITask<V> clearListeners() {
        this.listeners.clear();
        return this;
    }

    @Override
    public synchronized V getDef(V def) {
        return get(5, TimeUnit.SECONDS, def);
    }

    @Override
    public synchronized V get(long time, TimeUnit timeUnit, V def) {
        Preconditions.checkNotNull(timeUnit);

        try {
            return get(time, timeUnit);
        } catch (Throwable throwable) {
            return def;
        }
    }

    @Override
    public V call() {
        if (this.callable == null || this.done) {
            return this.value;
        }

        if (!isCancelled()) {
            try {
                this.value = this.callable.call();
            } catch (Throwable throwable) {
                this.invokeFailure(throwable);
            }
        }

        if (this.repeats > 0) {
            this.repeats--;
        }

        if ((this.repeats > 0 || this.repeats == -1) && !this.cancelled) {
            this.delayedTimeStamp = System.currentTimeMillis() + this.repeat;
        } else {
            this.done = true;
            this.invokeTaskListener();

            if (this.wait) {
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }

        return this.value;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mayInterruptIfRunning) {
            this.callable = null;
            this.repeats = 0;
        }

        return mayInterruptIfRunning;
    }

    @Override
    public synchronized V get() throws InterruptedException {
        this.wait = true;
        while (!isDone()) {
            this.wait();
        }

        return this.value;
    }

    @Override
    public synchronized V get(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        this.wait = true;
        if (!isDone()) {
            this.wait(unit.toMillis(timeout));
        }

        return this.value;
    }


    private void initListenersCollectionIfNotExists() {
        if (this.listeners == null) {
            this.listeners = new ConcurrentLinkedQueue<>();
        }
    }

    private void invokeTaskListener() {
        if (this.listeners != null) {
            for (ITaskListener<V> listener : this.listeners) {
                try {
                    if (this.cancelled) {
                        listener.onCancelled(this);
                    } else {
                        listener.onComplete(this, this.value);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void invokeFailure(Throwable throwable) {
        if (this.listeners != null) {
            for (ITaskListener<V> listener : this.listeners) {
                try {
                    listener.onFailure(this, throwable);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean isRepeatable() {
        return this.repeats > 0 || this.repeats == -1;
    }

    @Override
    public IScheduledTask<V> setDelayMillis(long delayMillis) {
        this.delay = delayMillis;
        return this;
    }

    @Override
    public long getDelayMillis() {
        return this.delay;
    }

    @Override
    public IScheduledTask<V> setRepeatMillis(long repeatMillis) {
        this.repeat = repeatMillis;
        return this;
    }

    @Override
    public long getRepeatMillis() {
        return this.repeat;
    }

    @Override
    public IScheduledTask<V> cancel() {
        this.cancel(true);
        return this;
    }
}