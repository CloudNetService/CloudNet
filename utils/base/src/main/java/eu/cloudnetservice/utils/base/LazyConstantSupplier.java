/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.utils.base;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A thread-safe supplier that computes and stores a non-null value on first access. This implementation is basically
 * the same as {@code LazyConstant}, except that it allows to determine if the value is initialized already.
 *
 * @param <T> the supplied value type.
 * @since 4.0
 */
@SuppressWarnings("unchecked")
public final class LazyConstantSupplier<T> implements Supplier<T> {

  private static final VarHandle VH_VALUE;

  static {
    try {
      var lookup = MethodHandles.lookup();
      VH_VALUE = lookup.findVarHandle(LazyConstantSupplier.class, "value", Object.class);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  @SuppressWarnings("unused") // read & updated via VarHandle
  private T value;
  private volatile Supplier<? extends T> computingFunction;

  /**
   * Creates a lazy supplier using the given computing function.
   *
   * @param computingFunction the function used to compute the value.
   * @throws NullPointerException if the given computing function is null.
   */
  public LazyConstantSupplier(@NonNull Supplier<? extends T> computingFunction) {
    this.computingFunction = computingFunction;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull T get() {
    var value = (T) VH_VALUE.getAcquire(this);
    return value != null ? value : this.initialize();
  }

  /**
   * Initializes and returns the value.
   *
   * @return the initialized value.
   */
  private @NonNull T initialize() {
    if (Thread.holdsLock(this)) {
      throw new IllegalStateException("Initialization of LazyConstantSupplier is recursive");
    }

    synchronized (this) {
      var value = (T) VH_VALUE.getAcquire(this);
      if (value == null) {
        value = this.computingFunction.get();
        Objects.requireNonNull(value, "computed value");
        VH_VALUE.setRelease(this, value);
        this.computingFunction = null;
      }

      return value;
    }
  }

  /**
   * Returns whether the value has already been initialized.
   *
   * @return true if the value is initialized, false otherwise.
   */
  public boolean initialized() {
    var value = (T) VH_VALUE.getAcquire(this);
    return value != null;
  }

  /**
   * Returns the initialized value, or the given fallback without initializing it.
   *
   * @param other the fallback value.
   * @return the initialized value, or {@code other} if it is not initialized.
   */
  public @UnknownNullability T orElse(@Nullable T other) {
    var value = (T) VH_VALUE.getAcquire(this);
    return value != null ? value : other;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    // if a value is present show the toString representation of the value
    var value = (T) VH_VALUE.getAcquire(this);
    if (value != null) {
      var valueString = value == this ? "(this supplier)" : value.toString();
      return super.toString() + '[' + valueString + ']';
    }

    // if no value is present then just present the computing function
    var computingFunction = this.computingFunction;
    if (computingFunction != null) {
      return super.toString() + "[cf=" + computingFunction + ']';
    }

    // did a volatile read on the computing function which turned out null,
    // so the underlying value must have been initialized, just try again
    return this.toString();
  }
}
