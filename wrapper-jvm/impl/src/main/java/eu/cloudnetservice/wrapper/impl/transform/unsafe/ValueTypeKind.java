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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Kinds of values that can be accessed using unsafe. Differentiates between all primitive types and reference types.
 *
 * @since 4.0
 */
enum ValueTypeKind {

  // CHECKSTYLE.OFF: checkstyle isn't really helping here
  BYTE(arr -> MemorySegment.ofArray((byte[]) arr), ValueLayout.JAVA_BYTE),
  SHORT(arr -> MemorySegment.ofArray((short[]) arr), ValueLayout.JAVA_SHORT),
  INT(arr -> MemorySegment.ofArray((int[]) arr), ValueLayout.JAVA_INT),
  LONG(arr -> MemorySegment.ofArray((long[]) arr), ValueLayout.JAVA_LONG),
  FLOAT(arr -> MemorySegment.ofArray((float[]) arr), ValueLayout.JAVA_FLOAT),
  DOUBLE(arr -> MemorySegment.ofArray((double[]) arr), ValueLayout.JAVA_DOUBLE),
  BOOL(
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
  CHAR(arr -> MemorySegment.ofArray((char[]) arr), ValueLayout.JAVA_CHAR),
  REF(null, null);
  // CHECKSTYLE.ON

  private final ValueLayout valueLayout;
  private final ValueLayout unalignedValueLayout;
  private final Function<Object, MemorySegment> memSegmentFromArrayFactory;

  /**
   * Constructs a new value type kind instance.
   *
   * @param memSegmentFromArrayFactory a factory to create a memory segment from an array of the target type.
   * @param valueLayout                the value layout of the target type, null if off-heap access is not possible.
   */
  ValueTypeKind(
    @Nullable Function<Object, MemorySegment> memSegmentFromArrayFactory,
    @Nullable ValueLayout valueLayout
  ) {
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
   * Converts the given boxed primitive value into its raw bits. Null or non-primitive values are converted to 0.
   *
   * @param value the boxed value to get the raw bits of.
   * @return the raw bits of the given value.
   */
  public static long toBits(@Nullable Object value) {
    return switch (value) {
      case Byte b -> b;
      case Short s -> s;
      case Character c -> c;
      case Integer i -> i;
      case Long l -> l;
      case Float f -> Float.floatToRawIntBits(f);
      case Double d -> Double.doubleToRawLongBits(d);
      case Boolean b -> b ? 1 : 0;
      case null, default -> 0;
    };
  }

  /**
   * Converts the given raw bits into a boxed value of this kind, truncating the bits if this kind is narrower than 8
   * bytes.
   *
   * @param bits the raw bits to convert.
   * @return a boxed value of this kind which represents the given bits.
   * @throws UnsupportedOperationException if this kind is the reference kind.
   */
  public @NonNull Object fromBits(long bits) {
    return switch (this) {
      case BYTE -> (byte) bits;
      case SHORT -> (short) bits;
      case CHAR -> (char) bits;
      case INT -> (int) bits;
      case LONG -> bits;
      case FLOAT -> Float.intBitsToFloat((int) bits);
      case DOUBLE -> Double.longBitsToDouble(bits);
      case BOOL -> (byte) bits != 0; // same as Unsafe.byte2bool
      case REF -> throw new UnsupportedOperationException("cannot convert raw bits to a reference");
    };
  }

  /**
   * Checks if the given values are the same for this type kind. Primitive values are compared by their raw bits,
   * references by identity (which matches the semantics of a compare-and-set operation).
   *
   * @param left  the left value.
   * @param right the right value to compare to the left value.
   * @return true if the given values are the same for this type kind, false otherwise.
   */
  public boolean areValuesEqual(@Nullable Object left, @Nullable Object right) {
    if (this == REF) {
      return left == right;
    }

    var mask = this.bitMask();
    return (toBits(left) & mask) == (toBits(right) & mask);
  }

  /**
   * Get the mask that selects the lowest bytes of raw bits that are occupied by a value of this kind.
   *
   * @return the mask that selects the bits of a value of this kind.
   */
  public long bitMask() {
    var byteSize = this.byteSize();
    return byteSize == 8 ? -1L : (1L << (byteSize * 8)) - 1;
  }

  /**
   * Adds the given boxed values of this kind, returning the boxed result.
   *
   * @param left  the left value to add.
   * @param right the right value to add.
   * @return the boxed result of the addition.
   * @throws UnsupportedOperationException if the type kind does not support addition.
   */
  public @NonNull Object add(@Nullable Object left, @Nullable Object right) {
    return switch (this) {
      case BYTE, SHORT, CHAR, INT, LONG -> this.fromBits(toBits(left) + toBits(right));
      case FLOAT -> (float) this.fromBits(toBits(left)) + (float) FLOAT.fromBits(toBits(right));
      case DOUBLE -> (double) this.fromBits(toBits(left)) + (double) DOUBLE.fromBits(toBits(right));
      case BOOL, REF -> throw new UnsupportedOperationException("cannot add values of kind " + this);
    };
  }

  /**
   * Get the byte size of this type kind.
   *
   * @return the byte size of this type kind.
   */
  public int byteSize() {
    var valueLayout = this.valueLayout;
    return valueLayout == null ? 4 : (int) valueLayout.byteSize();
  }

  /**
   * Get the var handle that can be used to access values of this kind that are aligned in memory.
   *
   * @return the var handle that can be used to access values of this kind that are aligned in memory.
   * @throws NullPointerException if this is the reference kind.
   */
  public @NonNull VarHandle layoutVarHandle() {
    var valueLayout = Objects.requireNonNull(this.valueLayout, "unsupported operation");
    return valueLayout.varHandle();
  }

  /**
   * Get the var handle that can be used to access values of this kind that are not aligned in memory.
   *
   * @return the var handle that can be used to access values of this kind that are not aligned in memory.
   * @throws NullPointerException if this is the reference kind.
   */
  public @NonNull VarHandle unalignedLayoutVarHandle() {
    var valueLayout = Objects.requireNonNull(this.unalignedValueLayout, "unsupported operation");
    return valueLayout.varHandle();
  }

  /**
   * Creates a memory segment for the given array. The given array must be of the same type as this kind.
   *
   * @param array the array to wrap into a memory segment.
   * @return a memory segment for the given array.
   * @throws NullPointerException if the given array is null or this is the reference kind.
   * @throws ClassCastException   if the given array is not of the type of this kind.
   */
  public @NonNull MemorySegment createArrayMemorySegment(@NonNull Object array) {
    var factory = Objects.requireNonNull(this.memSegmentFromArrayFactory, "unsupported operation");
    return factory.apply(array);
  }
}
