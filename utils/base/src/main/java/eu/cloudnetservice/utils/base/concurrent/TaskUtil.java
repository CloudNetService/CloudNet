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

package eu.cloudnetservice.utils.base.concurrent;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A {@link Future} util, which provides utility methods to supply runnables and functions into a task. Furthermore, the
 * util provides methods to await a task without throwing any exception.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class TaskUtil {

  private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  private TaskUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Runs the given runnable in a fork thread pool and wraps it into a completable future.
   *
   * @param runnable the runnable to run.
   * @return a new completable future containing the given runnable.
   * @throws NullPointerException if the given runnable is null.
   */
  public static @NonNull CompletableFuture<Void> runAsync(@NonNull CheckedRunnable runnable) {
    return supplyAsync(() -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Runs the given runnable in a virtual thread pool and wraps it into a completable future.
   *
   * @param runnable the runnable to run.
   * @return a new completable future containing the given runnable.
   * @throws NullPointerException if the given runnable is null.
   */
  public static @NonNull CompletableFuture<Void> runVirtualAsync(@NonNull CheckedRunnable runnable) {
    return supplyVirtualAsync(() -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Supplies the given supplier into the fork thread pool. A new future is created and completed with the return value
   * of the supplier. Thrown exceptions are caught and passed into the created future.
   *
   * @param supplier the supplier to execute in the thread pool.
   * @param <V>      the generic type of the supplier and the future.
   * @return the new future completing the value of the supplier.
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
   * Supplies the given supplier into a virtual thread pool. A new future is created and completed with the return value
   * of the supplier. Thrown exceptions are caught and passed into the created future.
   *
   * @param supplier the supplier to execute in the thread pool.
   * @param <V>      the generic type of the supplier and the future.
   * @return the new future completing the value of the supplier.
   * @throws NullPointerException if the given supplier is null.
   */
  public static <V> @NonNull CompletableFuture<V> supplyVirtualAsync(@NonNull CheckedFunction0<V> supplier) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return supplier.apply();
      } catch (Throwable throwable) {
        throw new CompletionException(throwable);
      }
    }, VIRTUAL_THREAD_EXECUTOR);
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
  public static <V> @NonNull CompletableFuture<V> finishedFuture(@Nullable Object result) {
    // complete exceptionally if an exception was given
    if (result instanceof Throwable throwable) {
      return CompletableFuture.failedFuture(throwable);
    } else {
      return CompletableFuture.completedFuture((V) result);
    }
  }

  /**
   * This blocks the calling thread until the result of the task is available. If any exception occurs during the
   * execution of the future or the future is cancelled the given default value is returned.
   *
   * @param <V>          the generic type of the future and the result.
   * @param future       the future to await.
   * @param defaultValue the default value returned on failure.
   * @return the completed result of this future or the default value if any exception occurred.
   */
  public static <V> @UnknownNullability V getOrDefault(@NonNull Future<V> future, @Nullable V defaultValue) {
    return getOrDefault(future, Duration.ZERO, defaultValue);
  }

  /**
   * This blocks the calling thread until the result of the future is available. If any exception occurs during the
   * execution of the future or the future is cancelled the given default value is returned. This will also return the
   * default value if the future did not complete the result within the given time-out.
   * <p>
   * If the given timeout is either {@link Duration#ZERO} or a negative duration no timeout is used.
   *
   * @param <V>          the generic type of the future and the result.
   * @param future       the future to await.
   * @param timeout      the duration to wait for the completion before throwing an exception.
   * @param defaultValue the default value returned on failure or if the future did not complete within time-out.
   * @return the completed result of this future or the default value if the future did not complete.
   * @throws NullPointerException if the given future or timeout is null.
   */
  public static <V> @UnknownNullability V getOrDefault(
    @NonNull Future<V> future,
    @NonNull Duration timeout,
    @Nullable V defaultValue
  ) {
    try {
      var useInfiniteTimeout = timeout.isZero() || timeout.isNegative();
      return useInfiniteTimeout ? future.get() : future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
    } catch (CancellationException | ExecutionException | TimeoutException _) {
      return defaultValue;
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return defaultValue;
    }
  }
}
