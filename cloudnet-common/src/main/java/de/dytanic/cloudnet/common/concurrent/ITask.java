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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ITask<V> extends Future<V>, Callable<V> {

  @NotNull
  ITask<V> addListener(ITaskListener<V> listener);

  @NotNull
  default ITask<V> addListener(ITaskListener<V>... listeners) {
    for (ITaskListener<V> listener : listeners) {
      this.addListener(listener);
    }
    return this;
  }

  @NotNull
  default ITask<V> onComplete(BiConsumer<ITask<V>, V> consumer) {
    return this.addListener(new ITaskListener<V>() {
      @Override
      public void onComplete(ITask<V> task, V v) {
        consumer.accept(task, v);
      }
    });
  }

  @NotNull
  default ITask<V> onComplete(Consumer<V> consumer) {
    return this.onComplete((task, v) -> consumer.accept(v));
  }

  @NotNull
  default ITask<V> onFailure(BiConsumer<ITask<V>, Throwable> consumer) {
    return this.addListener(new ITaskListener<V>() {
      @Override
      public void onFailure(ITask<V> task, Throwable th) {
        consumer.accept(task, th);
      }
    });
  }

  @NotNull
  default ITask<V> onFailure(Consumer<Throwable> consumer) {
    return this.onFailure((task, th) -> consumer.accept(th));
  }

  @NotNull
  default ITask<V> onCancelled(Consumer<ITask<V>> consumer) {
    return this.addListener(new ITaskListener<V>() {
      @Override
      public void onCancelled(ITask<V> task) {
        consumer.accept(task);
      }
    });
  }

  default ITask<V> fireExceptionOnFailure() {
    return this.onFailure((Consumer<Throwable>) Throwable::printStackTrace);
  }

  @NotNull
  ITask<V> clearListeners();

  Collection<ITaskListener<V>> getListeners();

  Callable<V> getCallable();

  V getDef(V def);

  V get(long time, TimeUnit timeUnit, V def);

  <T> ITask<T> mapThrowable(@Nullable ThrowableFunction<V, T, Throwable> mapper);

  default <T> ITask<T> map(@Nullable Function<V, T> mapper) {
    return this.mapThrowable(mapper == null ? null : mapper::apply);
  }

}
