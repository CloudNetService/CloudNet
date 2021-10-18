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

import de.dytanic.cloudnet.common.function.ThrowableFunction;
import de.dytanic.cloudnet.common.function.ThrowableSupplier;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public class CompletableTask<V> extends CompletableFuture<V> implements ITask<V> {

  private static final ExecutorService SERVICE = Executors.newCachedThreadPool();

  private volatile Collection<ITaskListener<V>> listeners;

  public CompletableTask() {
    // handles the uni completion stage 'done' (or success)
    this.thenAccept(result -> {
      // check if there are registered listeners
      if (this.listeners != null) {
        for (ITaskListener<V> listener : this.listeners) {
          listener.onComplete(this, result);
        }
        // depopulate the listeners - no more completions are possible
        this.depopulateListeners();
      }
    });
    // handles the uni completion stages 'cancel' and 'exceptionally'
    this.exceptionally(throwable -> {
      // check if there are registered listeners
      if (this.listeners != null) {
        // check if the future was cancelled
        if (throwable instanceof CancellationException) {
          // post the cancel result
          for (ITaskListener<V> listener : this.listeners) {
            listener.onCancelled(this);
          }
        } else {
          // exception completion - post that
          for (ITaskListener<V> listener : this.listeners) {
            listener.onFailure(this, throwable);
          }
        }
        // depopulate the listeners - no more completions are possible
        this.depopulateListeners();
      }
      // must be a function...
      return null;
    });
  }

  public static <V> @NotNull CompletableTask<V> supply(@NotNull Runnable runnable) {
    return supply(() -> {
      runnable.run();
      return null;
    });
  }

  public static <V> @NotNull CompletableTask<V> supply(@NotNull ThrowableSupplier<V, Throwable> supplier) {
    CompletableTask<V> task = new CompletableTask<>();
    SERVICE.execute(() -> {
      try {
        task.complete(supplier.get());
      } catch (Throwable throwable) {
        task.completeExceptionally(throwable);
      }
    });
    return task;
  }


  @Override
  public @NotNull ITask<V> addListener(@NotNull ITaskListener<V> listener) {
    this.initListeners().add(listener);
    return this;
  }

  @Override
  public @NotNull ITask<V> clearListeners() {
    // we don't need to initialize the listeners field here
    if (this.listeners != null) {
      this.listeners.clear();
    }

    return this;
  }

  @Override
  public @UnmodifiableView @NotNull Collection<ITaskListener<V>> getListeners() {
    return this.listeners == null ? Collections.emptyList() : Collections.unmodifiableCollection(this.listeners);
  }

  @Override
  public @UnknownNullability V getDef(@Nullable V def) {
    try {
      return this.getNow(def);
    } catch (CancellationException | CompletionException exception) {
      return def;
    }
  }

  @Override
  public @UnknownNullability V get(long time, @NotNull TimeUnit timeUnit, @Nullable V def) {
    try {
      return this.get(time, timeUnit);
    } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException exception) {
      return def;
    }
  }

  @Override
  public @NotNull <T> ITask<T> map(@NotNull ThrowableFunction<V, T, Throwable> mapper) {
    // if this task is already done we can just compute the value
    if (this.isDone()) {
      return CompletedTask.create(() -> mapper.apply(this.getNow(null)));
    }
    // create a new task mapping the current task
    CompletableTask<T> task = new CompletableTask<>();
    // handle the result of this task and post the result to the downstream task
    this.addListener(new ITaskListener<V>() {
      @Override
      public void onComplete(@NotNull ITask<V> t, @Nullable V v) {
        try {
          task.complete(mapper.apply(v));
        } catch (Throwable throwable) {
          task.completeExceptionally(throwable);
        }
      }

      @Override
      public void onCancelled(@NotNull ITask<V> t) {
        task.cancel(true);
      }

      @Override
      public void onFailure(@NotNull ITask<V> t, @NotNull Throwable th) {
        task.completeExceptionally(th);
      }
    });
    // the new task listens now to this task
    return task;
  }

  protected @NotNull Collection<ITaskListener<V>> initListeners() {
    if (this.listeners == null) {
      // ConcurrentLinkedQueue gives us O(1) insertion using CAS - results under moderate
      // load in the fastest insert and read times
      return this.listeners = new ConcurrentLinkedQueue<>();
    } else {
      return this.listeners;
    }
  }

  protected void depopulateListeners() {
    // ensures a better gc
    this.listeners.clear();
    this.listeners = null;
  }
}
