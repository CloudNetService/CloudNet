package de.dytanic.cloudnet.common.concurrent;

import de.dytanic.cloudnet.common.Validate;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public final class DefaultScheduledTask<V> implements IScheduledTask<V> {

    private static final AtomicLong TASK_ID_COUNTER = new AtomicLong();

    private final long taskId = TASK_ID_COUNTER.incrementAndGet();

    @Getter
    private Collection<ITaskListener<V>> listeners;

    private volatile V value;

    @Setter
    private volatile boolean wait, done, cancelled;

    private long delay, repeat, repeats, delayedTimeStamp;

    @Getter
    private Callable<V> callable;

    public DefaultScheduledTask(Callable<V> callable, long delay, long repeat, long repeats, TimeUnit timeUnit)
    {
        this.callable = callable;

        this.delay = delay > 0 ? timeUnit.toMillis(delay) : -1;
        this.repeat = repeat > 0 ? timeUnit.toMillis(repeat) : -1;

        this.repeats = repeats < 1 ? 1 : repeats;
        this.delayedTimeStamp = System.currentTimeMillis() + this.delay;
    }

    @SafeVarargs
    @Override
    public final ITask<V> addListener(ITaskListener<V>... listeners)
    {
        if (listeners == null) return this;

        initListenersCollectionIfNotExists();

        for (ITaskListener<V> listener : listeners)
            if (listener != null)
                this.listeners.add(listener);

        return this;
    }

    @Override
    public ITask<V> clearListeners()
    {
        this.listeners.clear();
        return this;
    }

    @Override
    public synchronized V getDef(V def)
    {
        return get(5, TimeUnit.SECONDS, def);
    }

    @Override
    public synchronized V get(long time, TimeUnit timeUnit, V def)
    {
        Validate.checkNotNull(timeUnit);

        try
        {
            return get(time, timeUnit);
        } catch (Throwable ignored)
        {
        }

        return def;
    }

    @Override
    public V call() throws Exception
    {
        if (callable == null || done) return this.value;

        if (!isCancelled())
        {
            try
            {
                this.value = this.callable.call();
            } catch (Throwable throwable)
            {
                this.invokeFailure(throwable);
            }
        }

        if (repeats > 0) repeats--;

        if ((repeats > 0 || repeats == -1) && !cancelled)
        {
            this.delayedTimeStamp = System.currentTimeMillis() + repeat;
        } else
        {
            this.done = true;
            this.invokeTaskListener();

            if (this.wait)
                synchronized (this)
                {
                    this.notifyAll();
                }
        }

        return this.value;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        if (mayInterruptIfRunning)
        {
            callable = null;
            repeats = 0;
        }

        return mayInterruptIfRunning;
    }

    @Override
    public synchronized V get() throws InterruptedException, ExecutionException
    {
        wait = true;
        while (!isDone()) this.wait();

        return value;
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        wait = true;
        if (!isDone()) this.wait(unit.toMillis(timeout));

        return value;
    }

    /*= ---------------------------------------------------------------------------------- =*/

    private void initListenersCollectionIfNotExists()
    {
        if (this.listeners == null)
            this.listeners = new ConcurrentLinkedQueue<>();
    }

    private void invokeTaskListener()
    {
        if (this.listeners != null)
            for (ITaskListener<V> listener : this.listeners)
                try
                {
                    if (this.cancelled)
                        listener.onCancelled(this);
                    else
                        listener.onComplete(this, this.value);
                } catch (Exception exception)
                {
                    exception.printStackTrace();
                }
    }

    private void invokeFailure(Throwable throwable)
    {
        if (this.listeners != null)
            for (ITaskListener<V> listener : this.listeners)
                try
                {
                    listener.onFailure(this, throwable);
                } catch (Exception exception)
                {
                    exception.printStackTrace();
                }
    }

    @Override
    public boolean isRepeatable()
    {
        return repeats > 0 || repeats == -1;
    }

    @Override
    public IScheduledTask<V> setDelayMillis(long delayMillis)
    {
        this.delay = delayMillis;
        return this;
    }

    @Override
    public long getDelayMillis()
    {
        return this.delay;
    }

    @Override
    public IScheduledTask<V> setRepeatMillis(long repeatMillis)
    {
        this.repeat = repeatMillis;
        return this;
    }

    @Override
    public long getRepeatMillis()
    {
        return this.repeat;
    }

    @Override
    public IScheduledTask<V> cancel()
    {
        this.cancel(true);
        return this;
    }
}