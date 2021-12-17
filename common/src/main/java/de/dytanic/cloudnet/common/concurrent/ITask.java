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
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public interface ITask<V> extends Future<V> {

  @NotNull ITask<V> addListener(@NotNull ITaskListener<V> listener);

  @NotNull ITask<V> clearListeners();

  @UnmodifiableView
  @NotNull Collection<ITaskListener<V>> listeners();

  @UnknownNullability V getDef(@Nullable V def);

  @UnknownNullability V get(long time, @NotNull TimeUnit timeUnit, @Nullable V def);

  @NotNull <T> ITask<T> map(@NotNull ThrowableFunction<V, T, Throwable> mapper);

  @NotNull
  default ITask<V> onComplete(@NotNull Consumer<V> consumer) {
    return this.addListener(new ITaskListener<V>() {
      @Override
      public void onComplete(@NotNull ITask<V> task, @Nullable V v) {
        consumer.accept(v);
      }
    });
  }

  default @NotNull ITask<V> onFailure(@NotNull Consumer<Throwable> consumer) {
    return this.addListener(new ITaskListener<V>() {
      @Override
      public void onFailure(@NotNull ITask<V> task, @NotNull Throwable th) {
        consumer.accept(th);
      }
    });
  }

  default @NotNull ITask<V> onCancelled(@NotNull Consumer<ITask<V>> consumer) {
    return this.addListener(new ITaskListener<V>() {
      @Override
      public void onCancelled(@NotNull ITask<V> task) {
        consumer.accept(task);
      }
    });
  }

  default @NotNull ITask<V> fireExceptionOnFailure() {
    return this.onFailure(Throwable::printStackTrace);
  }
}
