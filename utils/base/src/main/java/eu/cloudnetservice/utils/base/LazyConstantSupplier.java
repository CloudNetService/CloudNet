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
 * A thread-safe supplier that computes and stores a non-null value on first access.
 *
 * @param <T> the supplied value type.
 * @since 4.0
 */
@SuppressWarnings("unchecked")
public final class LazyConstantSupplier<T> implements Supplier<T> {

  private static final VarHandle VH_VALUE;

  static {
    try {
      VH_VALUE = MethodHandles.lookup().findVarHandle(LazyConstantSupplier.class, "value", Object.class);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  @SuppressWarnings("unused") // read and updated via VarHandle
  private T value;
  private volatile Supplier<? extends T> computingFunction;

  public LazyConstantSupplier(@NonNull Supplier<? extends T> computingFunction) {
    this.computingFunction = computingFunction;
  }

  @Override
  public @NonNull T get() {
    var value = (T) VH_VALUE.getAcquire(this);
    return value != null ? value : this.initialize();
  }

  private @NonNull T initialize() {
    if (Thread.holdsLock(this)) {
      throw new IllegalStateException("Initialization of LazyConstantSupplier is recursive");
    }

    synchronized (this) {
      var value = (T) VH_VALUE.getAcquire(this);
      if (value == null) {
        value = Objects.requireNonNull(this.computingFunction.get(), "computed value");
        VH_VALUE.setRelease(this, value);
        this.computingFunction = null;
      }
      return value;
    }
  }

  public boolean initialized() {
    return VH_VALUE.getAcquire(this) != null;
  }

  public @UnknownNullability T orElse(@Nullable T other) {
    var value = (T) VH_VALUE.getAcquire(this);
    return value != null ? value : other;
  }

  @Override
  public String toString() {
    var value = (T) VH_VALUE.getAcquire(this);
    if (value != null) {
      var valueString = value == this ? "(this supplier)" : value.toString();
      return super.toString() + '[' + valueString + ']';
    }
    var computingFunction = this.computingFunction;
    if (computingFunction != null) {
      return super.toString() + "[cf=" + computingFunction + ']';
    }
    return this.toString();
  }
}
