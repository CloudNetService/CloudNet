/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.common.concurrent;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * This task is a {@link java.util.concurrent.Future} which can be completed at any time in the future and provides some
 * useful shortcuts for completable futures.
 *
 * @since 4.0
 */
public class Task {

  /**
   * Supplies and executes the given runnable in a cached thread pool and wraps it into a task.
   *
   * @param runnable the runnable to run.
   * @return a new task containing the given runnable.
   * @throws NullPointerException if the given runnable is null.
   */
  public static @NonNull CompletableFuture<Void> runAsync(@NonNull CheckedRunnable runnable) {
    return supplyAsync(() -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Supplies the given supplier into the cached thread pool. A new task is created and completed with the return value
   * of the supplier. Thrown exceptions are caught and passed into the created task.
   *
   * @param supplier the supplier to execute in the thread pool.
   * @param <V>      the generic type of the supplier and the task.
   * @return the new task completing the value of the supplier.
   * @throws NullPointerException if the given supplier is null.
   */
  public static <V> @NonNull CompletableFuture<V> supplyAsync(@NonNull CheckedFunction0<V> supplier) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return supplier.apply();
      } catch (Throwable throwable) {
        throw new CompletionException(throwable);
      }
    });
  }

  /**
   * Creates a new task that already has a defined result. If the given result is a throwable the task is completed
   * exceptionally {@link CompletableFuture#failedFuture(Throwable)} instead of completing a normal result.
   *
   * @param result the result for the new task.
   * @param <V>    the generic type of the new task.
   * @return the new task completed with the given result.
   */
  @SuppressWarnings("unchecked") // it's fine
  public static <V> @NonNull CompletableFuture<V> completedTask(@Nullable Object result) {
    // complete exceptionally if an exception was given
    if (result instanceof Throwable throwable) {
      return CompletableFuture.failedFuture(throwable);
    } else {
      return CompletableFuture.completedFuture((V) result);
    }
  }

  /**
   * This blocks the calling thread until the result of the task is available. If any exception occurs during the
   * execution of the task or the task is cancelled null is returned as result. This method is equivalent to
   * {@code task.getDef(null)}.
   *
   * @return the completed result of this task, null if any exception occurred.
   */
  public static <V> @UnknownNullability V getOrNull(@NonNull CompletableFuture<V> future) {
    return getDef(future, null);
  }

  /**
   * This blocks the calling thread until the result of the task is available. If any exception occurs during the
   * execution of the task or the task is cancelled the given default value is returned.
   *
   * @param def the default value returned on failure.
   * @return the completed result of this task or the default value if any exception occurred.
   */
  public static <V> @UnknownNullability V getDef(@NonNull CompletableFuture<V> future, @Nullable V def) {
    try {
      return future.join();
    } catch (CancellationException | CompletionException exception) {
      return def;
    }
  }

  /**
   * This blocks the calling thread until the result of the task is available. If any exception occurs during the
   * execution of the task or the task is cancelled the given default value is returned. This will also return the
   * default value if the task did not complete the result within the given time-out.
   *
   * @param time     the time to wait for the completion of the result.
   * @param timeUnit the time unit of the time.
   * @param def      the default value returned on failure or if the task did not complete within time-out.
   * @return the completed result of this task or the default value if the task did not complete.
   * @throws NullPointerException if the given time unit is null.
   */
  public static <V> @UnknownNullability V get(
    @NonNull CompletableFuture<V> future,
    long time,
    @NonNull TimeUnit timeUnit,
    @Nullable V def
  ) {
    try {
      return future.get(time, timeUnit);
    } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException exception) {
      return def;
    }
  }
}
