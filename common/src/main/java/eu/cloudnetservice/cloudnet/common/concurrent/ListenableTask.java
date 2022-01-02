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
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public class ListenableTask<V> extends FutureTask<V> implements Task<V> {

  private volatile Collection<TaskListener<V>> listeners;

  public ListenableTask(@NonNull Callable<V> callable) {
    super(callable);
  }

  @Override
  protected void done() {
    // check if we have listeners
    if (this.listeners == null) {
      return;
    }
    // check if the task was cancelled
    if (this.isCancelled()) {
      for (var listener : this.listeners) {
        listener.onCancelled(this);
      }
    } else {
      // populate the result that get() gives us
      try {
        var result = this.get();
        for (var listener : this.listeners) {
          listener.onComplete(this, result);
        }
      } catch (InterruptedException | ExecutionException exception) {
        // we know that the task is done - it can only be a ExecutionException wrapping the original exception
        for (var listener : this.listeners) {
          listener.onFailure(this, exception.getCause());
        }
      }
    }
    // remove all listeners
    this.depopulateListeners();
  }

  @Override
  public @NonNull Task<V> addListener(@NonNull TaskListener<V> listener) {
    this.initListeners().add(listener);
    return this;
  }

  @Override
  public @NonNull Task<V> clearListeners() {
    // we don't need to initialize the listeners field here
    if (this.listeners != null) {
      this.listeners.clear();
    }

    return this;
  }

  @Override
  public @UnmodifiableView @NonNull Collection<TaskListener<V>> listeners() {
    return this.listeners == null ? Collections.emptyList() : Collections.unmodifiableCollection(this.listeners);
  }

  @Override
  public @UnknownNullability V getDef(@Nullable V def) {
    try {
      return this.get();
    } catch (InterruptedException | ExecutionException | CancellationException exception) {
      return def;
    }
  }

  @Override
  public @UnknownNullability V get(long time, @NonNull TimeUnit timeUnit, @Nullable V def) {
    try {
      return this.get(time, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException | CancellationException exception) {
      return def;
    }
  }

  @Override
  public @NonNull <T> Task<T> map(@NonNull ThrowableFunction<V, T, Throwable> mapper) {
    return CompletableTask.supply(() -> mapper.apply(this.get()));
  }

  @Override
  public boolean runAndReset() {
    return super.runAndReset();
  }

  protected @NonNull Collection<TaskListener<V>> initListeners() {
    // ConcurrentLinkedQueue gives us O(1) insertion using CAS - results under moderate
    // load in the fastest insert and read times
    return Objects.requireNonNullElseGet(this.listeners, () -> this.listeners = new ConcurrentLinkedQueue<>());
  }

  protected void depopulateListeners() {
    // ensures a better gc
    this.listeners.clear();
    this.listeners = null;
  }
}
