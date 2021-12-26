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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public class CompletableTask<V> extends CompletableFuture<V> implements Task<V> {

  private static final ExecutorService SERVICE = Executors.newCachedThreadPool();

  public static <V> @NonNull CompletableTask<V> supply(@NonNull Runnable runnable) {
    return supply(() -> {
      runnable.run();
      return null;
    });
  }

  public static <V> @NonNull CompletableTask<V> supply(@NonNull ThrowableSupplier<V, Throwable> supplier) {
    var task = new CompletableTask<V>();
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
  public @NonNull Task<V> addListener(@NonNull TaskListener<V> listener) {
    this.whenComplete((result, exception) -> {
      // cancelled and exceptionally are both here
      if (exception != null) {
        if (exception instanceof CancellationException) {
          listener.onCancelled(this);
        } else {
          listener.onFailure(this, exception);
        }
      } else {
        // normal completion
        listener.onComplete(this, result);
      }
    });
    return this;
  }

  @Override
  public @NonNull Task<V> clearListeners() {
    return this; // no-op
  }

  @Override
  public @UnmodifiableView @NonNull Collection<TaskListener<V>> listeners() {
    return Collections.emptyList(); // no-op
  }

  @Override
  public @UnknownNullability V getDef(@Nullable V def) {
    try {
      return this.join();
    } catch (CancellationException | CompletionException exception) {
      return def;
    }
  }

  @Override
  public @UnknownNullability V get(long time, @NonNull TimeUnit timeUnit, @Nullable V def) {
    try {
      return this.get(time, timeUnit);
    } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException exception) {
      return def;
    }
  }

  @Override
  public @NonNull <T> Task<T> map(@NonNull ThrowableFunction<V, T, Throwable> mapper) {
    // if this task is already done we can just compute the value
    if (this.isDone()) {
      return CompletedTask.create(() -> mapper.apply(this.getNow(null)));
    }
    // create a new task mapping the current task
    var task = new CompletableTask<T>();
    // handle the result of this task and post the result to the downstream task
    this.addListener(new TaskListener<>() {
      @Override
      public void onComplete(@NonNull Task<V> t, @Nullable V v) {
        try {
          task.complete(mapper.apply(v));
        } catch (Throwable throwable) {
          task.completeExceptionally(throwable);
        }
      }

      @Override
      public void onCancelled(@NonNull Task<V> t) {
        task.cancel(true);
      }

      @Override
      public void onFailure(@NonNull Task<V> t, @NonNull Throwable th) {
        task.completeExceptionally(th);
      }
    });
    // the new task listens now to this task
    return task;
  }
}

