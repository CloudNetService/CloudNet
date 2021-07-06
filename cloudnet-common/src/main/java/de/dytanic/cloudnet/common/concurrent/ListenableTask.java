/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.common.concurrent;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableFunction;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

@NotNull
public class ListenableTask<V> implements ITask<V> {

  private final Callable<V> callable;
  private Collection<ITaskListener<V>> listeners;

  private volatile V value;
  private volatile boolean done;
  private volatile boolean cancelled;
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
    return this.callable;
  }

  @Override
  public Collection<ITaskListener<V>> getListeners() {
    return this.listeners;
  }

  public V getValue() {
    return this.value;
  }

  @Override
  public boolean isDone() {
    return this.done;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  @NotNull
  public ITask<V> addListener(ITaskListener<V> listener) {
    if (listener == null) {
      return this;
    }

    this.initListenersCollectionIfNotExists();

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
    return this.get(5, TimeUnit.SECONDS, def);
  }

  @Override
  public V get(long time, TimeUnit timeUnit, V def) {
    Preconditions.checkNotNull(timeUnit);

    try {
      return this.get(time, timeUnit);
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
      if (!this.isDone()) {
        this.wait();
      }
    }

    return this.value;
  }

  @Override
  public V get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, TimeoutException {
    synchronized (this) {
      if (!this.isDone()) {
        this.wait(unit.toMillis(timeout));
      }
    }

    if (!this.isDone()) {
      throw new TimeoutException("Task has not been called within the given time!");
    }

    return this.value;
  }


  @Override
  public V call() {
    if (!this.isCancelled()) {
      try {
        this.value = this.callable.call();
      } catch (Throwable throwable) {
        this.throwable = throwable;
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

  @Override
  public <T> ListenableTask<T> mapThrowable(ThrowableFunction<V, T, Throwable> function) {
    AtomicReference<T> reference = new AtomicReference<>();
    ListenableTask<T> task = new ListenableTask<>(reference::get);

    this.onComplete(v -> {
      try {
        reference.set(function == null ? null : function.apply(v));
      } catch (Throwable throwable) {
        task.throwable = throwable;
      }
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
