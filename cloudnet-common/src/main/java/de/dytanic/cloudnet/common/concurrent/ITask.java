package de.dytanic.cloudnet.common.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ITask<V> extends Future<V>, Callable<V> {

    ITask<V> addListener(ITaskListener<V> listener);

    default ITask<V> addListener(ITaskListener<V>... listeners) {
        for (ITaskListener<V> listener : listeners) {
            this.addListener(listener);
        }
        return this;
    }

    default ITask<V> onComplete(BiConsumer<ITask<V>, V> consumer) {
        return this.addListener(new ITaskListener<V>() {
            @Override
            public void onComplete(ITask<V> task, V v) {
                consumer.accept(task, v);
            }
        });
    }

    default ITask<V> onComplete(Consumer<V> consumer) {
        return this.onComplete((task, v) -> consumer.accept(v));
    }

    default ITask<V> onFailure(BiConsumer<ITask<V>, Throwable> consumer) {
        return this.addListener(new ITaskListener<V>() {
            @Override
            public void onFailure(ITask<V> task, Throwable th) {
                consumer.accept(task, th);
            }
        });
    }

    default ITask<V> onFailure(Consumer<Throwable> consumer) {
        return this.onFailure((task, th) -> consumer.accept(th));
    }

    default ITask<V> onCancelled(Consumer<ITask<V>> consumer) {
        return this.addListener(new ITaskListener<V>() {
            @Override
            public void onCancelled(ITask<V> task) {
                consumer.accept(task);
            }
        });
    }

    ITask<V> clearListeners();

    Collection<ITaskListener<V>> getListeners();

    V getDef(V def);

    V get(long time, TimeUnit timeUnit, V def);
}