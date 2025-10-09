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

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Delegate class that holds current replacements for fields/methods defined in {@code sun.misc.Unsafe}.
 * <p>
 * <b>DO NOT USE THIS CLASS!</b> This class is intended for old, outdated libs to continue functioning. All deprecated
 * methods in {@code sun.misc.Unsafe} have a safe replacement within the jdk, use those instead!
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class UnsafeReplacementDelegate {

  // accessor for cleaning up direct byte buffers
  @VisibleForTesting
  static final Consumer<ByteBuffer> BB_CLEANER_NOOP = _ -> {
  };
  @VisibleForTesting
  static final Supplier<Consumer<ByteBuffer>> BB_CLEANER = createByteBufferCleaner();

  // accessor for the operating system mx bean
  private static final Supplier<OperatingSystemMXBean> OS_MX_BEAN =
    StableValue.supplier(ManagementFactory::getOperatingSystemMXBean);

  // method handle to define a class in any class loader, takes the target class loader as the first argument
  // method descriptor: ClassLoader.defineClass1(ClassLoader, String, byte[], int, int, ProtectionDomain, String)
  private static final Supplier<MethodHandle> CLASS_DEFINE_METHOD_HANDLE = createClassDefineMethodHandleSupplier();

  // the size of addresses on the machine, either 4 or 8
  private static final byte ADDRESS_SIZE = (byte) ValueLayout.ADDRESS.byteSize();

  private UnsafeReplacementDelegate() {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a supplier that resolves the
   * {@code ClassLoader.defineClass1(ClassLoader, String, byte[], int, int, ProtectionDomain, String)} method handle
   * once when initialized.
   *
   * @return a supplier for a method handle to invoke {@code ClassLoader.defineClass}.
   */
  private static @NonNull Supplier<MethodHandle> createClassDefineMethodHandleSupplier() {
    return StableValue.supplier(() -> {
      try {
        // resolves the method handle for: Class<?> defineClass1(ClassLoader, String, byte[], int, int, ProtectionDomain, String)
        var lookup = OpConstants.TRUSTED_LOOKUP.get();
        var defineClassMt = MethodType.methodType(
          Class.class,
          ClassLoader.class,
          String.class,
          byte[].class,
          int.class,
          int.class,
          ProtectionDomain.class,
          String.class);
        return lookup.findStatic(ClassLoader.class, "defineClass1", defineClassMt);
      } catch (NoSuchMethodException | IllegalAccessException exception) {
        throw new ExceptionInInitializerError(exception); // cannot happen
      }
    });
  }

  /**
   * Get a supplier that creates a consumer to clean a direct byte buffer. The consumer is only created once on the
   * first initialization of the supplier.
   *
   * @return a supplier that creates a consumer to clean a direct byte buffer.
   */
  private static @NonNull Supplier<Consumer<ByteBuffer>> createByteBufferCleaner() {
    return StableValue.supplier(() -> {
      try {
        var lookup = OpConstants.TRUSTED_LOOKUP.get();

        // get the method handle to get the cleaner of the provided byte buffer (type: (ByteBuffer):Cleaner)
        var directBufferClass = Class.forName("sun.nio.ch.DirectBuffer");
        var cleanerMethod = directBufferClass.getDeclaredMethod("cleaner");
        var cleanerHandle = MethodHandles.explicitCastArguments(
          lookup.unreflect(cleanerMethod),
          MethodType.methodType(cleanerMethod.getReturnType(), ByteBuffer.class));

        // get the method handle to invoke the clean method on the Cleaner class (type: (Cleaner):void)
        var cleanerClass = cleanerMethod.getReturnType();
        var cleanMethod = cleanerClass.getDeclaredMethod("clean");
        var cleanHandle = lookup.unreflect(cleanMethod);

        // adapt the clean() method handle by pre-processing it with the result of the cleaner retrieval
        // method handle, this results in a chained invocation (type: (ByteBuffer):void)
        var cleanBufferHandle = MethodHandles.filterReturnValue(cleanerHandle, cleanHandle);

        // get a method handle to check if a buffer has an attachment (type: (ByteBuffer):boolean)
        var attachementMethod = directBufferClass.getMethod("attachment");
        var attachmentHandle = MethodHandles.explicitCastArguments(
          lookup.unreflect(attachementMethod),
          MethodType.methodType(attachementMethod.getReturnType(), ByteBuffer.class));
        var isNullHandle = lookup.findStatic(
          Objects.class,
          "isNull",
          MethodType.methodType(boolean.class, Object.class));
        var isAttachmentNullHandle = MethodHandles.collectArguments(isNullHandle, 0, attachmentHandle);

        // get a method handle that throws an IAE with the message 'duplicate or slice' (type: ():void)
        // this handle needs to then be adapted to add an extra, ignored ByteBuffer param (type: (ByteBuffer):void)
        var iaeMessageCtrHandle = lookup.findConstructor(
          IllegalArgumentException.class,
          MethodType.methodType(void.class, String.class));
        var dupOrSliceIaeCtrHandle = MethodHandles.insertArguments(iaeMessageCtrHandle, 0, "duplicate or slice");
        var throwIaeHandle = MethodHandles.throwException(void.class, IllegalArgumentException.class);
        var throwDupOrSliceHandle = MethodHandles.collectArguments(throwIaeHandle, 0, dupOrSliceIaeCtrHandle);
        var throwDupOrSliceHandleWithBBArg = MethodHandles.dropArguments(throwDupOrSliceHandle, 0, ByteBuffer.class);

        // construct a method handle that conditionally invokes 'bb.cleaner().clean()' or throws an
        // IAE depending on the fact if the provided ByteBuffer has an attachment or not
        var cleanIfNotAttachmentHandle = MethodHandles.guardWithTest(
          isAttachmentNullHandle,
          cleanBufferHandle,
          throwDupOrSliceHandleWithBBArg);
        return buffer -> {
          try {
            cleanIfNotAttachmentHandle.invokeExact(buffer);
          } catch (IllegalArgumentException exception) {
            throw exception;
          } catch (Throwable _) {
          }
        };
      } catch (Throwable throwable) {
        UnsafeLogUtil.debug("Unable to access byte buffer cleaning methods; falling back to no cleaning", throwable);
        return BB_CLEANER_NOOP; // unable to clean direct buffers
      }
    });
  }

  //<editor-fold defaultstate="collapsed" desc="Static Replacement Fields">
  // @formatter:off
  /* replacement for ADDRESS_SIZE */
  @UnsafeReplacement(name = "ADDRESS_SIZE")
  public static final int UNSAFE_ADDRESS_SIZE = unsafeAddressSize();

  /* replacement for INVALID_FIELD_OFFSET */
  @UnsafeReplacement(name = "INVALID_FIELD_OFFSET")
  public static final int UNSAFE_INVALID_FIELD_OFFSET = -1;

  /* replacement for ARRAY_BOOLEAN_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_BOOLEAN_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_BOOLEAN_BASE_OFFSET = 0;
  /* replacement for ARRAY_BYTE_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_BYTE_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_BYTE_BASE_OFFSET = 0;
  /* replacement for ARRAY_SHORT_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_SHORT_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_SHORT_BASE_OFFSET = 0;
  /* replacement for ARRAY_CHAR_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_CHAR_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_CHAR_BASE_OFFSET = 0;
  /* replacement for ARRAY_INT_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_INT_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_INT_BASE_OFFSET = 0;
  /* replacement for ARRAY_LONG_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_LONG_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_LONG_BASE_OFFSET = 0;
  /* replacement for ARRAY_FLOAT_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_FLOAT_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_FLOAT_BASE_OFFSET = 0;
  /* replacement for ARRAY_DOUBLE_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_DOUBLE_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_DOUBLE_BASE_OFFSET = 0;
  /* replacement for ARRAY_OBJECT_BASE_OFFSET */
  @UnsafeReplacement(name = "ARRAY_OBJECT_BASE_OFFSET")
  public static final int UNSAFE_ARRAY_OBJECT_BASE_OFFSET = 0;

  /* replacement for ARRAY_BOOLEAN_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_BOOLEAN_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_BOOLEAN_INDEX_SCALE = unsafeArrayIndexScale(boolean[].class);
  /* replacement for ARRAY_BYTE_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_BYTE_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_BYTE_INDEX_SCALE = unsafeArrayIndexScale(byte[].class);
  /* replacement for ARRAY_SHORT_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_SHORT_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_SHORT_INDEX_SCALE = unsafeArrayIndexScale(short[].class);
  /* replacement for ARRAY_CHAR_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_CHAR_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_CHAR_INDEX_SCALE = unsafeArrayIndexScale(char[].class);
  /* replacement for ARRAY_INT_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_INT_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_INT_INDEX_SCALE = unsafeArrayIndexScale(int[].class);
  /* replacement for ARRAY_LONG_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_LONG_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_LONG_INDEX_SCALE = unsafeArrayIndexScale(long[].class);
  /* replacement for ARRAY_FLOAT_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_FLOAT_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_FLOAT_INDEX_SCALE = unsafeArrayIndexScale(float[].class);
  /* replacement for ARRAY_DOUBLE_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_DOUBLE_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_DOUBLE_INDEX_SCALE = unsafeArrayIndexScale(double[].class);
  /* replacement for ARRAY_OBJECT_INDEX_SCALE */
  @UnsafeReplacement(name = "ARRAY_OBJECT_INDEX_SCALE")
  public static final int UNSAFE_ARRAY_OBJECT_INDEX_SCALE = unsafeArrayIndexScale(Object[].class);
  // @formatter:on
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Field/Array Offset Getters">
  /* shared code to validate a field offset can be retrieved */
  public static void validateSaneFieldAccess(Field f) {
    Objects.requireNonNull(f);

    var declaringClass = f.getDeclaringClass();
    if (declaringClass.isHidden()) {
      throw new UnsupportedOperationException("can't get field offset on a hidden class: " + f);
    }

    if (declaringClass.isRecord()) {
      throw new UnsupportedOperationException("can't get field offset on a record class: " + f);
    }
  }

  /* replacement for arrayBaseOffset(Class<?>) */
  @UnsafeReplacement(name = "arrayBaseOffset")
  public static int unsafeArrayBaseOffset(Class<?> ignored) {
    return 0; // array ops don't need any offset into the fields anymore
  }

  /* replacement for arrayIndexScale(Class<?>) */
  @UnsafeReplacement(name = "arrayIndexScale")
  public static int unsafeArrayIndexScale(Class<?> arrayType) {
    Objects.requireNonNull(arrayType);
    if (!arrayType.isArray()) {
      throw new IllegalArgumentException(arrayType.getName());
    }

    var componentType = arrayType.getComponentType();
    var typeKind = ValueTypeKind.of(componentType);
    return typeKind.byteSize();
  }

  /* replacement for objectFieldOffset(Field) */
  @UnsafeReplacement(name = "objectFieldOffset")
  public static long unsafeObjectFieldOffset(Field f) {
    validateSaneFieldAccess(f);
    return FieldOffsetOps.fieldOffset(f);
  }

  /* replacement for staticFieldOffset(Field) */
  @UnsafeReplacement(name = "staticFieldOffset")
  public static long unsafeStaticFieldOffset(Field f) {
    return unsafeObjectFieldOffset(f); // new impl just does the same thing for static/instance fields
  }

  /* replacement for staticFieldBase(Field) */
  @UnsafeReplacement(name = "staticFieldBase")
  public static Object unsafeStaticFieldBase(Field f) {
    validateSaneFieldAccess(f);
    return FieldOffsetOps.staticFieldBase(f);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Field/Array Getters">
  /* shared code to read the value from a field or array */
  private static @Nullable Object unsafeGet(
    @NonNull Object object,
    long offset,
    @NonNull ValueTypeKind kind,
    @NonNull OpConstants.GetOp op
  ) {
    try {
      var type = object.getClass();
      if (type.isArray()) {
        return ArrayOps.arrayGet(kind, op, object, offset);
      }

      // requested read of static or instance field
      var fieldAccessor = FieldOffsetOps.fieldFromOffset(object, offset);
      return fieldAccessor == null ? null : fieldAccessor.get(object, op);
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug("Unable to unsafe get: [obj={}, offset={}, op={}]", object, offset, op, throwable);
      return null;
    }
  }

  /* replacement for getInt(Object, long) */
  @UnsafeReplacement(name = "getInt")
  public static int unsafeGetInt(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.INT, OpConstants.GetOp.DEFAULT);
        yield val instanceof Number number ? number.intValue() : 0;
      }
      case null -> unsafeGetInt(offset);
    };
  }

  /* replacement for getIntVolatile(Object, long) */
  @UnsafeReplacement(name = "getIntVolatile")
  public static int unsafeGetIntVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.INT, OpConstants.GetOp.VOLATILE);
        yield val instanceof Number number ? number.intValue() : 0;
      }
      case null -> (int) MemoryOps.memGet(ValueTypeKind.INT, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getByte(Object, long) */
  @UnsafeReplacement(name = "getByte")
  public static byte unsafeGetByte(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.BYTE, OpConstants.GetOp.DEFAULT);
        yield val instanceof Number number ? number.byteValue() : 0;
      }
      case null -> unsafeGetByte(offset);
    };
  }

  /* replacement for getByte(Object, long) */
  @UnsafeReplacement(name = "getByteVolatile")
  public static byte unsafeGetByteVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.BYTE, OpConstants.GetOp.VOLATILE);
        yield val instanceof Number number ? number.byteValue() : 0;
      }
      case null -> (byte) MemoryOps.memGet(ValueTypeKind.BYTE, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getShort(Object, long) */
  @UnsafeReplacement(name = "getShort")
  public static short unsafeGetShort(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.SHORT, OpConstants.GetOp.DEFAULT);
        yield val instanceof Number number ? number.shortValue() : 0;
      }
      case null -> unsafeGetShort(offset);
    };
  }

  /* replacement for getShortVolatile(Object, long) */
  @UnsafeReplacement(name = "getShortVolatile")
  public static short unsafeGetShortVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.SHORT, OpConstants.GetOp.VOLATILE);
        yield val instanceof Number number ? number.shortValue() : 0;
      }
      case null -> (short) MemoryOps.memGet(ValueTypeKind.SHORT, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getLong(Object, long) */
  @UnsafeReplacement(name = "getLong")
  public static long unsafeGetLong(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.LONG, OpConstants.GetOp.DEFAULT);
        yield val instanceof Number number ? number.longValue() : 0;
      }
      case null -> unsafeGetLong(offset);
    };
  }

  /* replacement for getLongVolatile(Object, long) */
  @UnsafeReplacement(name = "getLongVolatile")
  public static long unsafeGetLongVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.LONG, OpConstants.GetOp.VOLATILE);
        yield val instanceof Number number ? number.longValue() : 0;
      }
      case null -> (long) MemoryOps.memGet(ValueTypeKind.LONG, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getFloat(Object, long) */
  @UnsafeReplacement(name = "getFloat")
  public static float unsafeGetFloat(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.FLOAT, OpConstants.GetOp.DEFAULT);
        yield val instanceof Number number ? number.floatValue() : 0;
      }
      case null -> unsafeGetFloat(offset);
    };
  }

  /* replacement for getFloatVolatile(Object, long) */
  @UnsafeReplacement(name = "getFloatVolatile")
  public static float unsafeGetFloatVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.FLOAT, OpConstants.GetOp.VOLATILE);
        yield val instanceof Number number ? number.floatValue() : 0;
      }
      case null -> (float) MemoryOps.memGet(ValueTypeKind.FLOAT, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getDouble(Object, long) */
  @UnsafeReplacement(name = "getDouble")
  public static double unsafeGetDouble(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.DOUBLE, OpConstants.GetOp.DEFAULT);
        yield val instanceof Number number ? number.doubleValue() : 0;
      }
      case null -> unsafeGetDouble(offset);
    };
  }

  /* replacement for getDoubleVolatile(Object, long) */
  @UnsafeReplacement(name = "getDoubleVolatile")
  public static double unsafeGetDoubleVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.DOUBLE, OpConstants.GetOp.VOLATILE);
        yield val instanceof Number number ? number.doubleValue() : 0;
      }
      case null -> (double) MemoryOps.memGet(ValueTypeKind.DOUBLE, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getBoolean(Object, long) */
  @UnsafeReplacement(name = "getBoolean")
  public static boolean unsafeGetBoolean(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.BOOL, OpConstants.GetOp.DEFAULT);
        yield val instanceof Boolean bool && bool;
      }
      case null -> (boolean) MemoryOps.memGet(ValueTypeKind.BOOL, OpConstants.GetOp.DEFAULT, offset);
    };
  }

  /* replacement for getBooleanVolatile(Object, long) */
  @UnsafeReplacement(name = "getBooleanVolatile")
  public static boolean unsafeGetBooleanVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.BOOL, OpConstants.GetOp.VOLATILE);
        yield val instanceof Boolean bool && bool;
      }
      case null -> (boolean) MemoryOps.memGet(ValueTypeKind.BOOL, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getChar(Object, long) */
  @UnsafeReplacement(name = "getChar")
  public static char unsafeGetChar(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.CHAR, OpConstants.GetOp.DEFAULT);
        yield val instanceof Character character ? character : 0;
      }
      case null -> unsafeGetChar(offset);
    };
  }

  /* replacement for getCharVolatile(Object, long) */
  @UnsafeReplacement(name = "getCharVolatile")
  public static char unsafeGetCharVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> {
        var val = unsafeGet(obj, offset, ValueTypeKind.CHAR, OpConstants.GetOp.VOLATILE);
        yield val instanceof Character character ? character : 0;
      }
      case null -> (char) MemoryOps.memGet(ValueTypeKind.CHAR, OpConstants.GetOp.VOLATILE, offset);
    };
  }

  /* replacement for getObject(Object, long) */
  @UnsafeReplacement(name = "getObject")
  public static @Nullable Object unsafeGetObject(Object object, long offset) {
    return switch (object) {
      case Object obj -> unsafeGet(obj, offset, ValueTypeKind.REF, OpConstants.GetOp.DEFAULT);
      case null -> null; // cannot get an object from non-heap directly
    };
  }

  /* replacement for getObjectVolatile(Object, long) */
  @UnsafeReplacement(name = "getObjectVolatile")
  public static @Nullable Object unsafeGetObjectVolatile(Object object, long offset) {
    return switch (object) {
      case Object obj -> unsafeGet(obj, offset, ValueTypeKind.REF, OpConstants.GetOp.VOLATILE);
      case null -> null; // cannot get an object from non-heap directly
    };
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Memory Getters">
  /* replacement for getByte(long) */
  @UnsafeReplacement(name = "getByte")
  public static byte unsafeGetByte(long offset) {
    return (byte) MemoryOps.memGet(ValueTypeKind.BYTE, OpConstants.GetOp.DEFAULT, offset);
  }

  /* replacement for getShort(long) */
  @UnsafeReplacement(name = "getShort")
  public static short unsafeGetShort(long offset) {
    return (short) MemoryOps.memGet(ValueTypeKind.SHORT, OpConstants.GetOp.DEFAULT, offset);
  }

  /* replacement for getChar(long) */
  @UnsafeReplacement(name = "getChar")
  public static char unsafeGetChar(long offset) {
    return (char) MemoryOps.memGet(ValueTypeKind.CHAR, OpConstants.GetOp.DEFAULT, offset);
  }

  /* replacement for getInt(long) */
  @UnsafeReplacement(name = "getInt")
  public static int unsafeGetInt(long offset) {
    return (int) MemoryOps.memGet(ValueTypeKind.INT, OpConstants.GetOp.DEFAULT, offset);
  }

  /* replacement for getLong(long) */
  @UnsafeReplacement(name = "getLong")
  public static long unsafeGetLong(long offset) {
    return (long) MemoryOps.memGet(ValueTypeKind.LONG, OpConstants.GetOp.DEFAULT, offset);
  }

  /* replacement for getFloat(long) */
  @UnsafeReplacement(name = "getFloat")
  public static float unsafeGetFloat(long offset) {
    return (float) MemoryOps.memGet(ValueTypeKind.FLOAT, OpConstants.GetOp.DEFAULT, offset);
  }

  /* replacement for getDouble(long) */
  @UnsafeReplacement(name = "getDouble")
  public static double unsafeGetDouble(long offset) {
    return (double) MemoryOps.memGet(ValueTypeKind.DOUBLE, OpConstants.GetOp.DEFAULT, offset);
  }

  /* replacement for getAddress(long) */
  @UnsafeReplacement(name = "getAddress")
  public static long unsafeGetAddress(long offset) {
    return switch (ADDRESS_SIZE) {
      case 4 -> Integer.toUnsignedLong(unsafeGetInt(offset));
      case 8 -> unsafeGetLong(offset);
      default -> throw new AssertionError(); // cannot happen
    };
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Field/Array Setters">
  /* shared code to set a value of a field or array */
  private static void unsafePut(
    @NonNull Object object,
    long offset,
    @Nullable Object value,
    @NonNull ValueTypeKind kind,
    @NonNull OpConstants.SetOp op
  ) {
    try {
      var type = object.getClass();
      if (type.isArray()) {
        ArrayOps.arrayPut(kind, op, object, offset, value);
        return;
      }

      // requested read of static or instance field
      var fieldAccessor = FieldOffsetOps.fieldFromOffset(object, offset);
      if (fieldAccessor != null) {
        fieldAccessor.put(object, value, op);
      }
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug(
        "Unable to unsafe set: [obj={}, offset={}, val={} op={}]",
        object, offset, value, op, throwable);
    }
  }

  /* replacement for putInt(Object, long, int) */
  @UnsafeReplacement(name = "putInt")
  public static void unsafePutInt(Object object, long offset, int value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.INT, OpConstants.SetOp.DEFAULT);
      case null -> unsafePutInt(offset, value);
    }
  }

  /* replacement for putIntVolatile(Object, long, int) */
  @UnsafeReplacement(name = "putIntVolatile")
  public static void unsafePutIntVolatile(Object object, long offset, int value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.INT, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.INT, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putOrderedInt(Object, long, int) */
  @UnsafeReplacement(name = "putOrderedInt")
  public static void unsafePutOrderedInt(Object object, long offset, int value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.INT, OpConstants.SetOp.RELEASE);
      case null -> MemoryOps.memPut(ValueTypeKind.INT, OpConstants.SetOp.RELEASE, offset, value);
    }
  }

  /* replacement for putByte(Object, long, byte) */
  @UnsafeReplacement(name = "putByte")
  public static void unsafePutByte(Object object, long offset, byte value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.BYTE, OpConstants.SetOp.DEFAULT);
      case null -> unsafePutByte(offset, value);
    }
  }

  /* replacement for putByteVolatile(Object, long, byte) */
  @UnsafeReplacement(name = "putByteVolatile")
  public static void unsafePutByteVolatile(Object object, long offset, byte value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.BYTE, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.BYTE, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putShort(Object, long, short) */
  @UnsafeReplacement(name = "putShort")
  public static void unsafePutShort(Object object, long offset, short value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.SHORT, OpConstants.SetOp.DEFAULT);
      case null -> unsafePutShort(offset, value);
    }
  }

  /* replacement for putShortVolatile(Object, long, short) */
  @UnsafeReplacement(name = "putShortVolatile")
  public static void unsafePutShortVolatile(Object object, long offset, short value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.SHORT, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.SHORT, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putLong(Object, long, long) */
  @UnsafeReplacement(name = "putLong")
  public static void unsafePutLong(Object object, long offset, long value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.LONG, OpConstants.SetOp.DEFAULT);
      case null -> unsafePutLong(offset, value);
    }
  }

  /* replacement for putLongVolatile(Object, long, long) */
  @UnsafeReplacement(name = "putLongVolatile")
  public static void unsafePutLongVolatile(Object object, long offset, long value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.LONG, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.LONG, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putOrderedLong(Object, long, long) */
  @UnsafeReplacement(name = "putOrderedLong")
  public static void unsafePutOrderedLong(Object object, long offset, long value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.LONG, OpConstants.SetOp.RELEASE);
      case null -> MemoryOps.memPut(ValueTypeKind.LONG, OpConstants.SetOp.RELEASE, offset, value);
    }
  }

  /* replacement for putFloat(Object, long, float) */
  @UnsafeReplacement(name = "putFloat")
  public static void unsafePutFloat(Object object, long offset, float value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.FLOAT, OpConstants.SetOp.DEFAULT);
      case null -> unsafePutFloat(offset, value);
    }
  }

  /* replacement for putFloatVolatile(Object, long, float) */
  @UnsafeReplacement(name = "putFloatVolatile")
  public static void unsafePutFloatVolatile(Object object, long offset, float value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.FLOAT, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.FLOAT, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putDouble(Object, long, double) */
  @UnsafeReplacement(name = "putDouble")
  public static void unsafePutDouble(Object object, long offset, double value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.DOUBLE, OpConstants.SetOp.DEFAULT);
      case null -> unsafePutDouble(offset, value);
    }
  }

  /* replacement for putDoubleVolatile(Object, long, double) */
  @UnsafeReplacement(name = "putDoubleVolatile")
  public static void unsafePutDoubleVolatile(Object object, long offset, double value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.DOUBLE, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.DOUBLE, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putBoolean(Object, long, boolean) */
  @UnsafeReplacement(name = "putBoolean")
  public static void unsafePutBoolean(Object object, long offset, boolean value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.BOOL, OpConstants.SetOp.DEFAULT);
      case null -> MemoryOps.memPut(ValueTypeKind.BOOL, OpConstants.SetOp.DEFAULT, offset, value);
    }
  }

  /* replacement for putBooleanVolatile(Object, long, boolean) */
  @UnsafeReplacement(name = "putBooleanVolatile")
  public static void unsafePutBooleanVolatile(Object object, long offset, boolean value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.BOOL, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.BOOL, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putChar(Object, long, char) */
  @UnsafeReplacement(name = "putChar")
  public static void unsafePutChar(Object object, long offset, char value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.CHAR, OpConstants.SetOp.DEFAULT);
      case null -> unsafePutChar(offset, value);
    }
  }

  /* replacement for putCharVolatile(Object, long, char) */
  @UnsafeReplacement(name = "putCharVolatile")
  public static void unsafePutCharVolatile(Object object, long offset, char value) {
    switch (object) {
      case Object obj -> unsafePut(obj, offset, value, ValueTypeKind.CHAR, OpConstants.SetOp.VOLATILE);
      case null -> MemoryOps.memPut(ValueTypeKind.CHAR, OpConstants.SetOp.VOLATILE, offset, value);
    }
  }

  /* replacement for putObject(Object, long, Object) */
  @UnsafeReplacement(name = "putObject")
  public static void unsafePutObject(Object object, long offset, Object value) {
    if (object != null) {
      unsafePut(object, offset, value, ValueTypeKind.REF, OpConstants.SetOp.DEFAULT);
    }
  }

  /* replacement for putObjectVolatile(Object, long, Object) */
  @UnsafeReplacement(name = "putObjectVolatile")
  public static void unsafePutObjectVolatile(Object object, long offset, Object value) {
    if (object != null) {
      unsafePut(object, offset, value, ValueTypeKind.REF, OpConstants.SetOp.VOLATILE);
    }
  }

  /* replacement for putOrderedObject(Object, long, Object) */
  @UnsafeReplacement(name = "putOrderedObject")
  public static void unsafePutOrderedObject(Object object, long offset, Object value) {
    if (object != null) {
      unsafePut(object, offset, value, ValueTypeKind.REF, OpConstants.SetOp.RELEASE);
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Memory Setters">
  /* replacement for putByte(long, byte) */
  @UnsafeReplacement(name = "putByte")
  public static void unsafePutByte(long offset, byte value) {
    MemoryOps.memPut(ValueTypeKind.BYTE, OpConstants.SetOp.DEFAULT, offset, value);
  }

  /* replacement for putShort(long, short) */
  @UnsafeReplacement(name = "putShort")
  public static void unsafePutShort(long offset, short value) {
    MemoryOps.memPut(ValueTypeKind.SHORT, OpConstants.SetOp.DEFAULT, offset, value);
  }

  /* replacement for putChar(long, char) */
  @UnsafeReplacement(name = "putChar")
  public static void unsafePutChar(long offset, char value) {
    MemoryOps.memPut(ValueTypeKind.CHAR, OpConstants.SetOp.DEFAULT, offset, value);
  }

  /* replacement for putInt(long, int) */
  @UnsafeReplacement(name = "putInt")
  public static void unsafePutInt(long offset, int value) {
    MemoryOps.memPut(ValueTypeKind.INT, OpConstants.SetOp.DEFAULT, offset, value);
  }

  /* replacement for putLong(long, long) */
  @UnsafeReplacement(name = "putLong")
  public static void unsafePutLong(long offset, long value) {
    MemoryOps.memPut(ValueTypeKind.LONG, OpConstants.SetOp.DEFAULT, offset, value);
  }

  /* replacement for putFloat(long, float) */
  @UnsafeReplacement(name = "putFloat")
  public static void unsafePutFloat(long offset, float value) {
    MemoryOps.memPut(ValueTypeKind.FLOAT, OpConstants.SetOp.DEFAULT, offset, value);
  }

  /* replacement for putDouble(long, double) */
  @UnsafeReplacement(name = "putDouble")
  public static void unsafePutDouble(long offset, double value) {
    MemoryOps.memPut(ValueTypeKind.DOUBLE, OpConstants.SetOp.DEFAULT, offset, value);
  }

  /* replacement for putAddress(long, long) */
  @UnsafeReplacement(name = "putAddress")
  public static void unsafePutAddress(long offset, long value) {
    switch (ADDRESS_SIZE) {
      case 4 -> unsafePutInt(offset, (int) value);
      case 8 -> unsafePutLong(offset, value);
      default -> throw new AssertionError(); // cannot happen
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Field/Array/Memory CAS Operations">
  /* shared code to cas a value of a field or array */
  private static boolean unsafeCas(
    @NonNull Object object,
    long offset,
    @Nullable Object expected,
    @Nullable Object value,
    @NonNull ValueTypeKind kind
  ) {
    try {
      var type = object.getClass();
      if (type.isArray()) {
        return ArrayOps.arrayComparePut(kind, object, offset, expected, value);
      }

      // requested read of static or instance field
      var fieldAccessor = FieldOffsetOps.fieldFromOffset(object, offset);
      if (fieldAccessor != null) {
        return fieldAccessor.compareAndSet(object, expected, value);
      }

      return false;
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug(
        "Unable to unsafe CAS: [obj={}, offset={}, expected={} val={}]",
        object, offset, expected, value, throwable);
      return false;
    }
  }

  /* replacement for compareAndSwapInt(Object, long, int, int) */
  @UnsafeReplacement(name = "compareAndSwapInt")
  public static boolean unsafeCasInt(Object object, long offset, int expected, int value) {
    return switch (object) {
      case Object obj -> unsafeCas(obj, offset, expected, value, ValueTypeKind.INT);
      case null -> MemoryOps.memComparePut(ValueTypeKind.INT, offset, expected, value);
    };
  }

  /* replacement for compareAndSwapLong(Object, long, long, long) */
  @UnsafeReplacement(name = "compareAndSwapLong")
  public static boolean unsafeCasLong(Object object, long offset, long expected, long value) {
    return switch (object) {
      case Object obj -> unsafeCas(obj, offset, expected, value, ValueTypeKind.LONG);
      case null -> MemoryOps.memComparePut(ValueTypeKind.LONG, offset, expected, value);
    };
  }

  /* replacement for compareAndSwapObject(Object, long, Object, Object) */
  @UnsafeReplacement(name = "compareAndSwapObject")
  public static boolean unsafeCasObject(Object object, long offset, Object expected, Object value) {
    return switch (object) {
      case Object obj -> unsafeCas(obj, offset, expected, value, ValueTypeKind.REF);
      case null -> false; // cannot put an object directly into memory
    };
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Field/Array/Memory Get&Add Operations">
  /* shared code to cas a value of a field or array */
  private static @Nullable Object unsafeGetAndAdd(
    @NonNull Object object,
    long offset,
    @NonNull Number delta,
    @NonNull ValueTypeKind kind
  ) {
    try {
      var type = object.getClass();
      if (type.isArray()) {
        return ArrayOps.arrayGetAdd(kind, object, offset, delta);
      }

      // requested read of static or instance field
      var fieldAccessor = FieldOffsetOps.fieldFromOffset(object, offset);
      if (fieldAccessor != null) {
        return fieldAccessor.getAndAdd(object, delta);
      }

      return null;
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug(
        "Unable to unsafe get and add: [obj={}, offset={}, delta={}]",
        object, offset, delta, throwable);
      return null;
    }
  }

  /* replacement for getAndAddInt(Object, long, int) */
  @UnsafeReplacement(name = "getAndAddInt")
  public static int unsafeGetAndAddInt(Object object, long offset, int delta) {
    return switch (object) {
      case Object obj -> {
        var ret = unsafeGetAndAdd(obj, offset, delta, ValueTypeKind.INT);
        yield ret instanceof Number number ? number.intValue() : 0;
      }
      case null -> (int) MemoryOps.memGetAdd(ValueTypeKind.INT, offset, delta);
    };
  }

  /* replacement for getAndAddLong(Object, long, long) */
  @UnsafeReplacement(name = "getAndAddLong")
  public static long unsafeGetAndAddLong(Object object, long offset, long delta) {
    return switch (object) {
      case Object obj -> {
        var ret = unsafeGetAndAdd(obj, offset, delta, ValueTypeKind.LONG);
        yield ret instanceof Number number ? number.longValue() : 0;
      }
      case null -> (long) MemoryOps.memGetAdd(ValueTypeKind.LONG, offset, delta);
    };
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Field/Array/Memory Get&Set Operations">
  /* shared code to cas a value of a field or array */
  private static @Nullable Object unsafeGetAndSet(
    @NonNull Object object,
    long offset,
    @Nullable Object value,
    @NonNull ValueTypeKind kind
  ) {
    try {
      var type = object.getClass();
      if (type.isArray()) {
        return ArrayOps.arrayGetPut(kind, object, offset, value);
      }

      // requested read of static or instance field
      var fieldAccessor = FieldOffsetOps.fieldFromOffset(object, offset);
      if (fieldAccessor != null) {
        return fieldAccessor.getAndPut(object, value);
      }

      return null;
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug(
        "Unable to unsafe get and set: [obj={}, offset={}, val={}]",
        object, offset, value, throwable);
      return null;
    }
  }

  /* replacement for getAndSetInt(Object, long, int) */
  @UnsafeReplacement(name = "getAndSetInt")
  public static int unsafeGetAndSetInt(Object object, long offset, int value) {
    return switch (object) {
      case Object obj -> {
        var ret = unsafeGetAndSet(obj, offset, value, ValueTypeKind.INT);
        yield ret instanceof Number number ? number.intValue() : 0;
      }
      case null -> (int) MemoryOps.memGetPut(ValueTypeKind.INT, offset, value);
    };
  }

  /* replacement for getAndSetLong(Object, long, long) */
  @UnsafeReplacement(name = "getAndSetLong")
  public static long unsafeGetAndSetLong(Object object, long offset, long value) {
    return switch (object) {
      case Object obj -> {
        var ret = unsafeGetAndSet(obj, offset, value, ValueTypeKind.LONG);
        yield ret instanceof Number number ? number.longValue() : 0;
      }
      case null -> (long) MemoryOps.memGetPut(ValueTypeKind.LONG, offset, value);
    };
  }

  /* replacement for getAndSetObject(Object, long, Object) */
  @UnsafeReplacement(name = "getAndSetObject")
  public static Object unsafeGetAndSetObject(Object object, long offset, Object value) {
    return switch (object) {
      case Object obj -> unsafeGetAndSet(obj, offset, value, ValueTypeKind.REF);
      case null -> null; // cannot put an object directly into memory
    };
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Memory Control Operations">
  // validates that the given byte count is valid, throwing an IAE if that is not the case
  private static void validateByteCount(long byteCount) {
    if (ADDRESS_SIZE == 4) {
      var is32BitClean = byteCount >>> 32 == 0;
      if (!is32BitClean) {
        throw new IllegalArgumentException();
      }
    } else if (byteCount < 0) {
      throw new IllegalArgumentException();
    }
  }

  // validates that malloc or realloc did return a valid memory address, throwing an OOM if that is not the case
  private static long validateAddress(long address, long byteCount) {
    if (address == 0) {
      throw new OutOfMemoryError("Unable to allocate " + byteCount + " bytes");
    }

    return address;
  }

  /* replacement for allocateMemory(long) */
  @UnsafeReplacement(name = "allocateMemory")
  public static long unsafeAllocateMemory(long byteCount) {
    validateByteCount(byteCount);
    if (byteCount == 0) {
      return 0L; // mimics current behaviour
    }

    var memoryAddress = MemoryControlOps.malloc(byteCount);
    return validateAddress(memoryAddress, byteCount);
  }

  /* replacement for reallocateMemory(long, long) */
  @UnsafeReplacement(name = "reallocateMemory")
  public static long unsafeReallocateMemory(long address, long byteCount) {
    validateByteCount(byteCount);
    if (byteCount == 0) {
      // free the given block of memory
      unsafeFreeMemory(address);
      return 0;
    }

    if (address == 0) {
      // address 0 means that new memory should be allocated
      return unsafeAllocateMemory(byteCount);
    }

    // allocate a new block; copy the memory from the old block into the new block, free the old block (if known)
    var memoryAddress = MemoryControlOps.realloc(address, byteCount);
    return validateAddress(memoryAddress, byteCount);
  }

  /* replacement for freeMemory(long) */
  @UnsafeReplacement(name = "freeMemory")
  public static void unsafeFreeMemory(long address) {
    MemoryControlOps.free(address);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Memory/Array Copy/Fill Operations">
  /* common method to ensure that a given class is a primitive array, returning the component type kind in that case */
  private static @NonNull ValueTypeKind primitiveArrayComponentType(@NonNull Object o) {
    var componentType = o.getClass().getComponentType();
    if (componentType == null || !componentType.isPrimitive()) {
      throw new IllegalArgumentException(); // mimics current behavior
    }

    return ValueTypeKind.of(componentType);
  }

  /* replacement for setMemory(Object, long, long, byte) */
  @UnsafeReplacement(name = "setMemory")
  public static void unsafeSetMemory(Object base, long offset, long byteCount, byte value) {
    if (base != null) {
      // request to fill an on-heap array
      var kind = primitiveArrayComponentType(base);
      ArrayOps.arrayFill(kind, base, offset, byteCount, value);
    } else {
      // request to fill an off-heap memory region
      unsafeSetMemory(offset, byteCount, value);
    }
  }

  /* replacement for setMemory(long, long, byte) */
  @UnsafeReplacement(name = "setMemory")
  public static void unsafeSetMemory(long address, long byteCount, byte value) {
    MemoryOps.memFill(address, byteCount, value);
  }

  /* replacement for copyMemory(Object, long, Object, long, long) */
  @UnsafeReplacement(name = "copyMemory")
  public static void unsafeCopyMemory(Object srcBase, long srcOff, Object dstBase, long dstOff, long byteCount) {
    if (srcBase == null && dstBase == null) {
      // no on-heap arrays given; request to copy off-heap memory
      unsafeCopyMemory(srcOff, dstOff, byteCount);
      return;
    }

    if (srcBase != null && dstBase != null) {
      // both on-heap arrays given; copy from one array to the other
      var srcTypeKind = primitiveArrayComponentType(srcBase);
      var dstTypeKind = primitiveArrayComponentType(dstBase);
      ArrayOps.arrayCopy(srcTypeKind, srcBase, srcOff, dstTypeKind, dstBase, dstOff, byteCount);
      return;
    }

    if (srcBase != null) {
      // src is an on-heap array; copy the on-heap array into off-heap memory
      var srcTypeKind = primitiveArrayComponentType(srcBase);
      MemoryOps.memFromHeapCopy(srcTypeKind, srcBase, srcOff, dstOff, byteCount);
      return;
    }

    // dest is an on-heap array; copy off-heap memory into the on-heap array
    var dstTypeKind = primitiveArrayComponentType(dstBase);
    MemoryOps.memToHeapCopy(dstTypeKind, dstBase, srcOff, dstOff, byteCount);
  }

  /* replacement for copyMemory(long, long, long) */
  @UnsafeReplacement(name = "copyMemory")
  public static void unsafeCopyMemory(long srcOff, long dstOff, long byteCount) {
    MemoryOps.memCopy(srcOff, dstOff, byteCount);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Fence Operations">
  /* replacement for loadFence() */
  @UnsafeReplacement(name = "loadFence")
  public static void unsafeLoadFence() {
    VarHandle.acquireFence();
  }

  /* replacement for storeFence() */
  @UnsafeReplacement(name = "storeFence")
  public static void unsafeStoreFence() {
    VarHandle.releaseFence();
  }

  /* replacement for fullFence() */
  @UnsafeReplacement(name = "fullFence")
  public static void unsafeFullFence() {
    VarHandle.fullFence();
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Thread Parking Operations">
  /* replacement for unpark(Object) */
  @UnsafeReplacement(name = "unpark")
  public static void unsafeUnpark(Object thread) {
    if (thread instanceof Thread t) {
      LockSupport.unpark(t);
    }
  }

  /* replacement for park(boolean, long) */
  @UnsafeReplacement(name = "park")
  public static void unsafePark(boolean isAbsolute, long time) {
    if (isAbsolute) {
      LockSupport.parkUntil(time);
    } else {
      LockSupport.parkNanos(time);
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Class Initialization Methods">
  /* replacement for shouldBeInitialized(Class) */
  @UnsafeReplacement(name = "shouldBeInitialized")
  public static boolean unsafeShouldBeInitialized(Class<?> clazz) {
    return false; // there is no replacement and no need for one
  }

  /* replacement for ensureClassInitialized(Class) */
  @UnsafeReplacement(name = "ensureClassInitialized")
  public static void unsafeEnsureClassInitialized(Class<?> clazz) {
    try {
      if (!clazz.isPrimitive() && !clazz.isArray()) {
        var lookup = OpConstants.TRUSTED_LOOKUP.get();
        lookup.in(clazz).ensureInitialized(clazz);
      }
    } catch (IllegalAccessException _) {
      throw new AssertionError(); // cannot happen
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Class Defining Operations">
  /* replacement for defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain) */
  @UnsafeReplacement(name = "defineClass")
  public static Class<?> unsafeDefineClass(
    String name,
    byte[] data,
    int off,
    int len,
    ClassLoader loader,
    ProtectionDomain pd
  ) {
    try {
      var classLoaderDefineMh = CLASS_DEFINE_METHOD_HANDLE.get();
      return (Class<?>) classLoaderDefineMh.invokeExact(
        loader,
        name,
        data,
        off,
        len,
        pd,
        (String) null);
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  /* replacement for defineAnonymousClass(Class<?>, byte[], Object[]) */
  @UnsafeReplacement(name = "defineAnonymousClass")
  public static Class<?> unsafeDefineAnonymousClass(Class<?> host, byte[] data, Object[] patches) {
    try {
      var lookup = OpConstants.TRUSTED_LOOKUP.get();
      return lookup
        .in(host)
        .defineHiddenClass(data, true, MethodHandles.Lookup.ClassOption.NESTMATE)
        .lookupClass();
    } catch (IllegalAccessException _) {
      throw new AssertionError(); // cannot happen
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Misc Operations">
  /* replacement for addressSize() */
  @UnsafeReplacement(name = "addressSize")
  public static int unsafeAddressSize() {
    return ADDRESS_SIZE;
  }

  /* replacement for invokeCleaner(ByteBuffer) */
  @UnsafeReplacement(name = "invokeCleaner")
  public static void unsafeInvokeCleaner(ByteBuffer buffer) {
    if (!buffer.isDirect()) {
      throw new IllegalArgumentException("buffer is non-direct"); // mimics current behavior
    }

    var cleanerInvoker = BB_CLEANER.get();
    cleanerInvoker.accept(buffer);
  }

  /* replacement for getLoadAverage(double[], int) */
  @UnsafeReplacement(name = "getLoadAverage")
  public static int unsafeGetLoadAverage(double[] loadAverage, int reqSampleCount) {
    if (reqSampleCount < 0 || reqSampleCount > 3 || reqSampleCount > loadAverage.length) {
      throw new ArrayIndexOutOfBoundsException(); // mimics current behavior
    }

    var osMxBean = OS_MX_BEAN.get();
    var loadAvg = osMxBean.getSystemLoadAverage();
    if (loadAvg == -1) {
      return -1; // load avg unavailable
    } else {
      loadAverage[0] = loadAvg;
      return 1; // OsMxBean can only provide the load avg of the last minute
    }
  }
  //</editor-fold>
}
