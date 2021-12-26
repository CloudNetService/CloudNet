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

public interface Task<V> extends Future<V> {

  @NonNull Task<V> addListener(@NonNull TaskListener<V> listener);

  @NonNull Task<V> clearListeners();

  @UnmodifiableView
  @NonNull Collection<TaskListener<V>> listeners();

  @UnknownNullability V getDef(@Nullable V def);

  @UnknownNullability V get(long time, @NonNull TimeUnit timeUnit, @Nullable V def);

  @NonNull <T> Task<T> map(@NonNull ThrowableFunction<V, T, Throwable> mapper);

  @NonNull
  default Task<V> onComplete(@NonNull Consumer<V> consumer) {
    return this.addListener(new TaskListener<>() {
      @Override
      public void onComplete(@NonNull Task<V> task, @Nullable V v) {
        consumer.accept(v);
      }
    });
  }

  default @NonNull Task<V> onFailure(@NonNull Consumer<Throwable> consumer) {
    return this.addListener(new TaskListener<>() {
      @Override
      public void onFailure(@NonNull Task<V> task, @NonNull Throwable th) {
        consumer.accept(th);
      }
    });
  }

  default @NonNull Task<V> onCancelled(@NonNull Consumer<Task<V>> consumer) {
    return this.addListener(new TaskListener<>() {
      @Override
      public void onCancelled(@NonNull Task<V> task) {
        consumer.accept(task);
      }
    });
  }

  default @NonNull Task<V> fireExceptionOnFailure() {
    return this.onFailure(Throwable::printStackTrace);
  }
}
