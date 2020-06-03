package de.dytanic.cloudnet.common.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class CountingTask<V> implements ITask<V> {

    private final V value;
    private final CompletableFuture<V> future = new CompletableFuture<>();
    private final Collection<ITaskListener<V>> listeners = new ArrayList<>();
    private AtomicInteger count;

    public CountingTask(V value, int initialCount) {
        this.value = value;
        this.count = new AtomicInteger(initialCount);
    }

    public void incrementCount() {
        this.count.getAndIncrement();
    }

    public void countDown() {
        if (this.count.decrementAndGet() <= 0) {
            for (ITaskListener<V> listener : this.listeners) {
                listener.onComplete(this, this.value);
            }
            this.future.complete(this.value);
        }
    }

    public int currentCount() {
        return this.count.get();
    }

    @Override
    public @NotNull ITask<V> addListener(ITaskListener<V> listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public @NotNull ITask<V> clearListeners() {
        this.listeners.clear();
        return this;
    }

    @Override
    public Collection<ITaskListener<V>> getListeners() {
        return this.listeners;
    }

    @Override
    public Callable<V> getCallable() {
        return () -> this.value;
    }

    @Override
    public V getDef(V def) {
        return this.get(5, TimeUnit.SECONDS, def);
    }

    @Override
    public V get(long time, TimeUnit timeUnit, V def) {
        try {
            return this.future.get(time, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            return def;
        }
    }

    @Override
    public <T> ITask<T> map(Function<V, T> mapper) {
        CompletableTask<T> task = new CompletableTask<>();
        this.onComplete(v -> task.complete(mapper.apply(v)));
        this.onCancelled(otherTask -> task.cancel(true));
        return task;
    }

    @Override
    public V call() throws Exception {
        return this.value;
    }

    @Override
    public boolean cancel(boolean b) {
        if (this.future.isCancelled()) {
            return false;
        }

        for (ITaskListener<V> listener : this.listeners) {
            listener.onCancelled(this);
        }
        return this.future.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    @Override
    public V get(long l, @NotNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.future.get(l, timeUnit);
    }
}
