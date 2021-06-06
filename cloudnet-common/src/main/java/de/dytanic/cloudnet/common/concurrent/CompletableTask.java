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

import de.dytanic.cloudnet.common.concurrent.function.ThrowableFunction;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableSupplier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;

public class CompletableTask<V> implements ITask<V> {

  private static final ExecutorService SERVICE = Executors.newCachedThreadPool();

  private final Collection<ITaskListener<V>> listeners = new ArrayList<>();

  private final CompletableFuture<V> future;

  private Throwable throwable;

  public CompletableTask() {
    this(new CompletableFuture<>());
  }

  private CompletableTask(CompletableFuture<V> future) {
    this.future = future;
    this.future.exceptionally(throwable -> {
      this.throwable = throwable;
      return null;
    });
  }

  public static <V> CompletableTask<V> supplyAsync(ThrowableSupplier<V, Throwable> supplier) {
    CompletableTask<V> task = new CompletableTask<>();
    SERVICE.execute(() -> {
      try {
        task.complete(supplier.get());
      } catch (Throwable throwable) {
        task.fail(throwable);
      }
    });
    return task;
  }

  public static <I, O> ITask<O> mapFrom(ITask<I> source, ThrowableFunction<I, O, Throwable> mapper) {
    CompletableTask<O> result = new CompletableTask<>();
    source.addListener(new ITaskListener<I>() {
      @Override
      public void onComplete(ITask<I> task, I i) {
        try {
          result.complete(mapper == null ? null : mapper.apply(i));
        } catch (Throwable throwable) {
          result.fail(throwable);
        }
      }

      @Override
      public void onCancelled(ITask<I> task) {
        result.cancel(true);
      }

      @Override
      public void onFailure(ITask<I> task, Throwable throwable) {
        result.fail(throwable);
      }
    });
    return result;
  }

  @Override
  public @NotNull ITask<V> addListener(ITaskListener<V> listener) {
    if (this.future.isDone()) {
      if (this.future.isCancelled()) {
        listener.onCancelled(this);
      } else if (this.throwable != null) {
        listener.onFailure(this, this.throwable);
      } else {
        listener.onComplete(this, this.future.getNow(null));
      }
      return this;
    }

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
    return this.future::get;
  }

  @Override
  public V getDef(V def) {
    return this.get(5, TimeUnit.SECONDS, def);
  }

  @Override
  public V get(long time, TimeUnit timeUnit, V def) {
    try {
      return this.get(time, timeUnit);
    } catch (InterruptedException | ExecutionException | TimeoutException exception) {
      return def;
    }
  }

  public void fail(Throwable throwable) {
    this.throwable = throwable;
    this.future.completeExceptionally(throwable);
    for (ITaskListener<V> listener : this.listeners) {
      listener.onFailure(this, throwable);
    }
  }

  @Override
  public V call() {
    if (this.future.isDone()) {
      return this.future.getNow(null);
    }
    throw new UnsupportedOperationException("Use #complete in the CompletableTask");
  }

  public void complete(V value) {
    this.future.complete(value);
    for (ITaskListener<V> listener : this.listeners) {
      listener.onComplete(this, value);
    }
  }

  @Override
  public boolean cancel(boolean b) {
    if (this.future.isCancelled()) {
      return false;
    }

    if (this.future.cancel(b)) {
      for (ITaskListener<V> listener : this.listeners) {
        listener.onCancelled(this);
      }
      return true;
    }
    return false;
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

  @Override
  public <T> ITask<T> mapThrowable(ThrowableFunction<V, T, Throwable> mapper) {
    CompletableTask<T> task = new CompletableTask<>();
    this.future.thenAccept(v -> {
      try {
        task.complete(mapper == null ? null : mapper.apply(v));
      } catch (Throwable throwable) {
        task.fail(throwable);
      }
    });
    this.onFailure(task.future::completeExceptionally);
    this.onCancelled(otherTask -> task.cancel(true));
    return task;
  }
}
