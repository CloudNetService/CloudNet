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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * Supplier that computes the underlying value only once, lazily.
 *
 * @param <T> the type of results supplied by this supplier
 * @since 4.0
 */
@Deprecated // in favor of stable values
final class LazyMemoizingSupplier<T> implements Supplier<T> {

  private static final VarHandle WRAPPED_VAR_HANDLE;

  static {
    try {
      var lookup = MethodHandles.lookup();
      WRAPPED_VAR_HANDLE = lookup.findVarHandle(LazyMemoizingSupplier.class, "wrapped", Object.class);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private T wrapped; // lazily initialized on first access using var handles; the field is not unused
  private Supplier<T> delegateSupplier;

  /**
   * Constructs a new instance using the given supplier as the wrapped delegate. The given delegate supplier is not
   * allowed to return a null value.
   *
   * @param delegateSupplier the supplier to use to initialize the wrapped value.
   * @throws NullPointerException if the given delegate supplier is null.
   */
  public LazyMemoizingSupplier(@NonNull Supplier<T> delegateSupplier) {
    this.delegateSupplier = delegateSupplier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NonNull T get() {
    var wrapped = WRAPPED_VAR_HANDLE.getAcquire(this);
    if (wrapped != null) {
      return (T) wrapped;
    }

    synchronized (this) {
      wrapped = this.wrapped; // plain access is safe due to locking
      if (wrapped != null) {
        return (T) wrapped;
      }

      var newValue = this.delegateSupplier.get();
      Objects.requireNonNull(newValue, "delegate supplier must not return null");
      this.delegateSupplier = null; // release delegate for GC, no longer needed
      WRAPPED_VAR_HANDLE.setRelease(this, newValue);
      return newValue;
    }
  }
}
