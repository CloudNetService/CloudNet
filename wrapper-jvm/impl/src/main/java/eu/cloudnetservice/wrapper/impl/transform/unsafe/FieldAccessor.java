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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReferenceArray;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * An accessor for a field in a class.
 *
 * @since 4.0
 */
final class FieldAccessor {

  // indices of the method handles in the handle tables. the access mode is added to the
  // GET and SET indices to get the handle for the access mode (e.g. GET + OpConstants.MODE_VOLATILE)
  private static final int GET = 0;
  private static final int SET = 2;
  private static final int CAS = 5;
  private static final int GET_AND_ADD = 6;
  private static final int GET_AND_SET = 7;
  private static final int HANDLE_COUNT = 8;
  private static final VarHandle.AccessMode[] ACCESS_MODES = {
    VarHandle.AccessMode.GET,
    VarHandle.AccessMode.GET_VOLATILE,
    VarHandle.AccessMode.SET,
    VarHandle.AccessMode.SET_VOLATILE,
    VarHandle.AccessMode.SET_RELEASE,
    VarHandle.AccessMode.COMPARE_AND_SET,
    VarHandle.AccessMode.GET_AND_ADD,
    VarHandle.AccessMode.GET_AND_SET,
  };

  private static final MethodHandle TO_BITS; // (Object)long
  private static final MethodHandle SHIFTED_BITS; // (int, Object)long
  private static final MethodHandle FROM_BITS; // (ValueTypeKind, long)Object
  private static final MethodHandle THROW_UNSUPPORTED; // (String)Object
  private static final MethodHandle IS_INSTANCE_OR_NULL; // (Class, Object)boolean
  private static final MethodHandle EMULATED_PUT; // (Object, MH, int, Object, Object)void
  private static final MethodHandle EMULATED_CAS; // (ValueTypeKind, Object, MH, MH, Object, Object, Object)boolean
  private static final MethodHandle EMULATED_GET_AND_ADD; // (ValueTypeKind, Object, MH, MH, Object, Object)Object
  private static final MethodHandle EMULATED_GET_AND_SET; // (Object, MH, MH, Object, Object)Object
  private static final MethodHandle NARROW_PUT; // (ValueTypeKind, long, int, MH, MH, Object, Object)void
  private static final MethodHandle NARROW_CAS; // (ValueTypeKind, long, int, MH, MH, Object, Object, Object)boolean
  private static final MethodHandle NARROW_GET_AND_ADD; // (ValueTypeKind, ValueTypeKind, long, int, MH, MH, Object, Object)
  private static final MethodHandle NARROW_GET_AND_SET; // (ValueTypeKind, ValueTypeKind, long, int, MH, MH, Object, Object)

