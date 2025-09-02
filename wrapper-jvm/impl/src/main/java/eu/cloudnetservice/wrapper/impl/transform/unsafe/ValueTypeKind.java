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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Kinds of values that can be accessed using unsafe. Differentiates between all primitive types and reference types.
 *
 * @since 4.0
 */
@SuppressWarnings("deprecation") // uses LazyMemoizingSupplier
enum ValueTypeKind {

  // CHECKSTYLE.OFF: checkstyle isn't really helping here
  BYTE(
    (left, right) -> (byte) left == (byte) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(byte[].class)),
    arr -> MemorySegment.ofArray((byte[]) arr),
    ValueLayout.JAVA_BYTE),
  SHORT(
    (left, right) -> (short) left == (short) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(short[].class)),
    arr -> MemorySegment.ofArray((short[]) arr),
    ValueLayout.JAVA_SHORT),
  INT(
    (left, right) -> (int) left == (int) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(int[].class)),
    arr -> MemorySegment.ofArray((int[]) arr),
    ValueLayout.JAVA_INT),
  LONG(
    (left, right) -> (long) left == (long) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(long[].class)),
    arr -> MemorySegment.ofArray((long[]) arr),
    ValueLayout.JAVA_LONG),
  FLOAT(
    (left, right) -> (float) left == (float) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(float[].class)),
    arr -> MemorySegment.ofArray((float[]) arr),
    ValueLayout.JAVA_FLOAT),
  DOUBLE(
    (left, right) -> (double) left == (double) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(double[].class)),
    arr -> MemorySegment.ofArray((double[]) arr),
    ValueLayout.JAVA_DOUBLE),
  BOOL(
    (left, right) -> (boolean) left == (boolean) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(boolean[].class)),
    arr -> {
      // boolean arrays aren't natively supported, but they are basically byte arrays with 0/1 values
      var boolArray = (boolean[]) arr;
      var targetArray = new byte[boolArray.length];
      for (var index = 0; index < boolArray.length; index++) {
        targetArray[index] = (byte) (boolArray[index] ? 1 : 0);
      }
      return MemorySegment.ofArray(targetArray);
    },
    ValueLayout.JAVA_BOOLEAN),
  CHAR(
    (left, right) -> (char) left == (char) right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(char[].class)),
    arr -> MemorySegment.ofArray((char[]) arr),
    ValueLayout.JAVA_CHAR),
  REF(
    (left, right) -> left == right,
    new LazyMemoizingSupplier<>(() -> MethodHandles.arrayElementVarHandle(Object[].class)),
    null,
    null);
  // CHECKSTYLE.ON

  private final BiPredicate<Object, Object> valueEqualChecker;

  // array ops
  private final Supplier<VarHandle> arrayElementVarHandleSupplier;
  private final Function<Object, MemorySegment> memSegmentFromArrayFactory;

  // mem/array ops
  private final ValueLayout valueLayout;
  private final ValueLayout unalignedValueLayout;

  /**
   * Constructs a new value type kind instance.
   *
   * @param valueEqualChecker             checker if two values of the type are equal.
   * @param arrayElementVarHandleSupplier a supplier for a var handle to modify elements in arrays of the target type.
   * @param memSegmentFromArrayFactory    a factory to create a memory segment from an array of the target type.
   * @param valueLayout                   the value layout of the target type, null if off-heap access is not possible.
   * @throws NullPointerException if the given value equal checker or array element var handle supplier is null.
   */
  ValueTypeKind(
    @NonNull BiPredicate<Object, Object> valueEqualChecker,
    @NonNull Supplier<VarHandle> arrayElementVarHandleSupplier,
    @Nullable Function<Object, MemorySegment> memSegmentFromArrayFactory,
    @Nullable ValueLayout valueLayout
  ) {
    this.valueEqualChecker = valueEqualChecker;
    this.arrayElementVarHandleSupplier = arrayElementVarHandleSupplier;
    this.memSegmentFromArrayFactory = memSegmentFromArrayFactory;
    this.valueLayout = valueLayout;
    this.unalignedValueLayout = valueLayout == null ? null : valueLayout.withByteAlignment(1);
  }

  /**
   * Gets the value type kind for the given type.
   *
   * @param type the type to get the value type kind for.
   * @return the value type kind for the given type.
   * @throws NullPointerException if the given type is null.
   */
  // IMPL NOTE: very hot path, readability doesn't really matter here
  static @NonNull ValueTypeKind of(@NonNull Class<?> type) {
    if (!type.isPrimitive()) {
      // optimization to prevent the comparisons below
      return REF;
    }

    if (type == byte.class) {
      return BYTE;
    }
    if (type == boolean.class) {
      return BOOL;
    }
    if (type == char.class) {
      return CHAR;
    }
    if (type == short.class) {
      return SHORT;
    }
    if (type == int.class) {
      return INT;
    }
    if (type == long.class) {
      return LONG;
    }
    if (type == float.class) {
      return FLOAT;
    }
    if (type == double.class) {
      return DOUBLE;
    }

    // can only reach here when called with void which should not happen
    throw new AssertionError();
  }

  /**
   * Checks if the given values are the same for this type kind.
   *
   * @param left  the left value.
   * @param right the right value to compare to the left value.
   * @return true if the given values are the same for this type kind, false otherwise.
   */
  public boolean areValuesEqual(@Nullable Object left, @Nullable Object right) {
    return this.valueEqualChecker.test(left, right);
  }

  /**
   * Get the scale for indexes in arrays of this type. Only returns values greater zero.
   *
   * @return the scale for indexes in arrays of this type.
   */
  public int byteSize() {
    var valueLayout = this.valueLayout;
    return valueLayout == null ? 4 : (int) valueLayout.byteSize();
  }

  /**
   * Get a var handle to access memory segments for the wrapped type. The returned var handle can only be used for
   * aligned access, but supports all access operations.
   *
   * @return a var handle to access memory segments for this type.
   * @throws NullPointerException if the operation is not supported for this type kind.
   */
  public @NonNull VarHandle layoutVarHandle() {
    var valueLayout = Objects.requireNonNull(this.valueLayout, "unsupported operation");
    return valueLayout.varHandle();
  }

  /**
   * Get a var handle to access memory segments for the wrapped type. The returned var handle can be used for aligned
   * and unaligned access, but does only support the {@code get} and {@code set} operations.
   *
   * @return a var handle to access memory segments for this type.
   * @throws NullPointerException if the operation is not supported for this type kind.
   */
  public @NonNull VarHandle unalignedLayoutVarHandle() {
    var valueLayout = Objects.requireNonNull(this.unalignedValueLayout, "unsupported operation");
    return valueLayout.varHandle();
  }

  /**
   * Constructs a new memory segment from the given array.
   *
   * @param array the array to create a memory segment for.
   * @return a new memory segment from the given array.
   * @throws NullPointerException if the given array is null or a memory segment cannot be constructed for this kind.
   */
  public @NonNull MemorySegment createArrayMemorySegment(@NonNull Object array) {
    var factory = Objects.requireNonNull(this.memSegmentFromArrayFactory, "unsupported operation");
    return factory.apply(array);
  }

  /**
   * Get a var handle to access elements in arrays of this type kind.
   *
   * @return a var handle to access elements in arrays of this type kind.
   */
  public @NonNull VarHandle arrayElementVarHandle() {
    return this.arrayElementVarHandleSupplier.get();
  }
}
