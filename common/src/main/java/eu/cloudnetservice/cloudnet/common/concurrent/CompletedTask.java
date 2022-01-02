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

import eu.cloudnetservice.cloudnet.common.function.ThrowableFunction;
import eu.cloudnetservice.cloudnet.common.function.ThrowableSupplier;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

@SuppressWarnings("unchecked") // uni result is an object - unchecked casts are required
public class CompletedTask<V> implements Task<V> {

  protected static final int UNI_DONE = 0;
  protected static final int UNI_CANCEL = 1;
  protected static final int UNI_EXCEPTIONALLY = 2;

  protected static final CompletedTask<?> CANCELLED = new CompletedTask<>(UNI_CANCEL, null);
  protected static final CompletedTask<?> NULL_SUCCESS = new CompletedTask<>(UNI_DONE, null);

  protected final int uniStage;
  protected final Object uniResult;

  protected CompletedTask(int uniStage, Object uniResult) {
    this.uniStage = uniStage;
    this.uniResult = uniResult;
  }

  public static <T> @NonNull CompletedTask<T> cancelled() {
    return (CompletedTask<T>) CANCELLED;
  }

  public static <T> @NonNull CompletedTask<T> done(@Nullable T result) {
    return result == null ? (CompletedTask<T>) NULL_SUCCESS : new CompletedTask<>(UNI_DONE, result);
  }

  public static <T> @NonNull CompletedTask<T> exceptionally(@NonNull Throwable throwable) {
    return new CompletedTask<>(UNI_EXCEPTIONALLY, throwable);
  }

  public static <T> @NonNull CompletedTask<T> create(@NonNull ThrowableSupplier<T, Throwable> supplier) {
    // 2 possibilities: ok or exception
    try {
      return CompletedTask.done(supplier.get());
    } catch (Throwable throwable) {
      return CompletedTask.exceptionally(throwable);
    }
  }

  @Override
  public @NonNull Task<V> addListener(@NonNull TaskListener<V> listener) {
    // invoke the listener directly based on the result uni stage
    switch (this.uniStage) {
      case UNI_DONE -> listener.onComplete(this, (V) this.uniResult);
      case UNI_CANCEL -> listener.onCancelled(this);
      case UNI_EXCEPTIONALLY -> listener.onFailure(this, (Throwable) this.uniResult);
      default -> throw new IllegalStateException("Invalid uni completion stage " + this.uniStage);
    }

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
    return this.uniStage == UNI_DONE ? (V) this.uniResult : def;
  }

  @Override
  public @UnknownNullability V get(long time, @NonNull TimeUnit timeUnit, @Nullable V def) {
    return this.getDef(null); // redirect to that method - nothing we can wait for
  }

  @Override
  public @NonNull <T> Task<T> map(@NonNull ThrowableFunction<V, T, Throwable> mapper) {
    // if the current is not successful we can return a future holding the same information as this one
    if (this.uniStage != UNI_DONE) {
      return this.uniStage == UNI_CANCEL ? cancelled() : CompletedTask.exceptionally((Throwable) this.uniResult);
    }
    // map the result and create an appropriate result task
    try {
      return CompletedTask.done(mapper.apply((V) this.uniResult));
    } catch (Throwable throwable) {
      return CompletedTask.exceptionally(throwable);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false; // no-op
  }

  @Override
  public boolean isCancelled() {
    return this.uniStage == UNI_CANCEL;
  }

  @Override
  public boolean isDone() {
    return true; // no-op
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    return switch (this.uniStage) {
      case UNI_DONE ->
        // normal completion - return the result
        (V) this.uniResult;
      case UNI_CANCEL ->
        // cancelled - throw cancellation exception
        throw new CancellationException();
      case UNI_EXCEPTIONALLY ->
        // completed with an exception - rethrow
        throw new ExecutionException((Throwable) this.uniResult);
      default ->
        // should not happen
        throw new IllegalStateException("Invalid uni stage result " + this.uniResult);
    };
  }

  @Override
  public V get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return this.get(); // no-op
  }
}
