package de.dytanic.cloudnet.common.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CompletedTask<V> implements ITask<V> {

    private static final ITask<Void> VOID_TASK = new CompletedTask<>(null);

    private final V value;

    private CompletedTask(V value) {
        this.value = value;
    }

    public static <V> ITask<V> create(V value) {
        return new CompletedTask<>(value);
    }

    public static ITask<Void> voidTask() {
        return VOID_TASK;
    }

    @Override
    public @NotNull ITask<V> addListener(ITaskListener<V> listener) {
        listener.onComplete(this, this.value);
        return this;
    }

    @Override
    public @NotNull ITask<V> clearListeners() {
        return this;
    }

    @Override
    public Collection<ITaskListener<V>> getListeners() {
        return Collections.emptyList();
    }

    @Override
    public Callable<V> getCallable() {
        return () -> this.value;
    }

    @Override
    public V getDef(V def) {
        return this.value;
    }

    @Override
    public V get(long time, TimeUnit timeUnit, V def) {
        return this.value;
    }

    @Override
    public V call() throws Exception {
        return this.value;
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public V get() {
        return this.value;
    }

    @Override
    public V get(long l, @NotNull TimeUnit timeUnit) {
        return this.value;
    }
}
