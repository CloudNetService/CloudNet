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

import java.util.concurrent.Callable;
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

/**
 * This task is a {@link java.util.concurrent.Future} which can be completed at any time in the future and provides some
 * useful shortcuts for completable futures.
 *
 * @param <V> the generic type of the value to complete.
 * @since 4.0
 */
public class Task<V> extends CompletableFuture<V> {

  private static final ExecutorService SERVICE = Executors.newCachedThreadPool();

  /**
   * Supplies and executes the given runnable in a cached thread pool and wraps it into a task.
   *
   * @param runnable the runnable to run.
   * @param <V>      the generic type of the task.
   * @return a new task containing the given runnable.
   * @throws NullPointerException if the given runnable is null.
   */
  public static <V> @NonNull Task<V> supply(@NonNull Runnable runnable) {
    return supply(() -> {
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
  public static <V> @NonNull Task<V> supply(@NonNull Callable<V> supplier) {
    var task = new Task<V>();
    SERVICE.execute(() -> {
      try {
        task.complete(supplier.call());
      } catch (Exception exception) {
        task.completeExceptionally(exception);
      }
    });
    return task;
  }

  /**
   * Wraps the given completable future into a cloudnet task. Exceptions thrown in the future are passed down to the
   * task.
   *
   * @param future the future to wrap as to a task.
   * @param <V>    the generic type of the completable future and the new task.
   * @return the new task wrapping the completable future.
   * @throws NullPointerException if the given future is null.
   */
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

  /**
   * Creates a new task that already has a defined result. If the given result is a throwable the task is completed
   * exceptionally {@link Task#completeExceptionally(Throwable)} instead of completing a normal result.
   *
   * @param result the result for the new task.
   * @param <V>    the generic type of the new task.
   * @return the new task completed with the given result.
   */
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

  /**
   * This blocks the calling thread until the result of the task is available. If any exception occurs during the
   * execution of the task or the task is cancelled null is returned as result. This method is equivalent to
   * {@code task.getDef(null)}.
   *
   * @return the completed result of this task, null if any exception occurred.
   */
  public @UnknownNullability V getOrNull() {
    return this.getDef(null);
  }

  /**
   * This blocks the calling thread until the result of the task is available. If any exception occurs during the
   * execution of the task or the task is cancelled the given default value is returned.
   *
   * @param def the default value returned on failure.
   * @return the completed result of this task or the default value if any exception occurred.
   */
  public @UnknownNullability V getDef(@Nullable V def) {
    try {
      return this.join();
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
  public @UnknownNullability V get(long time, @NonNull TimeUnit timeUnit, @Nullable V def) {
    try {
      return this.get(time, timeUnit);
    } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException exception) {
      return def;
    }
  }
}
