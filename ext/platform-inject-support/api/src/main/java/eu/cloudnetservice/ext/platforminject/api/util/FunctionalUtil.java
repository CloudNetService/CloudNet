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

package eu.cloudnetservice.ext.platforminject.api.util;

import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class FunctionalUtil {

  private static final Function<?, Object> IDENTITY = Function.identity();

  private FunctionalUtil() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  public static @NonNull <I> Function<I, Object> identity() {
    return (Function<I, Object>) IDENTITY;
  }

  public static @NonNull <T> Supplier<T> memoizing(@NonNull Supplier<T> delegate) {
    return new MemoizingSupplier<>(delegate);
  }

  private static final class MemoizingSupplier<T> implements Supplier<T> {

    private Supplier<T> delegate;

    private T value; // no need for volatile, this is already handled by the initialized boolean
    private volatile boolean initialized;

    private MemoizingSupplier(@NonNull Supplier<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public @Nullable T get() {
      // double check locking to prevent unneeded synchronization
      if (!this.initialized) {
        synchronized (this) {
          if (!this.initialized) {
            // set the delegate value & mark as initialized
            this.initialized = true;
            this.value = this.delegate.get();

            // release the delegate supplier for gc
            this.delegate = null;
            return this.value;
          }
        }
      }
      // already set
      return this.value;
    }
  }
}
