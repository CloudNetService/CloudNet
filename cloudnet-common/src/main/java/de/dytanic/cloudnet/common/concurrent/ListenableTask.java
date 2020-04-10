package de.dytanic.cloudnet.common.concurrent;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@NotNull
public class ListenableTask<V> implements ITask<V> {

    private final Callable<V> callable;
    private Collection<ITaskListener<V>> listeners;
    private volatile V value;
    private volatile boolean done, cancelled;
    private volatile Throwable throwable;

    public ListenableTask(Callable<V> callable) {
        this(callable, null);
    }

    public ListenableTask(Callable<V> callable, ITaskListener<V> listener) {
        Preconditions.checkNotNull(callable);

        this.callable = callable;

        if (listener != null) {
            this.addListener(listener);
        }
    }

    @Override
    public Callable<V> getCallable() {
        return callable;
    }

    @Override
    public Collection<ITaskListener<V>> getListeners() {
        return listeners;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    @NotNull
    public ITask<V> addListener(ITaskListener<V> listener) {
        if (listener == null) {
            return this;
        }

        initListenersCollectionIfNotExists();

        this.listeners.add(listener);

        if (this.done) {
            this.invokeTaskListener(listener);
        }

        return this;
    }

    @Override
    @NotNull
    public ITask<V> clearListeners() {
        if (this.listeners != null) {
            this.listeners.clear();
        }

        return this;
    }

    @Override
    public V getDef(V def) {
        return get(5, TimeUnit.SECONDS, def);
    }

    @Override
    public V get(long time, TimeUnit timeUnit, V def) {
        Preconditions.checkNotNull(timeUnit);

        try {
            return get(time, timeUnit);
        } catch (Throwable ignored) {
            return def;
        }

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.cancelled = mayInterruptIfRunning;
    }

    @Override
    public V get() throws InterruptedException {
        synchronized (this) {
            if (!isDone()) {
                this.wait();
            }
        }

        return value;
    }

    @Override
    public V get(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        synchronized (this) {
            if (!isDone()) {
                this.wait(unit.toMillis(timeout));
            }
        }

        return value;
    }


    @Override
    public V call() {
        if (!isCancelled()) {
            try {
                this.value = this.callable.call();
            } catch (Throwable throwable) {
                this.throwable = throwable;
                this.invokeTaskListener();
            }
        }

        this.done = true;
        this.invokeTaskListener();

        synchronized (this) {
            try {
                this.notifyAll();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
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
                this.invokeTaskListener(listener);
            }
        }
    }

    private void invokeTaskListener(ITaskListener<V> listener) {
        try {
            if (this.throwable != null) {
                listener.onFailure(this, this.throwable);
            }
            if (this.cancelled) {
                listener.onCancelled(this);
            } else {
                listener.onComplete(this, this.value);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public <T> ListenableTask<T> map(Function<V, T> function) {
        AtomicReference<T> reference = new AtomicReference<>();
        ListenableTask<T> task = new ListenableTask<>(reference::get);

        this.onComplete(v -> {
            reference.set(function.apply(v));
            task.call();
        });
        this.onCancelled(viTask -> {
            task.cancelled = true;
            task.invokeTaskListener();
        });
        this.onFailure(throwable -> task.throwable = throwable);

        return task;
    }

}