package de.dytanic.cloudnet.common.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface ITask<V> extends Future<V>, Callable<V> {

    ITask<V> addListener(ITaskListener<V>... listeners);

    ITask<V> clearListeners();

    Collection<ITaskListener<V>> getListeners();

    V getDef(V def);

    V get(long time, TimeUnit timeUnit, V def);
}