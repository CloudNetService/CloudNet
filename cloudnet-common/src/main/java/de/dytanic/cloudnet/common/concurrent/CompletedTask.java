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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class CompletedTask<V> implements ITask<V> {

  private static final ITask<Void> VOID_TASK = new CompletedTask<>(null, null);

  private final V value;
  private final Throwable throwable;

  public CompletedTask(V value, Throwable throwable) {
    this.value = value;
    this.throwable = throwable;
  }

  public static <V> ITask<V> createFailed(Throwable throwable) {
    return new CompletedTask<>(null, throwable);
  }

  public static <V> ITask<V> create(V value) {
    return new CompletedTask<>(value, null);
  }

  public static ITask<Void> voidTask() {
    return VOID_TASK;
  }

  @Override
  public @NotNull ITask<V> addListener(ITaskListener<V> listener) {
    if (this.throwable != null) {
      listener.onFailure(this, this.throwable);
    } else {
      listener.onComplete(this, this.value);
    }
    return this;
  }

  @Override
  public @NotNull ITask<V> clearListeners() {
    return this;
  }

  @Override
  public Collection<ITaskListener<V>> getListeners() {
    return Collections.emptyList();
  }

  @Override
  public Callable<V> getCallable() {
    return () -> this.value;
  }

  @Override
  public V getDef(V def) {
    return this.value;
  }

  @Override
  public V get(long time, TimeUnit timeUnit, V def) {
    return this.value;
  }

  @Override
  public <T> ITask<T> mapThrowable(ThrowableFunction<V, T, Throwable> mapper) {
    try {
      return create(mapper == null ? null : mapper.apply(this.value));
    } catch (Throwable exception) {
      return createFailed(this.throwable);
    }
  }

  @Override
  public V call() throws Exception {
    return this.value;
  }

  @Override
  public boolean cancel(boolean b) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public V get() {
    return this.value;
  }

  @Override
  public V get(long l, @NotNull TimeUnit timeUnit) {
    return this.value;
  }
}
