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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public interface ITask<V> extends Future<V> {

  @NonNull ITask<V> addListener(@NonNull ITaskListener<V> listener);

  @NonNull ITask<V> clearListeners();

  @UnmodifiableView
  @NonNull Collection<ITaskListener<V>> listeners();

  @UnknownNullability V getDef(@Nullable V def);

  @UnknownNullability V get(long time, @NonNull TimeUnit timeUnit, @Nullable V def);

  @NonNull <T> ITask<T> map(@NonNull ThrowableFunction<V, T, Throwable> mapper);

  @NonNull
  default ITask<V> onComplete(@NonNull Consumer<V> consumer) {
    return this.addListener(new ITaskListener<>() {
      @Override
      public void onComplete(@NonNull ITask<V> task, @Nullable V v) {
        consumer.accept(v);
      }
    });
  }

  default @NonNull ITask<V> onFailure(@NonNull Consumer<Throwable> consumer) {
    return this.addListener(new ITaskListener<>() {
      @Override
      public void onFailure(@NonNull ITask<V> task, @NonNull Throwable th) {
        consumer.accept(th);
      }
    });
  }

  default @NonNull ITask<V> onCancelled(@NonNull Consumer<ITask<V>> consumer) {
    return this.addListener(new ITaskListener<>() {
      @Override
      public void onCancelled(@NonNull ITask<V> task) {
        consumer.accept(task);
      }
    });
  }

  default @NonNull ITask<V> fireExceptionOnFailure() {
    return this.onFailure(Throwable::printStackTrace);
  }
}
