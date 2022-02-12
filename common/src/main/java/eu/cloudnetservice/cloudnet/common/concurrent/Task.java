/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.common.concurrent;

import eu.cloudnetservice.cloudnet.common.function.ThrowableSupplier;
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

public class Task<V> extends CompletableFuture<V> {

  private static final ExecutorService SERVICE = Executors.newCachedThreadPool();

  public static <V> @NonNull Task<V> supply(@NonNull Runnable runnable) {
    return supply(() -> {
      runnable.run();
      return null;
    });
  }

  public static <V> @NonNull Task<V> supply(@NonNull ThrowableSupplier<V, Throwable> supplier) {
    var task = new Task<V>();
    SERVICE.execute(() -> {
      try {
        task.complete(supplier.get());
      } catch (Throwable throwable) {
        task.completeExceptionally(throwable);
      }
    });
    return task;
  }

  public static <V> @NonNull Task<V> wrapFuture(@NonNull CompletableFuture<V> future) {
    var task = new Task<V>();
    future.whenComplete((result, exception) -> {
      // uni push either the exception or the result, the exception is unwrapped already
      if (exception == null) {
        task.complete(result);
      } else {
        task.completeExceptionally(exception);
      }
    });
    return task;
  }

  @SuppressWarnings("unchecked") // it's fine
  public static <V> @NonNull Task<V> completedTask(@Nullable Object result) {
    var future = new Task<V>();
    // complete exceptionally if an exception was given
    if (result instanceof Throwable throwable) {
      future.completeExceptionally(throwable);
    } else {
      future.complete((V) result);
    }
    // instantly completed when returning
    return future;
  }

  public @UnknownNullability V getOrNull() {
    return this.getDef(null);
  }

  public @UnknownNullability V getDef(@Nullable V def) {
    try {
      return this.join();
    } catch (CancellationException | CompletionException exception) {
      return def;
    }
  }

  public @UnknownNullability V get(long time, @NonNull TimeUnit timeUnit, @Nullable V def) {
    try {
      return this.get(time, timeUnit);
    } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException exception) {
      return def;
    }
  }
}