  static {
    try {
      var lookup = MethodHandles.lookup();
      TO_BITS = lookup.findStatic(
        ValueTypeKind.class,
        "toBits",
        MethodType.methodType(long.class, Object.class));
      SHIFTED_BITS = lookup.findStatic(
        FieldAccessor.class,
        "shiftedBits",
        MethodType.methodType(long.class, int.class, Object.class));
      FROM_BITS = lookup.findVirtual(
        ValueTypeKind.class,
        "fromBits",
        MethodType.methodType(Object.class, long.class));
      THROW_UNSUPPORTED = lookup.findStatic(
        FieldAccessor.class,
        "throwUnsupported",
        MethodType.methodType(Object.class, String.class));
      IS_INSTANCE_OR_NULL = lookup.findStatic(
        FieldAccessor.class,
        "isInstanceOrNull",
        MethodType.methodType(boolean.class, Class.class, Object.class));
      EMULATED_PUT = lookup.findStatic(
        FieldAccessor.class,
        "emulatedPut",
        MethodType.methodType(void.class, Object.class, MethodHandle.class, int.class, Object.class, Object.class));
      EMULATED_CAS = lookup.findStatic(
        FieldAccessor.class,
        "emulatedCompareAndSet",
        MethodType.methodType(
          boolean.class,
          ValueTypeKind.class, Object.class, MethodHandle.class, MethodHandle.class,
          Object.class, Object.class, Object.class));
      EMULATED_GET_AND_ADD = lookup.findStatic(
        FieldAccessor.class,
        "emulatedGetAndAdd",
        MethodType.methodType(
          Object.class,
          ValueTypeKind.class, Object.class, MethodHandle.class, MethodHandle.class, Object.class, Object.class));
      EMULATED_GET_AND_SET = lookup.findStatic(
        FieldAccessor.class,
        "emulatedGetAndSet",
        MethodType.methodType(
          Object.class,
          Object.class, MethodHandle.class, MethodHandle.class, Object.class, Object.class));
      NARROW_PUT = lookup.findStatic(
        FieldAccessor.class,
        "narrowPut",
        MethodType.methodType(
          void.class,
          ValueTypeKind.class, long.class, int.class, MethodHandle.class, MethodHandle.class, Object.class,
          Object.class));
      NARROW_CAS = lookup.findStatic(
        FieldAccessor.class,
        "narrowCompareAndSet",
        MethodType.methodType(
          boolean.class,
          ValueTypeKind.class, long.class, int.class, MethodHandle.class, MethodHandle.class,
          Object.class, Object.class, Object.class));
      NARROW_GET_AND_ADD = lookup.findStatic(
        FieldAccessor.class,
        "narrowGetAndAdd",
        MethodType.methodType(
          Object.class,
          ValueTypeKind.class, ValueTypeKind.class, long.class, int.class, MethodHandle.class, MethodHandle.class,
          Object.class, Object.class));
      NARROW_GET_AND_SET = lookup.findStatic(
        FieldAccessor.class,
        "narrowGetAndSet",
        MethodType.methodType(
          Object.class,
          ValueTypeKind.class, ValueTypeKind.class, long.class, int.class, MethodHandle.class, MethodHandle.class,
          Object.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private final Field field;
  private final boolean isStatic;
  private final ValueTypeKind kind;
  private final ByteOrder byteOrder;

  // handles for all operations, exactly typed to the field type
  private final MethodHandle[] handles;
  // lazily created handles that convert from/to the field type, index is: requested kind ordinal * HANDLE_COUNT + op
  private final AtomicReferenceArray<MethodHandle> convertedHandles;

  /**
   * Constructs a new field accessor from the given field.
   *
   * @param field     the field to construct the accessor for.
   * @param byteOrder the byte order to use for accesses narrower than the field.
   * @throws NullPointerException         if the given field is null.
   * @throws ReflectiveOperationException if an exception occurs while getting the required reflective values.
   */
  private FieldAccessor(@NonNull Field field, @NonNull ByteOrder byteOrder) throws ReflectiveOperationException {
    this.field = field;
    this.byteOrder = byteOrder;
    this.kind = ValueTypeKind.of(field.getType());
    this.isStatic = Modifier.isStatic(field.getModifiers());
    this.handles = createHandles(field, this.kind, this.isStatic);
    this.convertedHandles = new AtomicReferenceArray<>(ValueTypeKind.values().length * HANDLE_COUNT);
  }

  /**
   * Constructs a new field accessor from the given field.
   *
   * @param field the field to construct the accessor for.
   * @throws NullPointerException  if the given field is null.
   * @throws IllegalStateException if an exception occurs while getting the required reflective values.
   */
  public static @NonNull FieldAccessor make(@NonNull Field field) {
    return make(field, ByteOrder.nativeOrder());
  }

  /**
   * Constructs a new field accessor using the given byte order for accesses narrower than the field.
   *
   * @param field     the field to construct the accessor for.
   * @param byteOrder the byte order to use for accesses narrower than the field.
   * @return the new accessor for the given field.
   * @throws NullPointerException  if the given field or byte order is null.
   * @throws IllegalStateException if an exception occurs while getting the required reflective values.
   */
  @VisibleForTesting
  static @NonNull FieldAccessor make(@NonNull Field field, @NonNull ByteOrder byteOrder) {
    try {
      return new FieldAccessor(field, byteOrder);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /**
   * Creates the method handles for all operations on the given field, exactly typed to the field type.
   *
   * @param field    the field to create the handles for.
   * @param kind     the kind of the field type.
   * @param isStatic if the given field is static.
   * @return the handles for all operations on the given field.
   * @throws NullPointerException         if the given field or kind is null.
   * @throws ReflectiveOperationException if an exception occurs while resolving the field handles.
   */
  private static @NonNull MethodHandle[] createHandles(
    @NonNull Field field,
    @NonNull ValueTypeKind kind,
    boolean isStatic
  ) throws ReflectiveOperationException {
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var varHandle = lookup.unreflectVarHandle(field);
    var type = carrierType(kind);

    var handles = new MethodHandle[HANDLE_COUNT];
    for (var op = 0; op < HANDLE_COUNT; op++) {
      var accessMode = ACCESS_MODES[op];
      if (varHandle.isAccessModeSupported(accessMode)) {
        // static field handles get a leading dummy receiver argument for calling convenience
        var handle = varHandle.toMethodHandle(accessMode);
        handle = isStatic ? MethodHandles.dropArguments(handle, 0, Object.class) : handle;
        handles[op] = handle.asType(handleType(op, type));
      }
    }

    // ensure that the expected value uses the same type as the field
    if (handles[CAS] != null && kind == ValueTypeKind.REF && field.getType() != Object.class) {
      var expectedTypeCheck = MethodHandles.dropArguments(
        MethodHandles.insertArguments(IS_INSTANCE_OR_NULL, 0, field.getType()),
        0,
        Object.class);
      var casFailure = MethodHandles.dropArguments(
        MethodHandles.constant(boolean.class, false),
        0,
        handles[CAS].type().parameterList());
      handles[CAS] = MethodHandles.guardWithTest(expectedTypeCheck, handles[CAS], casFailure);
    }

    // var handles of final fields are read-only, use a setter instead
    if (handles[SET] == null) {
      var lock = new Object();
      var setter = isStatic
        ? MethodHandles.dropArguments(
        lookup.findStaticSetter(field.getDeclaringClass(), field.getName(), field.getType()),
        0,
        Object.class)
        : lookup.findSetter(field.getDeclaringClass(), field.getName(), field.getType());
      setter = setter.asType(handleType(SET, type));
      for (var mode = OpConstants.MODE_PLAIN; mode <= OpConstants.MODE_RELEASE; mode++) {
        handles[SET + mode] = MethodHandles.insertArguments(EMULATED_PUT, 0, lock, setter, mode)
          .asType(handleType(SET, type));
      }

      var getter = handles[GET];
      handles[CAS] = MethodHandles.insertArguments(EMULATED_CAS, 0, kind, lock, getter, setter)
        .asType(handleType(CAS, type));
      handles[GET_AND_ADD] = MethodHandles.insertArguments(EMULATED_GET_AND_ADD, 0, kind, lock, getter, setter)
        .asType(handleType(GET_AND_ADD, type));
      handles[GET_AND_SET] = MethodHandles.insertArguments(EMULATED_GET_AND_SET, 0, lock, getter, setter)
        .asType(handleType(GET_AND_SET, type));
    }

    // operations that are not supported by the field type, e.g. numeric operations on boolean fields
    for (var op = 0; op < HANDLE_COUNT; op++) {
      if (handles[op] == null) {
        var message = "Operation " + ACCESS_MODES[op] + " is not supported by field " + field;
        handles[op] = unsupportedHandle(handleType(op, type), message);
      }
    }

    return handles;
  }

  /**
   * Get the exact type of the handle for the given operation and value type.
   *
   * @param op   the operation to get the handle type for.
   * @param type the type of the value that is accessed.
   * @return the exact type of the handle for the given operation.
   * @throws NullPointerException if the given type is null.
   */
  private static @NonNull MethodType handleType(int op, @NonNull Class<?> type) {
    return switch (op) {
      case GET, GET + OpConstants.MODE_VOLATILE -> MethodType.methodType(type, Object.class);
      case CAS -> MethodType.methodType(boolean.class, Object.class, type, type);
      case GET_AND_ADD, GET_AND_SET -> MethodType.methodType(type, Object.class, type);
      default -> MethodType.methodType(void.class, Object.class, type);
    };
  }

  /**
   * Get the type that is used to carry values of the given kind in method handles.
   *
   * @param kind the kind to get the carrier type of.
   * @return the carrier type of the given kind.
   * @throws NullPointerException if the given kind is null.
   */
  private static @NonNull Class<?> carrierType(@NonNull ValueTypeKind kind) {
    return switch (kind) {
      case BYTE -> byte.class;
      case SHORT -> short.class;
      case INT -> int.class;
      case LONG -> long.class;
      case FLOAT -> float.class;
      case DOUBLE -> double.class;
      case BOOL -> boolean.class;
      case CHAR -> char.class;
      case REF -> Object.class;
    };
  }

  /**
   * Creates a method handle of the given type that throws an UnsupportedOperationException with the given message.
   *
   * @param type    the type of the method handle.
   * @param message the message of the exception to throw.
   * @return a method handle of the given type that throws an exception when invoked.
   * @throws NullPointerException if the given type or message is null.
   */
  private static @NonNull MethodHandle unsupportedHandle(@NonNull MethodType type, @NonNull String message) {
    var thrower = MethodHandles.insertArguments(THROW_UNSUPPORTED, 0, message);
    return MethodHandles.dropArguments(thrower, 0, type.parameterList()).asType(type);
  }

  /**
   * Throws an UnsupportedOperationException with the given message.
   *
   * @param message the message of the exception.
   * @return never returns normally.
   * @throws UnsupportedOperationException always.
   */
  private static Object throwUnsupported(String message) {
    throw new UnsupportedOperationException(message);
  }

  /**
   * Checks if the given value is null or an instance of the given type.
   *
   * @param type  the type to check the value against.
   * @param value the value to check.
   * @return true if the given value is null or an instance of the given type, false otherwise.
   */
  private static boolean isInstanceOrNull(Class<?> type, Object value) {
    return value == null || type.isInstance(value);
  }

  /**
   * Writes a field whose var handle does not support writes (trusted final fields). Shares the lock of the emulated
   * read-modify-write operations to prevent them from overwriting a concurrent write.
   *
   * @param lock   the lock that guards the emulated operations on the field.
   * @param setter the handle to write the field value.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @param base   the object that holds the field, ignored for static fields.
   * @param value  the value to write.
   * @throws Throwable if an exception occurs while invoking the setter.
   */
  private static void emulatedPut(
    Object lock,
    MethodHandle setter,
    int mode,
    Object base,
    Object value
  ) throws Throwable {
    synchronized (lock) {
      setter.invoke(base, value);
    }
    if (mode != OpConstants.MODE_PLAIN) {
      VarHandle.fullFence();
    }
  }

  /**
   * Emulates a compare-and-set operation on a field whose var handle does not support it (trusted final fields). The
   * operation is only atomic in relation to other emulated operations on the same field.
   *
   * @param kind     the kind of the field.
   * @param lock     the lock that guards the emulated operations on the field.
   * @param getter   the handle to read the field value.
   * @param setter   the handle to write the field value.
   * @param base     the object that holds the field, ignored for static fields.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   * @throws Throwable if an exception occurs while invoking the given handles.
   */
  private static boolean emulatedCompareAndSet(
    ValueTypeKind kind,
    Object lock,
    MethodHandle getter,
    MethodHandle setter,
    Object base,
    Object expected,
    Object value
  ) throws Throwable {
    synchronized (lock) {
      var current = getter.invoke(base);
      if (kind.areValuesEqual(current, expected)) {
        setter.invoke(base, value);
        return true;
      }
      return false;
    }
  }

  /**
   * Emulates a get-and-add operation on a field whose var handle does not support it (trusted final fields). The
   * operation is only atomic in relation to other emulated operations on the same field.
   *
   * @param kind   the kind of the field.
   * @param lock   the lock that guards the emulated operations on the field.
   * @param getter the handle to read the field value.
   * @param setter the handle to write the field value.
   * @param base   the object that holds the field, ignored for static fields.
   * @param delta  the delta to add to the current value.
   * @return the value before adding the delta.
   * @throws Throwable if an exception occurs while invoking the given handles.
   */
  private static Object emulatedGetAndAdd(
    ValueTypeKind kind,
    Object lock,
    MethodHandle getter,
    MethodHandle setter,
    Object base,
    Object delta
  ) throws Throwable {
    synchronized (lock) {
      var current = getter.invoke(base);
      setter.invoke(base, kind.add(current, delta));
      return current;
    }
  }

  /**
   * Emulates a get-and-set operation on a field whose var handle does not support it (trusted final fields). The
   * operation is only atomic in relation to other emulated operations on the same field.
   *
   * @param lock   the lock that guards the emulated operations on the field.
   * @param getter the handle to read the field value.
   * @param setter the handle to write the field value.
   * @param base   the object that holds the field, ignored for static fields.
   * @param value  the new value to set.
   * @return the value before setting the new value.
   * @throws Throwable if an exception occurs while invoking the given handles.
   */
  private static Object emulatedGetAndSet(
    Object lock,
    MethodHandle getter,
    MethodHandle setter,
    Object base,
    Object value
  ) throws Throwable {
    synchronized (lock) {
      var current = getter.invoke(base);
      setter.invoke(base, value);
      return current;
    }
  }

  /**
   * Merges the given bits into the selected bytes of the given current field value.
   *
   * @param fieldKind the kind of the field.
   * @param mask      the unshifted mask of the accessed value.
   * @param shift     the bit offset of the accessed bytes within the field value.
   * @param current   the current boxed value of the field.
   * @param bits      the bits to merge into the field value.
   * @return the boxed field value with the given bits merged into it.
   */
  private static Object mergeBits(ValueTypeKind fieldKind, long mask, int shift, Object current, long bits) {
    return fieldKind.fromBits((ValueTypeKind.toBits(current) & ~(mask << shift)) | ((bits & mask) << shift));
  }

  /**
   * Converts the given boxed field value to raw bits and shifts them right with zero extension to align the accessed
   * bytes with the least significant bits.
   *
   * @param shift the bit offset of the accessed bytes within the field value.
   * @param value the boxed field value to convert and shift.
   * @return the raw bits of the field value shifted right by the given bit offset.
   */
  private static long shiftedBits(int shift, @Nullable Object value) {
    return ValueTypeKind.toBits(value) >>> shift;
  }

  /**
   * Writes a value that is narrower than the field into its first bytes in memory order, using a compare-and-set loop.
   *
   * @param fieldKind the kind of the field.
   * @param mask      the unshifted mask of the accessed value.
   * @param shift     the bit offset of the accessed bytes within the field value.
   * @param getter    the handle to read the field value.
   * @param cas       the handle to compare and set the field value.
   * @param base      the object that holds the field, ignored for static fields.
   * @param value     the boxed value to write.
   * @throws Throwable if an exception occurs while invoking the given handles.
   */
  private static void narrowPut(
    ValueTypeKind fieldKind,
    long mask,
    int shift,
    MethodHandle getter,
    MethodHandle cas,
    Object base,
    Object value
  ) throws Throwable {
    var bits = ValueTypeKind.toBits(value);
    Object current;
    do {
      current = getter.invoke(base);
    } while (!(boolean) cas.invoke(base, current, mergeBits(fieldKind, mask, shift, current, bits)));
  }

  /**
   * Compares and sets the first bytes of the field in memory order, using a compare-and-set loop.
   *
   * @param fieldKind the kind of the field.
   * @param mask      the unshifted mask of the accessed value.
   * @param shift     the bit offset of the accessed bytes within the field value.
   * @param getter    the handle to read the field value.
   * @param cas       the handle to compare and set the field value.
   * @param base      the object that holds the field, ignored for static fields.
   * @param expected  the expected current value of the selected bytes.
   * @param value     the new value to set.
   * @return true if the value was set, false otherwise.
   * @throws Throwable if an exception occurs while invoking the given handles.
   */
  private static boolean narrowCompareAndSet(
    ValueTypeKind fieldKind,
    long mask,
    int shift,
    MethodHandle getter,
    MethodHandle cas,
    Object base,
    Object expected,
    Object value
  ) throws Throwable {
    var expectedBits = ValueTypeKind.toBits(expected) & mask;
    var bits = ValueTypeKind.toBits(value);
    for (; ; ) {
      var current = getter.invoke(base);
      if ((shiftedBits(shift, current) & mask) != expectedBits) {
        return false;
      }
      if ((boolean) cas.invoke(base, current, mergeBits(fieldKind, mask, shift, current, bits))) {
        return true;
      }
    }
  }

  /**
   * Adds the given delta to the raw bits of the first bytes in memory order, using a compare-and-set loop. Overflows do
   * not carry into the other bytes of the field.
   *
   * @param fieldKind the kind of the field.
   * @param requested the kind of value that was requested by the caller.
   * @param mask      the unshifted mask of the accessed value.
   * @param shift     the bit offset of the accessed bytes within the field value.
   * @param getter    the handle to read the field value.
   * @param cas       the handle to compare and set the field value.
   * @param base      the object that holds the field, ignored for static fields.
   * @param delta     the delta to add to the selected bytes.
   * @return the value of the selected bytes before adding the delta.
   * @throws Throwable if an exception occurs while invoking the given handles.
   */
  private static Object narrowGetAndAdd(
    ValueTypeKind fieldKind,
    ValueTypeKind requested,
    long mask,
    int shift,
    MethodHandle getter,
    MethodHandle cas,
    Object base,
    Object delta
  ) throws Throwable {
    var deltaBits = ValueTypeKind.toBits(delta);
    for (; ; ) {
      var current = getter.invoke(base);
      var currentBits = shiftedBits(shift, current);
      if ((boolean) cas.invoke(base, current, mergeBits(fieldKind, mask, shift, current, currentBits + deltaBits))) {
        return requested.fromBits(currentBits);
      }
    }
  }

  /**
   * Sets the first bytes of the field in memory order to the given value, using a compare-and-set loop.
   *
   * @param fieldKind the kind of the field.
   * @param requested the kind of value that was requested by the caller.
   * @param mask      the unshifted mask of the accessed value.
   * @param shift     the bit offset of the accessed bytes within the field value.
   * @param getter    the handle to read the field value.
   * @param cas       the handle to compare and set the field value.
   * @param base      the object that holds the field, ignored for static fields.
   * @param value     the new value to set.
   * @return the value of the selected bytes before setting the new value.
   * @throws Throwable if an exception occurs while invoking the given handles.
   */
  private static Object narrowGetAndSet(
    ValueTypeKind fieldKind,
    ValueTypeKind requested,
    long mask,
    int shift,
    MethodHandle getter,
    MethodHandle cas,
    Object base,
    Object value
  ) throws Throwable {
    var bits = ValueTypeKind.toBits(value);
    for (; ; ) {
      var current = getter.invoke(base);
      if ((boolean) cas.invoke(base, current, mergeBits(fieldKind, mask, shift, current, bits))) {
        return requested.fromBits(shiftedBits(shift, current));
      }
    }
  }

  /**
   * Throws the given throwable without requiring the caller to declare it.
   *
   * @param <T>       the type of throwable to throw.
   * @param throwable the throwable to throw.
   * @return never returns normally.
   */
  @SuppressWarnings("unchecked")
  private static <T extends Throwable> RuntimeException sneakyThrow(Throwable throwable) throws T {
    throw (T) throwable;
  }

  /**
   * Get the handle for the given operation that accepts and returns values of the given kind.
   *
   * @param op        the operation to get the handle for.
   * @param requested the requested kind of the values.
   * @return the handle for the given operation and value kind.
   */
  private MethodHandle handle(int op, ValueTypeKind requested) {
    return requested == this.kind ? this.handles[op] : this.convertedHandle(op, requested);
  }

  /**
   * Get or creates the handle for the given operation that converts values from/to the given kind.
   *
   * @param op        the operation to get the handle for.
   * @param requested the requested kind of the values.
   * @return the handle for the given operation that converts values from/to the given kind.
   * @throws NullPointerException if the given requested kind is null.
   */
  private @NonNull MethodHandle convertedHandle(int op, @NonNull ValueTypeKind requested) {
    var index = requested.ordinal() * HANDLE_COUNT + op;
    var handle = this.convertedHandles.get(index);
    if (handle == null) {
      handle = this.createConvertedHandle(op, requested);
      this.convertedHandles.set(index, handle);
    }

    return handle;
  }

  /**
   * Creates a handle for the given operation that converts the raw bits of the values from/to the given kind.
   *
   * @param op        the operation to create the handle for.
   * @param requested the requested kind of the values.
   * @return a handle for the given operation that converts values from/to the given kind.
   * @throws NullPointerException if the given requested kind is null.
   */
  private @NonNull MethodHandle createConvertedHandle(int op, @NonNull ValueTypeKind requested) {
    var targetType = handleType(op, carrierType(requested));
    if (requested == ValueTypeKind.REF || this.kind == ValueTypeKind.REF) {
      var message = "Cannot access field " + this.field + " as " + requested;
      return unsupportedHandle(targetType, message);
    }

    // Access the first bytes in native memory order: low bits on little-endian, high bits on big-endian.
    var narrow = requested.byteSize() < this.kind.byteSize();
    var shift = narrow && this.byteOrder == ByteOrder.BIG_ENDIAN
      ? (this.kind.byteSize() - requested.byteSize()) * Byte.SIZE
      : 0;
    var narrowWrite = op >= SET && narrow;
    var rawBitsAdd = op == GET_AND_ADD && (this.kind == ValueTypeKind.FLOAT || this.kind == ValueTypeKind.DOUBLE);
    if (narrowWrite || rawBitsAdd) {
      var mask = narrowWrite ? requested.bitMask() : this.kind.bitMask();
      var getter = this.handles[GET + OpConstants.MODE_VOLATILE];
      var cas = this.handles[CAS];
      var handle = switch (op) {
        case CAS -> MethodHandles.insertArguments(
          NARROW_CAS,
          0,
          this.kind, mask, shift, getter, cas);
        case GET_AND_ADD -> MethodHandles.insertArguments(
          NARROW_GET_AND_ADD,
          0,
          this.kind, requested, mask, shift, getter, cas);
        case GET_AND_SET -> MethodHandles.insertArguments(
          NARROW_GET_AND_SET,
          0,
          this.kind, requested, mask, shift, getter, cas);
        default -> MethodHandles.insertArguments(
          NARROW_PUT,
          0,
          this.kind, mask, shift, getter, cas);
      };
      return handle.asType(targetType);
    }

    // (Object)Object handles that convert the raw bits of a value into the field kind or the requested kind
    var toFieldKind = MethodHandles.filterReturnValue(TO_BITS, FROM_BITS.bindTo(this.kind));
    var readBits = shift == 0 ? TO_BITS : MethodHandles.insertArguments(SHIFTED_BITS, 0, shift);
    var toRequestedKind = MethodHandles.filterReturnValue(readBits, FROM_BITS.bindTo(requested));

    var handle = this.handles[op];
    handle = handle.asType(handle.type().generic());
    handle = switch (op) {
      case GET, GET + OpConstants.MODE_VOLATILE -> MethodHandles.filterReturnValue(handle, toRequestedKind);
      case CAS -> MethodHandles.filterArguments(handle, 1, toFieldKind, toFieldKind);
      case GET_AND_ADD, GET_AND_SET -> MethodHandles.filterReturnValue(
        MethodHandles.filterArguments(handle, 1, toFieldKind),
        toRequestedKind);
      default -> MethodHandles.filterArguments(handle, 1, toFieldKind);
    };
    return handle.asType(targetType);
  }

  /**
   * Get if the wrapped field is static.
   *
   * @return true if the wrapped field is static, false otherwise.
   */
  public boolean isStatic() {
    return this.isStatic;
  }

  /**
   * Get the field that was used to create this accessor. Only intended for use in tests.
   *
   * @return the field that was used to create this accessor.
   */
  @VisibleForTesting
  public @NonNull Field wrappedField() {
    return this.field;
  }

  /**
   * Reads the value of the wrapped field in the given base as a {@code byte}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public byte getByte(Object base, int mode) {
    try {
      return (byte) this.handle(GET + mode, ValueTypeKind.BYTE).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as a {@code short}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public short getShort(Object base, int mode) {
    try {
      return (short) this.handle(GET + mode, ValueTypeKind.SHORT).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as a {@code char}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public char getChar(Object base, int mode) {
    try {
      return (char) this.handle(GET + mode, ValueTypeKind.CHAR).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as an {@code int}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public int getInt(Object base, int mode) {
    try {
      return (int) this.handle(GET + mode, ValueTypeKind.INT).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as a {@code long}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public long getLong(Object base, int mode) {
    try {
      return (long) this.handle(GET + mode, ValueTypeKind.LONG).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as a {@code float}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public float getFloat(Object base, int mode) {
    try {
      return (float) this.handle(GET + mode, ValueTypeKind.FLOAT).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as a {@code double}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public double getDouble(Object base, int mode) {
    try {
      return (double) this.handle(GET + mode, ValueTypeKind.DOUBLE).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as a {@code boolean}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public boolean getBoolean(Object base, int mode) {
    try {
      return (boolean) this.handle(GET + mode, ValueTypeKind.BOOL).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Reads the value of the wrapped field in the given base as an {@code Object}.
   *
   * @param base the object that holds the field, ignored for static fields.
   * @param mode the access mode to use, either plain or volatile.
   * @return the value of the wrapped field.
   */
  public Object getObject(Object base, int mode) {
    try {
      return (Object) this.handle(GET + mode, ValueTypeKind.REF).invokeExact(base);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code byte} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putByte(Object base, byte value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.BYTE).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code short} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putShort(Object base, short value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.SHORT).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code char} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putChar(Object base, char value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.CHAR).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code int} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putInt(Object base, int value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.INT).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code long} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putLong(Object base, long value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.LONG).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code float} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putFloat(Object base, float value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.FLOAT).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code double} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putDouble(Object base, double value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.DOUBLE).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code boolean} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putBoolean(Object base, boolean value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.BOOL).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Writes the given {@code Object} into the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the value to write.
   * @param mode  the access mode to use, either plain, volatile or release.
   */
  public void putObject(Object base, Object value, int mode) {
    try {
      this.handle(SET + mode, ValueTypeKind.REF).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically sets the wrapped field of the given base to the given {@code int} if it currently holds the expected
   * value.
   *
   * @param base     the object that holds the field, ignored for static fields.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   */
  public boolean casInt(Object base, int expected, int value) {
    try {
      return (boolean) this.handle(CAS, ValueTypeKind.INT).invokeExact(base, expected, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically sets the wrapped field of the given base to the given {@code long} if it currently holds the expected
   * value.
   *
   * @param base     the object that holds the field, ignored for static fields.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   */
  public boolean casLong(Object base, long expected, long value) {
    try {
      return (boolean) this.handle(CAS, ValueTypeKind.LONG).invokeExact(base, expected, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically sets the wrapped field of the given base to the given {@code Object} if it currently holds the expected
   * value.
   *
   * @param base     the object that holds the field, ignored for static fields.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   */
  public boolean casObject(Object base, Object expected, Object value) {
    try {
      return (boolean) this.handle(CAS, ValueTypeKind.REF).invokeExact(base, expected, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically adds the given {@code int} delta to the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param delta the delta to add to the current value.
   * @return the value before adding the delta.
   */
  public int getAndAddInt(Object base, int delta) {
    try {
      return (int) this.handle(GET_AND_ADD, ValueTypeKind.INT).invokeExact(base, delta);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically adds the given {@code long} delta to the wrapped field of the given base.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param delta the delta to add to the current value.
   * @return the value before adding the delta.
   */
  public long getAndAddLong(Object base, long delta) {
    try {
      return (long) this.handle(GET_AND_ADD, ValueTypeKind.LONG).invokeExact(base, delta);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically sets the wrapped field of the given base to the given {@code int}.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the new value to set.
   * @return the value before setting the new value.
   */
  public int getAndSetInt(Object base, int value) {
    try {
      return (int) this.handle(GET_AND_SET, ValueTypeKind.INT).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically sets the wrapped field of the given base to the given {@code long}.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the new value to set.
   * @return the value before setting the new value.
   */
  public long getAndSetLong(Object base, long value) {
    try {
      return (long) this.handle(GET_AND_SET, ValueTypeKind.LONG).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }

  /**
   * Atomically sets the wrapped field of the given base to the given {@code Object}.
   *
   * @param base  the object that holds the field, ignored for static fields.
   * @param value the new value to set.
   * @return the value before setting the new value.
   */
  public Object getAndSetObject(Object base, Object value) {
    try {
      return (Object) this.handle(GET_AND_SET, ValueTypeKind.REF).invokeExact(base, value);
    } catch (Throwable throwable) {
      throw sneakyThrow(throwable);
    }
  }
}
