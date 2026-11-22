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

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UnsafeReplacementDelegateTest {

  private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

  @Test
  void testAllocateMemory() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    Assertions.assertNotEquals(0L, address);

    UnsafeReplacementDelegate.unsafePutLong(address, Long.MAX_VALUE);
    Assertions.assertEquals(Long.MAX_VALUE, UnsafeReplacementDelegate.unsafeGetLong(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);

    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeAllocateMemory(-1));
    Assertions.assertThrows(
      OutOfMemoryError.class,
      () -> UnsafeReplacementDelegate.unsafeAllocateMemory(Long.MAX_VALUE));
  }

  @Test
  void testCopyOffHeapMemory() {
    var srcAddr = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    var dstAddr = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    Assertions.assertNotEquals(0L, srcAddr);
    Assertions.assertNotEquals(0L, dstAddr);

    for (var i = 0; i < 8; i++) {
      UnsafeReplacementDelegate.unsafePutByte(srcAddr + i, (byte) i);
      UnsafeReplacementDelegate.unsafeCopyMemory(srcAddr, dstAddr, i);
      for (var j = 0; j < i; j++) {
        Assertions.assertEquals(j, UnsafeReplacementDelegate.unsafeGetByte(srcAddr + j));
        Assertions.assertEquals(j, UnsafeReplacementDelegate.unsafeGetByte(dstAddr + j));
      }
    }

    UnsafeReplacementDelegate.unsafeFreeMemory(srcAddr);
    UnsafeReplacementDelegate.unsafeFreeMemory(dstAddr);
  }

  @Test
  void testCopyOnHeapMemory() {
    var src = new int[8];
    var dst = new int[8];

    var scale = UnsafeReplacementDelegate.UNSAFE_ARRAY_INT_INDEX_SCALE;
    Assertions.assertEquals(4, scale);

    for (var i = 0L; i < 8; i++) {
      UnsafeReplacementDelegate.unsafePutInt(src, i * scale, (int) i);
      UnsafeReplacementDelegate.unsafeCopyMemory(src, 0, dst, 0, (i + 1) * scale);
      for (var j = 0L; j < i; j++) {
        Assertions.assertEquals(j, UnsafeReplacementDelegate.unsafeGetInt(src, j * scale));
        Assertions.assertEquals(j, UnsafeReplacementDelegate.unsafeGetInt(dst, j * scale));
      }
    }
  }

  @Test
  void testCopyOnHeapMemoryBool() {
    var src = new boolean[8];
    var dst = new boolean[8];

    var scale = UnsafeReplacementDelegate.UNSAFE_ARRAY_BOOLEAN_INDEX_SCALE;
    Assertions.assertEquals(1, scale);

    for (var i = 0L; i < 8; i++) {
      UnsafeReplacementDelegate.unsafePutBoolean(src, i * scale, i % 2 == 0);
      UnsafeReplacementDelegate.unsafeCopyMemory(src, 0, dst, 0, (i + 1) * scale);
      for (var j = 0L; j < i; j++) {
        Assertions.assertEquals(j % 2 == 0, UnsafeReplacementDelegate.unsafeGetBoolean(src, j * scale));
        Assertions.assertEquals(j % 2 == 0, UnsafeReplacementDelegate.unsafeGetBoolean(dst, j * scale));
      }
    }
  }

  @Test
  void testCopyOffHeapToOnHeap() {
    var srcAddr = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    var dst = new long[4];

    var scale = UnsafeReplacementDelegate.UNSAFE_ARRAY_LONG_INDEX_SCALE;
    Assertions.assertEquals(8, scale);

    for (var i = 0L; i < 4; i++) {
      UnsafeReplacementDelegate.unsafePutLong(srcAddr + (i * scale), Integer.MAX_VALUE + i);
      UnsafeReplacementDelegate.unsafeCopyMemory(null, srcAddr, dst, 0, (i + 1) * scale);
      for (var j = 0L; j < i; j++) {
        Assertions.assertEquals(Integer.MAX_VALUE + j, UnsafeReplacementDelegate.unsafeGetLong(srcAddr + (j * scale)));
        Assertions.assertEquals(Integer.MAX_VALUE + j, UnsafeReplacementDelegate.unsafeGetLong(dst, j * scale));
      }
    }

    UnsafeReplacementDelegate.unsafeFreeMemory(srcAddr);
  }

  @Test
  void testCopyOffHeapToOnHeapBool() {
    var srcAddr = UnsafeReplacementDelegate.unsafeAllocateMemory(4);
    var dst = new boolean[4];

    var scale = UnsafeReplacementDelegate.UNSAFE_ARRAY_BOOLEAN_INDEX_SCALE;
    Assertions.assertEquals(1, scale);

    for (var i = 0L; i < 4; i++) {
      UnsafeReplacementDelegate.unsafePutBoolean(null, srcAddr + (i * scale), i % 2 == 0);
      UnsafeReplacementDelegate.unsafeCopyMemory(null, srcAddr, dst, 0, (i + 1) * scale);
      for (var j = 0L; j < i; j++) {
        Assertions.assertEquals(j % 2 == 0, UnsafeReplacementDelegate.unsafeGetBoolean(null, srcAddr + (j * scale)));
        Assertions.assertEquals(j % 2 == 0, UnsafeReplacementDelegate.unsafeGetBoolean(dst, j * scale));
      }
    }

    UnsafeReplacementDelegate.unsafeFreeMemory(srcAddr);
  }

  @Test
  void testCopyOnHeapToOffHeap() {
    var src = new long[4];
    var dstAddr = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    var scale = UnsafeReplacementDelegate.UNSAFE_ARRAY_LONG_INDEX_SCALE;

    for (var i = 0L; i < 4; i++) {
      UnsafeReplacementDelegate.unsafePutLong(src, i * scale, Integer.MAX_VALUE + i);
      UnsafeReplacementDelegate.unsafeCopyMemory(src, 0, null, dstAddr, (i + 1) * scale);
      for (var j = 0L; j < i; j++) {
        Assertions.assertEquals(Integer.MAX_VALUE + j, UnsafeReplacementDelegate.unsafeGetLong(src, j * scale));
        Assertions.assertEquals(Integer.MAX_VALUE + j, UnsafeReplacementDelegate.unsafeGetLong(dstAddr + (j * scale)));
      }
    }

    UnsafeReplacementDelegate.unsafeFreeMemory(dstAddr);
  }

  @Test
  void testSetOffHeapMemory() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    UnsafeReplacementDelegate.unsafeSetMemory(address, 8L, (byte) 123);
    for (var index = 0; index < 8; index++) {
      Assertions.assertEquals(123, UnsafeReplacementDelegate.unsafeGetByte(address + index));
    }

    UnsafeReplacementDelegate.unsafeFreeMemory(address);
  }

  @Test
  void testSetOnHeapMemory() {
    var arr = new long[2];
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    UnsafeReplacementDelegate.unsafeSetMemory(arr, 0, 2L * scale, (byte) 1);
    Assertions.assertEquals(0x0101010101010101L, arr[0]);
    Assertions.assertEquals(0x0101010101010101L, arr[1]);
  }

  @Test
  void testSetOnHeapMemoryBool() {
    var arr = new boolean[]{false, false};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    UnsafeReplacementDelegate.unsafeSetMemory(arr, 0, 2L * scale, (byte) 1);
    Assertions.assertTrue(arr[0]);
    Assertions.assertTrue(arr[1]);
  }

  @Test
  void testReallocateOffHeapMemory() {
    // test that re-allocation copies previous memory
    {
      var address = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
      UnsafeReplacementDelegate.unsafePutLong(address, 1337L);
      var newAddress = UnsafeReplacementDelegate.unsafeReallocateMemory(address, 16);
      Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetLong(newAddress));
      UnsafeReplacementDelegate.unsafeFreeMemory(newAddress);
    }

    // test the re-allocation with 0 bytes frees the old segment and returns a null pointer
    {
      var address = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
      var newAddress = UnsafeReplacementDelegate.unsafeReallocateMemory(address, 0);
      Assertions.assertEquals(0L, newAddress);
    }

    // test that re-allocation with a null pointer results in a normal allocation
    {
      var address = UnsafeReplacementDelegate.unsafeReallocateMemory(0, 8);
      UnsafeReplacementDelegate.unsafePutLong(address, 1337L);
      Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetLong(address));
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testStaticFieldOffset() throws NoSuchFieldException {
    var inst = new ClassWithFields();

    // int
    {
      var field = ClassWithFields.class.getDeclaredField("intField");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetInt(base, offset);
      UnsafeReplacementDelegate.unsafePutOrderedInt(base, offset, 123456789);
      Assertions.assertEquals(1337, oldVal);
      Assertions.assertEquals(123456789, inst.getFieldValues()[1]);
      UnsafeReplacementDelegate.unsafePutInt(base, offset, oldVal); // reset value
    }

    // long
    {
      var field = ClassWithFields.class.getDeclaredField("longField");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetLong(base, offset);
      UnsafeReplacementDelegate.unsafePutLong(base, offset, 123456789L);
      Assertions.assertEquals(696969L, oldVal);
      Assertions.assertEquals(123456789L, inst.getFieldValues()[2]);
      UnsafeReplacementDelegate.unsafePutLong(base, offset, oldVal); // reset value
    }

    // string
    {
      var field = ClassWithFields.class.getDeclaredField("STR");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = (String) UnsafeReplacementDelegate.unsafeGetObject(base, offset);
      UnsafeReplacementDelegate.unsafePutObject(base, offset, "hi :)");
      Assertions.assertEquals("hello world", oldVal);
      Assertions.assertEquals("hi :)", inst.getFieldValues()[0]);
      UnsafeReplacementDelegate.unsafePutObject(base, offset, oldVal); // reset value
    }
  }

  @Test
  void testInstanceFieldOffset() throws NoSuchFieldException {
    var inst = new ClassWithFields();

    // int
    {
      var field = ClassWithFields.class.getDeclaredField("instIntField");
      var offset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetInt(inst, offset);
      UnsafeReplacementDelegate.unsafePutIntVolatile(inst, offset, 1337);
      Assertions.assertEquals(123456789, oldVal);
      Assertions.assertEquals(1337, inst.getFieldValues()[3]);
    }

    // string
    {
      var field = ClassWithFields.class.getDeclaredField("instStringField");
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = (String) UnsafeReplacementDelegate.unsafeGetObjectVolatile(inst, offset);
      UnsafeReplacementDelegate.unsafePutOrderedObject(inst, offset, "hello");
      Assertions.assertEquals("final string", oldVal);
      Assertions.assertEquals("hello", inst.getFieldValues()[4]);
    }
  }

  @Test
  void testGetInheritedField() throws NoSuchFieldException {
    var addressField = Buffer.class.getDeclaredField("address");
    var addressOffset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(addressField);
    var directBuffer = ByteBuffer.allocateDirect(5);
    var unsafeAddress = Assertions.assertDoesNotThrow(
      () -> UnsafeReplacementDelegate.unsafeGetLong(directBuffer, addressOffset));
    var memSegAddress = MemorySegment.ofBuffer(directBuffer).address();
    Assertions.assertEquals(memSegAddress, unsafeAddress);
  }

  @Test
  void testGetFieldFromClass() throws Exception {
    // static field
    {
      var annotationField = Class.class.getDeclaredField("ANNOTATION");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(annotationField);
      var off = UnsafeReplacementDelegate.unsafeStaticFieldOffset(annotationField);
      var value = UnsafeReplacementDelegate.unsafeGetLong(base, off);
      Assertions.assertEquals(0x00002000, value);
    }

    // instance field
    {
      var moduleField = Class.class.getDeclaredField("module");
      var off = UnsafeReplacementDelegate.unsafeObjectFieldOffset(moduleField);
      var value = UnsafeReplacementDelegate.unsafeGetObject(UnsafeReplacementDelegateTest.class, off);
      Assertions.assertNotNull(value);
      Assertions.assertSame(UnsafeReplacementDelegate.class.getModule(), value);
    }

    // instance field
    {
      var nameField = Class.class.getDeclaredField("name");
      var off = UnsafeReplacementDelegate.unsafeObjectFieldOffset(nameField);
      var value = UnsafeReplacementDelegate.unsafeGetObject(Class.class, off);
      Assertions.assertNotNull(value);
      Assertions.assertSame(Class.class.getName(), value);
    }
  }

  @Test
  void testEachFieldInClassHasDifferentOffset() {
    var inst = new ClassWithFields();
    var seenOffsets = new HashSet<Long>();
    var fields = ClassWithFields.class.getDeclaredFields();
    for (var field : fields) {
      var isStatic = Modifier.isStatic(field.getModifiers());
      var offset = isStatic
        ? UnsafeReplacementDelegate.unsafeStaticFieldOffset(field)
        : UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      Assertions.assertTrue(offset >= FieldOffsetOps.FIELD_BASE_OFFSET);
      Assertions.assertEquals(0, offset % FieldOffsetOps.FIELD_OFFSET_SCALE);
      Assertions.assertTrue(offset < FieldOffsetOps.indexToOffset(FieldOffsetOps.CLASS_FIELD_COUNT + fields.length));
      Assertions.assertTrue(seenOffsets.add(offset));

      var base = isStatic ? UnsafeReplacementDelegate.unsafeStaticFieldBase(field) : inst;
      var resolved = FieldOffsetOps.fieldFromOffset(base, offset);
      Assertions.assertNotNull(resolved);
      Assertions.assertEquals(field, resolved.wrappedField());
    }
  }

  @Test
  void testEachFieldInClassHierarchyHasDifferentOffset() {
    var inst = ByteBuffer.allocate(1);
    var seenOffsets = new HashSet<Long>();

    Class<?> current = inst.getClass();
    do {
      var fields = current.getDeclaredFields();
      for (var field : fields) {
        var isStatic = Modifier.isStatic(field.getModifiers());
        var offset = isStatic
          ? UnsafeReplacementDelegate.unsafeStaticFieldOffset(field)
          : UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
        Assertions.assertTrue(offset >= FieldOffsetOps.FIELD_BASE_OFFSET);
        Assertions.assertEquals(0, offset % FieldOffsetOps.FIELD_OFFSET_SCALE);
        Assertions.assertTrue(offset <= FieldOffsetOps.indexToOffset(FieldOffsetOps.CLASS_FIELD_COUNT + 250));
        Assertions.assertTrue(seenOffsets.add(offset));

        var base = isStatic ? UnsafeReplacementDelegate.unsafeStaticFieldBase(field) : inst;
        var resolved = FieldOffsetOps.fieldFromOffset(base, offset);
        Assertions.assertNotNull(resolved);
        Assertions.assertEquals(field, resolved.wrappedField());
      }
    } while ((current = current.getSuperclass()) != null);
  }

  @Test
  void testDiverseFieldOperations() throws NoSuchFieldException {
    var inst = new ClassWithFields();

    // final object field get&set/cas
    {
      var field = ClassWithFields.class.getDeclaredField("STR");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = (String) UnsafeReplacementDelegate.unsafeGetAndSetObject(base, offset, "world");
      Assertions.assertEquals("hello world", oldVal);
      Assertions.assertEquals("world", inst.getFieldValues()[0]);
      Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(base, offset, "world", oldVal));
      Assertions.assertEquals("hello world", inst.getFieldValues()[0]);
    }

    // object field get&set/cas
    {
      var field = ClassWithFields.class.getDeclaredField("instStringField");
      var offset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      var oldVal = (String) UnsafeReplacementDelegate.unsafeGetAndSetObject(inst, offset, "world");
      Assertions.assertEquals("final string", oldVal);
      Assertions.assertEquals("world", inst.getFieldValues()[4]);
      Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(inst, offset, "world", oldVal));
      Assertions.assertEquals("final string", inst.getFieldValues()[4]);
    }

    // int field get&set, get&add, cas
    {
      var field = ClassWithFields.class.getDeclaredField("intField");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetAndSetLong(base, offset, 123456789);
      Assertions.assertEquals(1337, oldVal);
      Assertions.assertEquals(123456789, UnsafeReplacementDelegate.unsafeGetAndAddInt(base, offset, 250));
      Assertions.assertEquals(123457039, inst.getFieldValues()[1]);
      Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(base, offset, 123457039, (int) oldVal));
      Assertions.assertEquals(1337, inst.getFieldValues()[1]);
    }

    // final int field get&set, get&add, cas
    {
      var field = ClassWithFields.class.getDeclaredField("instIntField");
      var offset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetAndSetLong(inst, offset, 987654321);
      Assertions.assertEquals(123456789, oldVal);
      Assertions.assertEquals(987654321, UnsafeReplacementDelegate.unsafeGetAndAddInt(inst, offset, 250));
      Assertions.assertEquals(987654571, inst.getFieldValues()[3]);
      Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(inst, offset, 987654571, (int) oldVal));
      Assertions.assertEquals(123456789, inst.getFieldValues()[3]);
    }
  }

  @Test
  void testWrongPrimitiveFieldWrites() throws NoSuchFieldException {
    var inst = new ClassWithFields();

    // write long into int field
    {
      var field = ClassWithFields.class.getDeclaredField("intField");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetInt(base, offset);
      UnsafeReplacementDelegate.unsafePutLong(base, offset, 123456789987654321L);
      Assertions.assertEquals(-531432783, inst.getFieldValues()[1]);
      UnsafeReplacementDelegate.unsafePutInt(base, offset, oldVal); // reset value
    }

    // write boolean into int field
    {
      var field = ClassWithFields.class.getDeclaredField("intField");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetInt(base, offset);
      UnsafeReplacementDelegate.unsafePutBoolean(base, offset, true);
      Assertions.assertEquals(
        LITTLE_ENDIAN ? (oldVal & ~0xFF) | 1 : (oldVal & 0x00FFFFFF) | 0x01000000,
        inst.getFieldValues()[1]); // only the first byte in memory order is replaced
      UnsafeReplacementDelegate.unsafePutInt(base, offset, oldVal); // reset value
    }

    // write char into long field
    {
      var field = ClassWithFields.class.getDeclaredField("longField");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetLong(base, offset);
      UnsafeReplacementDelegate.unsafePutChar(base, offset, 'C');
      Assertions.assertEquals(
        LITTLE_ENDIAN ? (oldVal & ~0xFFFFL) | 'C' : (oldVal & 0x0000FFFFFFFFFFFFL) | ((long) 'C' << 48),
        inst.getFieldValues()[2]);
      UnsafeReplacementDelegate.unsafePutLong(base, offset, oldVal); // reset value
    }

    // write long into boolean field
    {
      var field = ClassWithFields.class.getDeclaredField("instBoolField");
      var offset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetBoolean(inst, offset);
      UnsafeReplacementDelegate.unsafePutLong(inst, offset, 123456789987654321L);
      Assertions.assertEquals(true, inst.getFieldValues()[6]);
      UnsafeReplacementDelegate.unsafePutLong(inst, offset, 0x100L);
      Assertions.assertEquals(false, inst.getFieldValues()[6]);
      UnsafeReplacementDelegate.unsafePutBoolean(inst, offset, oldVal); // reset value
    }

    // write boolean into char field
    {
      var field = ClassWithFields.class.getDeclaredField("instCharField");
      var offset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetChar(inst, offset);
      UnsafeReplacementDelegate.unsafePutBoolean(inst, offset, true);
      Assertions.assertEquals(
        (char) (LITTLE_ENDIAN ? (oldVal & 0xFF00) | 1 : (oldVal & 0xFF) | 0x0100), inst.getFieldValues()[5]);
      UnsafeReplacementDelegate.unsafePutChar(inst, offset, oldVal); // reset value
    }
  }

  //<editor-fold defaultstate="collapsed" desc="Field Access Tests">
  private static long staticFieldOffsetOf(Field field) {
    return UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
  }

  private static long instanceFieldOffsetOf(Field field) {
    return UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
  }

  private static Field fieldWithInstanceOffset(Class<?> type, long offset) {
    return Arrays.stream(type.getDeclaredFields())
      .filter(field -> !Modifier.isStatic(field.getModifiers()))
      .filter(field -> instanceFieldOffsetOf(field) == offset)
      .findFirst()
      .orElseThrow();
  }

  // defines a class that declares more fields than Class to produce colliding instance and static offsets, returns an
  // instance of it. the static field is set to -1, the instance fields are set to 100 + their index
  private static Object newInstanceWithManyFields(String name) throws ReflectiveOperationException {
    var fieldCount = FieldOffsetOps.CLASS_FIELD_COUNT + 10;
    var classDesc = ClassDesc.of("eu.cloudnetservice.wrapper.test." + name);
    var classData = ClassFile.of().build(classDesc, classBuilder -> {
      classBuilder.withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL);
      classBuilder.withField("STATIC_FIELD", ConstantDescs.CD_int, ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC);
      for (var index = 0; index < fieldCount; index++) {
        classBuilder.withField("field" + index, ConstantDescs.CD_int, ClassFile.ACC_PUBLIC);
      }
      classBuilder.withMethod(
        ConstantDescs.INIT_NAME,
        ConstantDescs.MTD_void,
        ClassFile.ACC_PUBLIC,
        methodBuilder -> methodBuilder.withCode(codeBuilder -> codeBuilder
          .aload(0)
          .invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void)
          .return_()));
    });

    var classLoader = new ClassLoader(UnsafeReplacementDelegateTest.class.getClassLoader()) {
      Class<?> define(byte[] data) {
        return this.defineClass(null, data, 0, data.length);
      }
    };
    var type = classLoader.define(classData);

    type.getField("STATIC_FIELD").setInt(null, -1);
    var instance = type.getConstructor().newInstance();
    for (var index = 0; index < fieldCount; index++) {
      type.getField("field" + index).setInt(instance, 100 + index);
    }

    return instance;
  }

  private static Thread startConcurrently(int threadCount, Runnable task) {
    var threads = new Thread[threadCount];
    var startLatch = new CountDownLatch(1);
    for (var index = 0; index < threadCount; index++) {
      threads[index] = Thread.ofPlatform().start(() -> {
        try {
          startLatch.await();
        } catch (InterruptedException exception) {
          throw new IllegalStateException(exception);
        }
        task.run();
      });
    }

    startLatch.countDown();
    return Thread.ofPlatform().start(() -> {
      for (var thread : threads) {
        try {
          thread.join();
        } catch (InterruptedException exception) {
          throw new IllegalStateException(exception);
        }
      }
    });
  }

  // all non-synthetic fields declared in the given class and its superclasses, accessible for reflective verification
  private static List<Field> fieldHierarchyFields(Class<?> type) {
    var fields = new ArrayList<Field>();
    for (Class<?> current = type; current != Object.class; current = current.getSuperclass()) {
      for (var field : current.getDeclaredFields()) {
        if (!field.isSynthetic()) {
          field.setAccessible(true);
          fields.add(field);
        }
      }
    }
    return fields;
  }

  private static Field fieldNamed(Class<?> type, String name) throws NoSuchFieldException {
    var field = type.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  private static Object newFieldFixture(Class<?> type) throws ReflectiveOperationException {
    var constructor = type.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private static long fieldOffsetOf(Field field) {
    return Modifier.isStatic(field.getModifiers()) ? staticFieldOffsetOf(field) : instanceFieldOffsetOf(field);
  }

  // the base to access the given field with, the given instance is used for instance fields
  private static Object fieldBase(Field field, Object instance) {
    return Modifier.isStatic(field.getModifiers()) ? UnsafeReplacementDelegate.unsafeStaticFieldBase(field) : instance;
  }

  // the values of all fields in the hierarchy of the given type, read using reflection
  private static List<Object> fieldValues(Class<?> type, Object instance) throws IllegalAccessException {
    var values = new ArrayList<>();
    for (var field : fieldHierarchyFields(type)) {
      values.add(field.get(instance));
    }
    return values;
  }

  // all fields declared in Class, including the ones that are filtered from reflection
  private static Field[] unfilteredFieldsOfClass() throws Throwable {
    var getDeclaredFields = OpConstants.TRUSTED_LOOKUP.get().findVirtual(
      Class.class,
      "getDeclaredFields0",
      MethodType.methodType(Field[].class, boolean.class));
    return (Field[]) getDeclaredFields.invokeExact(Class.class, false);
  }

  // two distinct values of the given field type, both differ from the initial values of the field kind fixtures
  private static List<Object> fieldTestValues(Class<?> type) {
    return switch (ValueTypeKind.of(type)) {
      case BYTE -> List.of((byte) 0x12, (byte) -0x56);
      case SHORT -> List.of((short) 0x1234, (short) -0x5678);
      case CHAR -> List.of('c', (char) 0xFEDC);
      case INT -> List.of(0x12345678, -0x789ABCDE);
      case LONG -> List.of(0x123456789ABCDEFL, -0xFEDCBA987654321L);
      case FLOAT -> List.of(1.25F, -3.5F);
      case DOUBLE -> List.of(2.5D, -1.0E100D);
      case BOOL -> List.of(false, true);
      case REF -> List.of("first", new Object());
    };
  }

  private static Object fieldDefaultValue(Class<?> type) {
    return switch (ValueTypeKind.of(type)) {
      case BYTE -> (byte) 0;
      case SHORT -> (short) 0;
      case CHAR -> (char) 0;
      case INT -> 0;
      case LONG -> 0L;
      case FLOAT -> 0F;
      case DOUBLE -> 0D;
      case BOOL -> false;
      case REF -> null;
    };
  }

  // the put modes that are available for the given type, ordered puts only exist for int, long and Object
  private static int[] fieldPutModes(Class<?> type) {
    return switch (ValueTypeKind.of(type)) {
      case INT, LONG, REF -> new int[]{OpConstants.MODE_PLAIN, OpConstants.MODE_VOLATILE, OpConstants.MODE_RELEASE};
      default -> new int[]{OpConstants.MODE_PLAIN, OpConstants.MODE_VOLATILE};
    };
  }

  // reads a value of the given type using the plain or volatile getter of the delegate
  private static Object fieldGetValue(Class<?> type, Object base, long offset, boolean isVolatile) {
    return switch (ValueTypeKind.of(type)) {
      case BYTE -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetByteVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetByte(base, offset);
      case SHORT -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetShortVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetShort(base, offset);
      case CHAR -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetCharVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetChar(base, offset);
      case INT -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetIntVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetInt(base, offset);
      case LONG -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetLongVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetLong(base, offset);
      case FLOAT -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetFloatVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetFloat(base, offset);
      case DOUBLE -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetDoubleVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetDouble(base, offset);
      case BOOL -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetBooleanVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetBoolean(base, offset);
      case REF -> isVolatile
        ? UnsafeReplacementDelegate.unsafeGetObjectVolatile(base, offset)
        : UnsafeReplacementDelegate.unsafeGetObject(base, offset);
    };
  }

  // writes a value of the given type using the put method of the delegate for the given mode
  private static void fieldPutValue(Class<?> type, Object base, long offset, Object value, int mode) {
    var isVolatile = mode == OpConstants.MODE_VOLATILE;
    switch (ValueTypeKind.of(type)) {
      case BYTE -> {
        if (isVolatile) {
          UnsafeReplacementDelegate.unsafePutByteVolatile(base, offset, (byte) value);
        } else {
          UnsafeReplacementDelegate.unsafePutByte(base, offset, (byte) value);
        }
      }
      case SHORT -> {
        if (isVolatile) {
          UnsafeReplacementDelegate.unsafePutShortVolatile(base, offset, (short) value);
        } else {
          UnsafeReplacementDelegate.unsafePutShort(base, offset, (short) value);
        }
      }
      case CHAR -> {
        if (isVolatile) {
          UnsafeReplacementDelegate.unsafePutCharVolatile(base, offset, (char) value);
        } else {
          UnsafeReplacementDelegate.unsafePutChar(base, offset, (char) value);
        }
      }
      case FLOAT -> {
        if (isVolatile) {
          UnsafeReplacementDelegate.unsafePutFloatVolatile(base, offset, (float) value);
        } else {
          UnsafeReplacementDelegate.unsafePutFloat(base, offset, (float) value);
        }
      }
      case DOUBLE -> {
        if (isVolatile) {
          UnsafeReplacementDelegate.unsafePutDoubleVolatile(base, offset, (double) value);
        } else {
          UnsafeReplacementDelegate.unsafePutDouble(base, offset, (double) value);
        }
      }
      case BOOL -> {
        if (isVolatile) {
          UnsafeReplacementDelegate.unsafePutBooleanVolatile(base, offset, (boolean) value);
        } else {
          UnsafeReplacementDelegate.unsafePutBoolean(base, offset, (boolean) value);
        }
      }
      case INT -> {
        switch (mode) {
          case OpConstants.MODE_VOLATILE -> UnsafeReplacementDelegate.unsafePutIntVolatile(base, offset, (int) value);
          case OpConstants.MODE_RELEASE -> UnsafeReplacementDelegate.unsafePutOrderedInt(base, offset, (int) value);
          default -> UnsafeReplacementDelegate.unsafePutInt(base, offset, (int) value);
        }
      }
      case LONG -> {
        switch (mode) {
          case OpConstants.MODE_VOLATILE -> UnsafeReplacementDelegate.unsafePutLongVolatile(base, offset, (long) value);
          case OpConstants.MODE_RELEASE -> UnsafeReplacementDelegate.unsafePutOrderedLong(base, offset, (long) value);
          default -> UnsafeReplacementDelegate.unsafePutLong(base, offset, (long) value);
        }
      }
      case REF -> {
        switch (mode) {
          case OpConstants.MODE_VOLATILE -> UnsafeReplacementDelegate.unsafePutObjectVolatile(base, offset, value);
          case OpConstants.MODE_RELEASE -> UnsafeReplacementDelegate.unsafePutOrderedObject(base, offset, value);
          default -> UnsafeReplacementDelegate.unsafePutObject(base, offset, value);
        }
      }
    }
  }

  // asserts that plain reads at the given offset return the default value, plain writes are silently ignored and that
  // read-modify-write operations throw an exception
  private static void assertLenientInvalidFieldAccess(Object base, long offset) {
    List<Class<?>> types = List.of(
      byte.class, short.class, char.class, int.class, long.class, float.class, double.class, boolean.class, Object.class);
    for (var type : types) {
      Assertions.assertEquals(fieldDefaultValue(type), fieldGetValue(type, base, offset, false), () -> type + " @ " + offset);
      Assertions.assertEquals(fieldDefaultValue(type), fieldGetValue(type, base, offset, true), () -> type + " @ " + offset);
      for (var mode : fieldPutModes(type)) {
        var value = fieldTestValues(type).getFirst();
        Assertions.assertDoesNotThrow(() -> fieldPutValue(type, base, offset, value, mode), () -> type + " @ " + offset);
      }
    }

    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeCasInt(base, offset, 0, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeCasLong(base, offset, 0, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeCasObject(base, offset, null, null));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddInt(base, offset, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddLong(base, offset, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetInt(base, offset, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetLong(base, offset, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(base, offset, null));
  }

  @Test
  void testInvalidFieldOffsetIsNeverAValidOffset() {
    Assertions.assertEquals(-1, UnsafeReplacementDelegate.UNSAFE_INVALID_FIELD_OFFSET);
    Assertions.assertTrue(UnsafeReplacementDelegate.UNSAFE_INVALID_FIELD_OFFSET < FieldOffsetOps.FIELD_BASE_OFFSET);
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> FieldOffsetOps.fieldFromOffset(new FieldKinds(), UnsafeReplacementDelegate.UNSAFE_INVALID_FIELD_OFFSET));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> FieldOffsetOps.fieldFromOffset(FieldStaticKinds.class, UnsafeReplacementDelegate.UNSAFE_INVALID_FIELD_OFFSET));
  }

  @Test
  void testFieldOffsetsOfUnsupportedFieldsAreRejected() throws Exception {
    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeObjectFieldOffset(null));
    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeStaticFieldOffset(null));
    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeStaticFieldBase(null));

    // fields of hidden classes and records
    var hiddenClassData = ClassFile.of().build(
      ClassDesc.of(UnsafeReplacementDelegateTest.class.getPackageName() + ".FieldHiddenClass"),
      classBuilder -> classBuilder
        .withField("value", ConstantDescs.CD_int, ClassFile.ACC_PRIVATE)
        .withField("STATIC_VALUE", ConstantDescs.CD_int, ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC));
    var hiddenClass = MethodHandles.lookup().defineHiddenClass(hiddenClassData, false).lookupClass();
    Assertions.assertTrue(hiddenClass.isHidden());

    var unsupportedFields = new ArrayList<Field>();
    unsupportedFields.addAll(List.of(hiddenClass.getDeclaredFields()));
    unsupportedFields.addAll(List.of(FieldRecord.class.getDeclaredFields()));
    Assertions.assertEquals(4, unsupportedFields.size());
    for (var field : unsupportedFields) {
      Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> UnsafeReplacementDelegate.unsafeObjectFieldOffset(field));
      Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> UnsafeReplacementDelegate.unsafeStaticFieldOffset(field));
      Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> UnsafeReplacementDelegate.unsafeStaticFieldBase(field));
    }
  }

  @ParameterizedTest
  @ValueSource(classes = {
    FieldKinds.class, FieldFinalKinds.class, FieldStaticKinds.class, FieldStaticFinalKinds.class, PrimitiveFields.class})
  void testFieldOffsetsAreDenseAlignedAndStable(Class<?> type) {
    // the superclass of all given types is Object, so the field table only contains the declared fields
    var fields = type.getDeclaredFields();
    var indices = new HashSet<Long>();
    for (var field : fields) {
      var offset = instanceFieldOffsetOf(field);
      Assertions.assertEquals(offset, instanceFieldOffsetOf(field));
      Assertions.assertEquals(offset, staticFieldOffsetOf(field));
      Assertions.assertTrue(offset >= FieldOffsetOps.FIELD_BASE_OFFSET);
      Assertions.assertEquals(0, (offset - FieldOffsetOps.FIELD_BASE_OFFSET) % FieldOffsetOps.FIELD_OFFSET_SCALE);

      // static field offsets are located after the offsets of all fields in Class
      var index = (offset - FieldOffsetOps.FIELD_BASE_OFFSET) / FieldOffsetOps.FIELD_OFFSET_SCALE;
      if (Modifier.isStatic(field.getModifiers())) {
        Assertions.assertTrue(index >= FieldOffsetOps.CLASS_FIELD_COUNT);
        index -= FieldOffsetOps.CLASS_FIELD_COUNT;
      } else {
        Assertions.assertTrue(index < FieldOffsetOps.CLASS_FIELD_COUNT);
      }
      Assertions.assertTrue(indices.add(index));
    }

    for (var index = 0L; index < fields.length; index++) {
      Assertions.assertTrue(indices.contains(index));
    }
  }

  @Test
  void testFieldOffsetsOfSubclassesFollowTheSuperclassFields() throws Exception {
    var sharedOffset = instanceFieldOffsetOf(SharedFieldBase.class.getDeclaredField("sharedField"));
    var firstOffset = instanceFieldOffsetOf(FirstSharedFieldSubclass.class.getDeclaredField("firstField"));
    var secondOffset = instanceFieldOffsetOf(SecondSharedFieldSubclass.class.getDeclaredField("secondField"));
    Assertions.assertEquals(FieldOffsetOps.indexToOffset(0), sharedOffset);
    Assertions.assertEquals(FieldOffsetOps.indexToOffset(1), firstOffset);
    Assertions.assertEquals(firstOffset, secondOffset);

    // the same offset addresses a different field depending on the base
    var first = new FirstSharedFieldSubclass();
    var second = new SecondSharedFieldSubclass();
    first.firstField = 5L;
    second.secondField = "second";
    Assertions.assertEquals(5L, UnsafeReplacementDelegate.unsafeGetLong(first, firstOffset));
    Assertions.assertEquals("second", UnsafeReplacementDelegate.unsafeGetObject(second, firstOffset));
    Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObject(first, secondOffset));
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeGetLong(second, secondOffset));

    // offsets of subclass fields do not exist in instances of the superclass
    var base = new SharedFieldBase();
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeGetLong(base, firstOffset));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetLong(base, firstOffset, 1L));
  }

  @Test
  void testStaticFieldBaseIsTheDeclaringClass() throws Exception {
    for (var field : FieldStaticKinds.class.getDeclaredFields()) {
      Assertions.assertSame(FieldStaticKinds.class, UnsafeReplacementDelegate.unsafeStaticFieldBase(field));
    }
    Assertions.assertSame(
      Class.class,
      UnsafeReplacementDelegate.unsafeStaticFieldBase(Class.class.getDeclaredField("ANNOTATION")));

    // constants that are inherited from an interface are declared by the interface
    var inheritedField = FieldInterfaceImplementation.class.getField("VALUE");
    var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(inheritedField);
    Assertions.assertSame(InterfaceWithStaticField.class, base);
    Assertions.assertSame(
      InterfaceWithStaticField.VALUE,
      UnsafeReplacementDelegate.unsafeGetObject(base, staticFieldOffsetOf(inheritedField)));
  }

  @Test
  void testStaticPrimitiveFieldsOfInterfacesAreAccessible() throws Exception {
    var intField = FieldInterfaceWithPrimitiveConstants.class.getDeclaredField("INT_VALUE");
    var longField = FieldInterfaceWithPrimitiveConstants.class.getDeclaredField("LONG_VALUE");
    var booleanField = FieldInterfaceWithPrimitiveConstants.class.getDeclaredField("BOOLEAN_VALUE");
    var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(intField);
    Assertions.assertSame(FieldInterfaceWithPrimitiveConstants.class, base);

    Assertions.assertEquals(42, UnsafeReplacementDelegate.unsafeGetInt(base, staticFieldOffsetOf(intField)));
    Assertions.assertEquals(-42L, UnsafeReplacementDelegate.unsafeGetLongVolatile(base, staticFieldOffsetOf(longField)));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeGetBoolean(base, staticFieldOffsetOf(booleanField)));
  }

  @Test
  void testFieldsOfClassObjectsAreAccessible() throws Exception {
    var nameOffset = instanceFieldOffsetOf(Class.class.getDeclaredField("name"));
    var componentTypeOffset = instanceFieldOffsetOf(Class.class.getDeclaredField("componentType"));
    var redefinedCountOffset = instanceFieldOffsetOf(Class.class.getDeclaredField("classRedefinedCount"));

    // instance fields of Class
    Assertions.assertEquals(String.class.getName(), UnsafeReplacementDelegate.unsafeGetObject(String.class, nameOffset));
    Assertions.assertSame(int.class, UnsafeReplacementDelegate.unsafeGetObjectVolatile(int[].class, componentTypeOffset));
    Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObject(String.class, componentTypeOffset));
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetIntVolatile(FieldKinds.class, redefinedCountOffset));

    // writes into instance fields of Class, using a class that is not used anywhere else
    var type = newInstanceWithManyFields("ManyFieldsClassWrites").getClass();
    var name = type.getName();
    Assertions.assertSame(name, UnsafeReplacementDelegate.unsafeGetAndSetObject(type, nameOffset, "renamed"));
    Assertions.assertEquals("renamed", type.getName());
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(type, nameOffset, "renamed", name));
    Assertions.assertSame(name, type.getName());
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetAndAddInt(type, redefinedCountOffset, 2));
    Assertions.assertEquals(2, UnsafeReplacementDelegate.unsafeGetInt(type, redefinedCountOffset));
    UnsafeReplacementDelegate.unsafePutIntVolatile(type, redefinedCountOffset, 0);
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(type, redefinedCountOffset));

    // static fields of Class, the static base is an instance of Class as well
    var annotationField = Class.class.getDeclaredField("ANNOTATION");
    var serialVersionUidField = Class.class.getDeclaredField("serialVersionUID");
    var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(annotationField);
    Assertions.assertSame(Class.class, base);
    Assertions.assertEquals(0x00002000, UnsafeReplacementDelegate.unsafeGetIntVolatile(base, staticFieldOffsetOf(annotationField)));
    Assertions.assertEquals(
      3206093459760846163L,
      UnsafeReplacementDelegate.unsafeGetLong(base, staticFieldOffsetOf(serialVersionUidField)));
  }

  @Test
  void testInstanceFieldOffsetWithStaticBaseAddressesFieldsOfClass() throws Throwable {
    // a class used as base is an instance of Class, instance offsets are resolved against the fields of Class
    var classFields = unfilteredFieldsOfClass();
    for (var field : FieldKinds.class.getDeclaredFields()) {
      var offset = instanceFieldOffsetOf(field);
      var classField = Arrays.stream(classFields)
        .filter(candidate -> !Modifier.isStatic(candidate.getModifiers()))
        .filter(candidate -> instanceFieldOffsetOf(candidate) == offset)
        .findFirst();
      if (classField.isPresent()) {
        Assertions.assertEquals(classField.get(), FieldOffsetOps.fieldFromOffset(FieldKinds.class, offset).wrappedField());
      } else {
        Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> FieldOffsetOps.fieldFromOffset(FieldKinds.class, offset));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(classes = {
    FieldKinds.class,
    FieldKindsSubclass.class,
    FieldFinalKinds.class,
    FieldStaticKinds.class,
    FieldStaticFinalKinds.class})
  void testFieldPutAndGetOfEveryKindInEveryMode(Class<?> fixture) throws Exception {
    var instance = newFieldFixture(fixture);
    var fields = fieldHierarchyFields(fixture);
    Assertions.assertEquals(9, fields.size());
    for (var field : fields) {
      var type = field.getType();
      var base = fieldBase(field, instance);
      var offset = fieldOffsetOf(field);
      var initial = field.get(instance);
      Assertions.assertEquals(initial, fieldGetValue(type, base, offset, false), field::toString);
      Assertions.assertEquals(initial, fieldGetValue(type, base, offset, true), field::toString);

      for (var mode : fieldPutModes(type)) {
        for (var value : fieldTestValues(type)) {
          fieldPutValue(type, base, offset, value, mode);
          Assertions.assertEquals(value, field.get(instance), () -> field + " in mode " + mode);
          Assertions.assertEquals(value, fieldGetValue(type, base, offset, false), () -> field + " in mode " + mode);
          Assertions.assertEquals(value, fieldGetValue(type, base, offset, true), () -> field + " in mode " + mode);
        }
      }

      // restore the initial value as static fields are shared between tests
      fieldPutValue(type, base, offset, initial, OpConstants.MODE_PLAIN);
      Assertions.assertEquals(initial, field.get(instance), field::toString);
    }
  }

  @ParameterizedTest
  @ValueSource(classes = {
    FieldKinds.class,
    FieldKindsSubclass.class,
    FieldFinalKinds.class,
    FieldStaticKinds.class,
    FieldStaticFinalKinds.class})
  void testFieldAtomicOperationsOnEveryFieldVariant(Class<?> fixture) throws Exception {
    var instance = newFieldFixture(fixture);
    var fields = fieldHierarchyFields(fixture);
    Assertions.assertEquals(9, fields.size());
    for (var field : fields) {
      var base = fieldBase(field, instance);
      var offset = fieldOffsetOf(field);
      var type = field.getType();
      if (type == int.class) {
        var initial = field.getInt(instance);
        Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(base, offset, initial + 1, 0));
        Assertions.assertEquals(initial, field.getInt(instance));
        Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(base, offset, initial, initial + 1));
        Assertions.assertEquals(initial + 1, field.getInt(instance));
        Assertions.assertEquals(initial + 1, UnsafeReplacementDelegate.unsafeGetAndAddInt(base, offset, 10));
        Assertions.assertEquals(initial + 11, field.getInt(instance));
        Assertions.assertEquals(initial + 11, UnsafeReplacementDelegate.unsafeGetAndSetInt(base, offset, initial));
        Assertions.assertEquals(initial, field.getInt(instance));
      } else if (type == long.class) {
        var initial = field.getLong(instance);
        Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasLong(base, offset, initial + 1, 0));
        Assertions.assertEquals(initial, field.getLong(instance));
        Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(base, offset, initial, initial + 1));
        Assertions.assertEquals(initial + 1, field.getLong(instance));
        Assertions.assertEquals(initial + 1, UnsafeReplacementDelegate.unsafeGetAndAddLong(base, offset, 10));
        Assertions.assertEquals(initial + 11, field.getLong(instance));
        Assertions.assertEquals(initial + 11, UnsafeReplacementDelegate.unsafeGetAndSetLong(base, offset, initial));
        Assertions.assertEquals(initial, field.getLong(instance));
      } else if (type == Object.class) {
        var initial = field.get(instance);
        var other = new Object();
        Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasObject(base, offset, other, other));
        Assertions.assertSame(initial, field.get(instance));
        Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(base, offset, initial, other));
        Assertions.assertSame(other, field.get(instance));
        Assertions.assertSame(other, UnsafeReplacementDelegate.unsafeGetAndSetObject(base, offset, initial));
        Assertions.assertSame(initial, field.get(instance));
      }
    }
  }

  @Test
  void testFieldGetAndAddOverflowsWithinTheFieldWidth() throws Exception {
    for (var instance : List.of(new FieldKinds(), new FieldFinalKinds())) {
      var type = instance.getClass();
      var intField = fieldNamed(type, "intField");
      var intOffset = instanceFieldOffsetOf(intField);
      UnsafeReplacementDelegate.unsafePutInt(instance, intOffset, Integer.MAX_VALUE);
      Assertions.assertEquals(Integer.MAX_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, intOffset, 1));
      Assertions.assertEquals(Integer.MIN_VALUE, intField.getInt(instance));
      Assertions.assertEquals(Integer.MIN_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, intOffset, -1));
      Assertions.assertEquals(Integer.MAX_VALUE, intField.getInt(instance));

      var longField = fieldNamed(type, "longField");
      var longOffset = instanceFieldOffsetOf(longField);
      UnsafeReplacementDelegate.unsafePutLong(instance, longOffset, Long.MIN_VALUE);
      Assertions.assertEquals(Long.MIN_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddLong(instance, longOffset, -1));
      Assertions.assertEquals(Long.MAX_VALUE, longField.getLong(instance));
      Assertions.assertEquals(Long.MAX_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddLong(instance, longOffset, 1));
      Assertions.assertEquals(Long.MIN_VALUE, longField.getLong(instance));

      // narrower fields overflow within their own width
      var byteField = fieldNamed(type, "byteField");
      var byteOffset = instanceFieldOffsetOf(byteField);
      UnsafeReplacementDelegate.unsafePutByte(instance, byteOffset, Byte.MAX_VALUE);
      Assertions.assertEquals(Byte.MAX_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, byteOffset, 1));
      Assertions.assertEquals(Byte.MIN_VALUE, byteField.getByte(instance));

      var charField = fieldNamed(type, "charField");
      var charOffset = instanceFieldOffsetOf(charField);
      UnsafeReplacementDelegate.unsafePutChar(instance, charOffset, Character.MAX_VALUE);
      Assertions.assertEquals(
        (int) Character.MAX_VALUE,
        UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, charOffset, 1));
      Assertions.assertEquals(Character.MIN_VALUE, charField.getChar(instance));
    }
  }

  @Test
  void testFieldCompareAndSwapObjectComparesIdentity() throws Exception {
    for (var instance : List.of(new FieldKinds(), new FieldFinalKinds())) {
      var field = fieldNamed(instance.getClass(), "objectField");
      var offset = instanceFieldOffsetOf(field);
      var value = new String("value");
      var equalValue = new String("value");

      UnsafeReplacementDelegate.unsafePutObject(instance, offset, value);
      Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasObject(instance, offset, equalValue, "other"));
      Assertions.assertSame(value, field.get(instance));
      Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(instance, offset, value, equalValue));
      Assertions.assertSame(equalValue, field.get(instance));
      Assertions.assertSame(equalValue, UnsafeReplacementDelegate.unsafeGetAndSetObject(instance, offset, null));
      Assertions.assertNull(field.get(instance));
      Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(instance, offset, null, value));
      Assertions.assertSame(value, field.get(instance));
    }
  }

  @Test
  void testConcurrentFieldGetAndAddIsAtomic() throws Exception {
    var iterations = 25_000;
    var threadCount = 4;
    var plain = new FieldKinds();
    var narrow = new FieldKinds();
    var finals = new FieldFinalKinds();
    var intOffset = instanceFieldOffsetOf(fieldNamed(FieldKinds.class, "intField"));
    var longOffset = instanceFieldOffsetOf(fieldNamed(FieldKinds.class, "longField"));
    var finalIntOffset = instanceFieldOffsetOf(fieldNamed(FieldFinalKinds.class, "intField"));
    var finalLongOffset = instanceFieldOffsetOf(fieldNamed(FieldFinalKinds.class, "longField"));
    narrow.longField = LITTLE_ENDIAN ? 0x7FFFFFFFFFFFFFF0L : 0xFFFFFFF07FFFFFFFL;

    startConcurrently(threadCount, () -> {
      for (var index = 0; index < iterations; index++) {
        UnsafeReplacementDelegate.unsafeGetAndAddInt(plain, intOffset, 1);
        UnsafeReplacementDelegate.unsafeGetAndAddLong(plain, longOffset, 1);
        UnsafeReplacementDelegate.unsafeGetAndAddInt(finals, finalIntOffset, 1);
        UnsafeReplacementDelegate.unsafeGetAndAddLong(finals, finalLongOffset, 1);
        UnsafeReplacementDelegate.unsafeGetAndAddInt(narrow, longOffset, 1);
      }
    }).join();

    var total = iterations * threadCount;
    Assertions.assertEquals(7 + total, plain.intField);
    Assertions.assertEquals(7L + total, plain.longField);
    Assertions.assertEquals(7 + total, finals.intField);
    Assertions.assertEquals(7L + total, finals.longField);
    // the addition on the first bytes in memory order does not carry into the other bytes
    var addedBits = (0xFFFFFFF0L + total) & 0xFFFFFFFFL;
    Assertions.assertEquals(
      LITTLE_ENDIAN ? 0x7FFFFFFF00000000L | addedBits : (addedBits << 32) | 0x7FFFFFFFL,
      narrow.longField);
  }

  @Test
  void testFieldReadsWithOtherKindConvertRawBits() throws Exception {
    var instance = new FieldKinds();
    var byteOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("byteField"));
    var charOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("charField"));
    var intOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("intField"));
    var longOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("longField"));
    var floatOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("floatField"));
    var booleanOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("booleanField"));

    // wider reads sign extend signed and zero extend unsigned field values
    instance.intField = -2;
    Assertions.assertEquals(-2L, UnsafeReplacementDelegate.unsafeGetLong(instance, intOffset));
    instance.byteField = -1;
    Assertions.assertEquals(-1, UnsafeReplacementDelegate.unsafeGetInt(instance, byteOffset));
    Assertions.assertEquals((char) 0xFFFF, UnsafeReplacementDelegate.unsafeGetChar(instance, byteOffset));
    instance.charField = (char) 0xFFFF;
    Assertions.assertEquals(0xFFFFL, UnsafeReplacementDelegate.unsafeGetLongVolatile(instance, charOffset));
    Assertions.assertEquals((short) -1, UnsafeReplacementDelegate.unsafeGetShort(instance, charOffset));
    instance.booleanField = true;
    Assertions.assertEquals(1, UnsafeReplacementDelegate.unsafeGetInt(instance, booleanOffset));
    Assertions.assertEquals(1L, UnsafeReplacementDelegate.unsafeGetLong(instance, booleanOffset));

    // narrower reads return the first bytes in native memory order
    instance.intField = 0x12345678;
    Assertions.assertEquals(
      (short) (LITTLE_ENDIAN ? 0x5678 : 0x1234), UnsafeReplacementDelegate.unsafeGetShort(instance, intOffset));
    Assertions.assertEquals(
      (char) (LITTLE_ENDIAN ? 0x5678 : 0x1234), UnsafeReplacementDelegate.unsafeGetCharVolatile(instance, intOffset));
    Assertions.assertEquals(
      (byte) (LITTLE_ENDIAN ? 0x78 : 0x12), UnsafeReplacementDelegate.unsafeGetByteVolatile(instance, intOffset));
    instance.intField = 0x100;
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeGetBoolean(instance, intOffset));
    instance.intField = 0x01000101;
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeGetBooleanVolatile(instance, intOffset));

    // floating point values are converted from and to their raw bits
    instance.intField = Float.floatToRawIntBits(1.5F);
    Assertions.assertEquals(1.5F, UnsafeReplacementDelegate.unsafeGetFloat(instance, intOffset));
    instance.longField = Double.doubleToRawLongBits(-2.5D);
    Assertions.assertEquals(-2.5D, UnsafeReplacementDelegate.unsafeGetDoubleVolatile(instance, longOffset));
    instance.floatField = 1.5F;
    Assertions.assertEquals(Float.floatToRawIntBits(1.5F), UnsafeReplacementDelegate.unsafeGetIntVolatile(instance, floatOffset));
    Assertions.assertEquals(Float.floatToRawIntBits(1.5F), UnsafeReplacementDelegate.unsafeGetLong(instance, floatOffset));
  }

  @Test
  void testFieldWritesWithOtherKindStoreRawBits() throws Exception {
    var instance = new FieldKinds();
    var byteOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("byteField"));
    var shortOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("shortField"));
    var charOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("charField"));
    var intOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("intField"));
    var longOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("longField"));
    var floatOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("floatField"));

    // values of the same size are stored as is
    UnsafeReplacementDelegate.unsafePutFloat(instance, intOffset, 1.5F);
    Assertions.assertEquals(Float.floatToRawIntBits(1.5F), instance.intField);
    UnsafeReplacementDelegate.unsafePutIntVolatile(instance, floatOffset, Float.floatToRawIntBits(-2.5F));
    Assertions.assertEquals(-2.5F, instance.floatField);
    UnsafeReplacementDelegate.unsafePutDouble(instance, longOffset, 2.5D);
    Assertions.assertEquals(Double.doubleToRawLongBits(2.5D), instance.longField);
    UnsafeReplacementDelegate.unsafePutShort(instance, charOffset, (short) -1);
    Assertions.assertEquals((char) 0xFFFF, instance.charField);
    UnsafeReplacementDelegate.unsafePutCharVolatile(instance, shortOffset, (char) 0xFFFE);
    Assertions.assertEquals((short) -2, instance.shortField);
    UnsafeReplacementDelegate.unsafePutBoolean(instance, byteOffset, true);
    Assertions.assertEquals((byte) 1, instance.byteField);

    // wider values are truncated to the field width
    UnsafeReplacementDelegate.unsafePutLong(instance, byteOffset, 0x123456789ABCDEF0L);
    Assertions.assertEquals((byte) 0xF0, instance.byteField);
    UnsafeReplacementDelegate.unsafePutInt(instance, shortOffset, 0x12345678);
    Assertions.assertEquals((short) 0x5678, instance.shortField);
    UnsafeReplacementDelegate.unsafePutLongVolatile(instance, charOffset, -1L);
    Assertions.assertEquals((char) 0xFFFF, instance.charField);
    UnsafeReplacementDelegate.unsafePutOrderedLong(instance, intOffset, 0x100000002L);
    Assertions.assertEquals(2, instance.intField);

    // narrower values only replace the first bytes in native memory order
    instance.intField = 0x11223344;
    UnsafeReplacementDelegate.unsafePutShort(instance, intOffset, (short) 0x7777);
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x11227777 : 0x77773344, instance.intField);
    instance.longField = 0x1122334455667788L;
    UnsafeReplacementDelegate.unsafePutFloat(instance, longOffset, 1.5F);
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x112233443FC00000L : 0x3FC0000055667788L, instance.longField);
    instance.longField = 0x1122334455667788L;
    UnsafeReplacementDelegate.unsafePutBooleanVolatile(instance, longOffset, true);
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x1122334455667701L : 0x0122334455667788L, instance.longField);
  }

  @Test
  void testFieldAtomicOperationsWithOtherKind() throws Exception {
    var instance = new FieldKinds();
    var byteOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("byteField"));
    var shortOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("shortField"));
    var intOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("intField"));
    var floatOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("floatField"));
    var doubleOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("doubleField"));
    var booleanOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("booleanField"));

    // int operations on narrower fields, the bits that do not fit into the field are ignored
    instance.byteField = 0x12;
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(instance, byteOffset, 0x13, 0));
    Assertions.assertEquals((byte) 0x12, instance.byteField);
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(instance, byteOffset, 0x7712, 0x7713));
    Assertions.assertEquals((byte) 0x13, instance.byteField);
    instance.shortField = -2;
    Assertions.assertEquals(-2, UnsafeReplacementDelegate.unsafeGetAndSetInt(instance, shortOffset, 0x12345));
    Assertions.assertEquals((short) 0x2345, instance.shortField);

    // long operations on int fields
    instance.intField = Integer.MAX_VALUE;
    Assertions.assertEquals(Integer.MAX_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddLong(instance, intOffset, 1));
    Assertions.assertEquals(Integer.MIN_VALUE, instance.intField);
    Assertions.assertEquals(Integer.MIN_VALUE, UnsafeReplacementDelegate.unsafeGetAndSetLong(instance, intOffset, -1L));
    Assertions.assertEquals(-1, instance.intField);

    // boolean fields support compare-and-set and get-and-set, but no additions
    instance.booleanField = true;
    Assertions.assertEquals(1, UnsafeReplacementDelegate.unsafeGetAndSetInt(instance, booleanOffset, 0));
    Assertions.assertFalse(instance.booleanField);
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(instance, booleanOffset, 0, 1));
    Assertions.assertTrue(instance.booleanField);
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, booleanOffset, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddLong(instance, booleanOffset, 1));
    Assertions.assertTrue(instance.booleanField);

    // floating point fields are compared by their raw bits
    instance.floatField = 0.0F;
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(instance, floatOffset, Float.floatToRawIntBits(-0.0F), 1));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(
      instance,
      floatOffset,
      Float.floatToRawIntBits(0.0F),
      Float.floatToRawIntBits(1.5F)));
    Assertions.assertEquals(1.5F, instance.floatField);
    instance.doubleField = 2.5D;
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(
      instance,
      doubleOffset,
      Double.doubleToRawLongBits(2.5D),
      Double.doubleToRawLongBits(-1.0D)));
    Assertions.assertEquals(
      Double.doubleToRawLongBits(-1.0D),
      UnsafeReplacementDelegate.unsafeGetAndSetLong(instance, doubleOffset, Double.doubleToRawLongBits(4.0D)));
    Assertions.assertEquals(4.0D, instance.doubleField);
  }

  @Test
  void testFieldReferenceAndPrimitiveKindMismatch() throws Exception {
    var instance = new FieldKinds();
    var intOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("intField"));
    var longOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("longField"));
    var objectOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("objectField"));
    var object = instance.objectField;

    // reads return the default value
    Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObject(instance, intOffset));
    Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObjectVolatile(instance, longOffset));
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(instance, objectOffset));
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeGetLongVolatile(instance, objectOffset));
    Assertions.assertEquals(0D, UnsafeReplacementDelegate.unsafeGetDouble(instance, objectOffset));
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeGetBoolean(instance, objectOffset));

    // writes are ignored
    UnsafeReplacementDelegate.unsafePutObject(instance, intOffset, 1);
    UnsafeReplacementDelegate.unsafePutObjectVolatile(instance, longOffset, 1L);
    UnsafeReplacementDelegate.unsafePutOrderedObject(instance, intOffset, null);
    UnsafeReplacementDelegate.unsafePutInt(instance, objectOffset, 1);
    UnsafeReplacementDelegate.unsafePutOrderedLong(instance, objectOffset, 1L);
    UnsafeReplacementDelegate.unsafePutBooleanVolatile(instance, objectOffset, true);
    Assertions.assertEquals(7, instance.intField);
    Assertions.assertEquals(7L, instance.longField);
    Assertions.assertSame(object, instance.objectField);

    // read-modify-write operations throw
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeCasObject(instance, longOffset, 7L, 8L));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(instance, intOffset, 8));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeCasLong(instance, objectOffset, 0, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, objectOffset, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddLong(instance, objectOffset, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetInt(instance, objectOffset, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetLong(instance, objectOffset, 1));
    Assertions.assertEquals(7, instance.intField);
    Assertions.assertEquals(7L, instance.longField);
    Assertions.assertSame(object, instance.objectField);
  }

  @Test
  void testFieldWritesOfWrongReferenceTypeAreRejected() throws Exception {
    var instance = new FieldStringFields();
    var fields = fieldHierarchyFields(FieldStringFields.class);
    Assertions.assertEquals(2, fields.size());
    for (var field : fields) {
      var offset = instanceFieldOffsetOf(field);
      var current = field.get(instance);

      // plain writes are ignored, read-modify-write operations throw
      UnsafeReplacementDelegate.unsafePutObject(instance, offset, 1);
      UnsafeReplacementDelegate.unsafePutObjectVolatile(instance, offset, 2);
      UnsafeReplacementDelegate.unsafePutOrderedObject(instance, offset, 3);
      Assertions.assertSame(current, field.get(instance));
      Assertions.assertThrows(
        ClassCastException.class,
        () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(instance, offset, 4));
      Assertions.assertThrows(
        ClassCastException.class,
        () -> UnsafeReplacementDelegate.unsafeCasObject(instance, offset, current, 5));
      Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasObject(instance, offset, 6, "other"));
      Assertions.assertSame(current, field.get(instance));

      // values of the field type are accepted
      Assertions.assertSame(current, UnsafeReplacementDelegate.unsafeGetAndSetObject(instance, offset, "other"));
      Assertions.assertEquals("other", field.get(instance));
      UnsafeReplacementDelegate.unsafePutObject(instance, offset, null);
      Assertions.assertNull(field.get(instance));
    }
  }

  @Test
  void testFieldAccessWithInvalidOffsetIsLenient() throws Exception {
    var instance = new FieldKinds();
    var instanceValues = fieldValues(FieldKinds.class, instance);
    var staticValues = fieldValues(FieldStaticKinds.class, null);
    var intOffset = instanceFieldOffsetOf(FieldKinds.class.getDeclaredField("intField"));
    var staticIntOffset = staticFieldOffsetOf(FieldStaticKinds.class.getDeclaredField("intField"));

    var invalidOffsets = new long[]{
      UnsafeReplacementDelegate.UNSAFE_INVALID_FIELD_OFFSET,
      0L,
      -FieldOffsetOps.FIELD_OFFSET_SCALE,
      FieldOffsetOps.FIELD_BASE_OFFSET - FieldOffsetOps.FIELD_OFFSET_SCALE,
      FieldOffsetOps.indexToOffset(1L << 40),
      Long.MIN_VALUE,
      Long.MAX_VALUE,
    };
    for (var offset : invalidOffsets) {
      assertLenientInvalidFieldAccess(instance, offset);
      assertLenientInvalidFieldAccess(FieldStaticKinds.class, offset);
    }

    // misaligned offsets, offsets after the last field and static offsets used with an instance
    var invalidInstanceOffsets = new long[]{
      intOffset + 1,
      intOffset + 4,
      FieldOffsetOps.indexToOffset(FieldKinds.class.getDeclaredFields().length),
      staticIntOffset,
    };
    for (var offset : invalidInstanceOffsets) {
      assertLenientInvalidFieldAccess(instance, offset);
    }

    // misaligned offsets and offsets after the last static field
    var invalidStaticOffsets = new long[]{
      staticIntOffset + 1,
      staticIntOffset + 4,
      FieldOffsetOps.indexToOffset(FieldOffsetOps.CLASS_FIELD_COUNT + 1000),
    };
    for (var offset : invalidStaticOffsets) {
      assertLenientInvalidFieldAccess(FieldStaticKinds.class, offset);
    }

    Assertions.assertEquals(instanceValues, fieldValues(FieldKinds.class, instance));
    Assertions.assertEquals(staticValues, fieldValues(FieldStaticKinds.class, null));
  }

  @Test
  void testStaticAndInstanceAccessDoNotShareResolvedFields() throws Exception {
    // first static, then instance access
    {
      var instance = newInstanceWithManyFields("ManyFieldsStaticFirst");
      var type = instance.getClass();
      var staticFieldOffset = staticFieldOffsetOf(type.getField("STATIC_FIELD"));
      var collidingField = fieldWithInstanceOffset(type, staticFieldOffset);

      Assertions.assertEquals(-1, UnsafeReplacementDelegate.unsafeGetInt(type, staticFieldOffset));
      Assertions.assertEquals(
        collidingField.getInt(instance),
        UnsafeReplacementDelegate.unsafeGetInt(instance, staticFieldOffset));
    }

    // first instance, then static access
    {
      var instance = newInstanceWithManyFields("ManyFieldsInstanceFirst");
      var type = instance.getClass();
      var staticFieldOffset = staticFieldOffsetOf(type.getField("STATIC_FIELD"));
      var collidingField = fieldWithInstanceOffset(type, staticFieldOffset);

      Assertions.assertEquals(
        collidingField.getInt(instance),
        UnsafeReplacementDelegate.unsafeGetInt(instance, staticFieldOffset));
      Assertions.assertEquals(-1, UnsafeReplacementDelegate.unsafeGetInt(type, staticFieldOffset));
    }
  }

  @Test
  void testInstanceFieldsOfClassAreResolvedAfterInstanceAccess() throws Exception {
    var moduleOffset = instanceFieldOffsetOf(Class.class.getDeclaredField("module"));
    Assertions.assertTrue(moduleOffset < FieldOffsetOps.indexToOffset(FieldOffsetOps.CLASS_FIELD_COUNT));

    // access an instance field of the class with the same offset first
    var instance = newInstanceWithManyFields("ManyFieldsClassAccess");
    var type = instance.getClass();
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeGetInt(instance, moduleOffset));

    var module = UnsafeReplacementDelegate.unsafeGetObject(type, moduleOffset);
    Assertions.assertSame(type.getModule(), module);
  }

  @Test
  void testStaticFieldsOfInterfacesAreAccessible() throws Exception {
    var field = InterfaceWithStaticField.class.getDeclaredField("VALUE");
    var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
    var offset = staticFieldOffsetOf(field);

    Assertions.assertSame(InterfaceWithStaticField.VALUE, UnsafeReplacementDelegate.unsafeGetObject(base, offset));
    Assertions.assertSame(
      InterfaceWithStaticField.VALUE,
      UnsafeReplacementDelegate.unsafeGetObjectVolatile(base, offset));
  }

  @Test
  void testFieldsHiddenFromReflectionHaveOffsets() throws Throwable {
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var getDeclaredFields = lookup.findVirtual(
      Class.class,
      "getDeclaredFields0",
      MethodType.methodType(Field[].class, boolean.class));
    var classFields = (Field[]) getDeclaredFields.invokeExact(Class.class, false);
    var classLoaderField = Arrays.stream(classFields)
      .filter(field -> field.getName().equals("classLoader"))
      .findFirst()
      .orElseThrow();

    var offset = Assertions.assertDoesNotThrow(() -> instanceFieldOffsetOf(classLoaderField));
    var classLoader = UnsafeReplacementDelegate.unsafeGetObject(UnsafeReplacementDelegateTest.class, offset);
    Assertions.assertSame(UnsafeReplacementDelegateTest.class.getClassLoader(), classLoader);
  }

  @Test
  void testFieldAccessWithDifferentKindUsesRawBits() throws Exception {
    var instance = new PrimitiveFields();
    var doubleOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("doubleField"));
    var charOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("charField"));
    var intOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("intField"));
    var boolOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("boolField"));
    var floatOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("floatField"));

    // reads return the raw bits of the field value
    Assertions.assertEquals(
      Double.doubleToRawLongBits(1.5D),
      UnsafeReplacementDelegate.unsafeGetLong(instance, doubleOffset));
    Assertions.assertEquals(
      Float.floatToRawIntBits(2.5F),
      UnsafeReplacementDelegate.unsafeGetInt(instance, floatOffset));
    Assertions.assertEquals(0x1234, UnsafeReplacementDelegate.unsafeGetInt(instance, charOffset));
    Assertions.assertEquals((short) 0x1234, UnsafeReplacementDelegate.unsafeGetShort(instance, charOffset));
    Assertions.assertEquals(1, UnsafeReplacementDelegate.unsafeGetByte(instance, boolOffset));

    // writes store the raw bits, truncated to the field width
    UnsafeReplacementDelegate.unsafePutLong(instance, doubleOffset, Double.doubleToRawLongBits(-7.25D));
    Assertions.assertEquals(-7.25D, instance.doubleField);
    UnsafeReplacementDelegate.unsafePutShort(instance, charOffset, (short) 0x4321);
    Assertions.assertEquals('䌡', instance.charField);
    UnsafeReplacementDelegate.unsafePutChar(instance, intOffset, '噸');
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x5678 : 0x56780007, instance.intField);
    UnsafeReplacementDelegate.unsafePutByte(instance, boolOffset, (byte) 0);
    Assertions.assertFalse(instance.boolField);
    UnsafeReplacementDelegate.unsafePutByte(instance, boolOffset, (byte) 2);
    Assertions.assertTrue(instance.boolField);

    // atomic operations with a different kind
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(
      instance, intOffset, LITTLE_ENDIAN ? 0x5678 : 0x56780007, 42));
    Assertions.assertEquals(42, instance.intField);
    Assertions.assertEquals(
      Double.doubleToRawLongBits(-7.25D),
      UnsafeReplacementDelegate.unsafeGetAndSetLong(instance, doubleOffset, Double.doubleToRawLongBits(3D)));
    Assertions.assertEquals(3D, instance.doubleField);
  }

  @Test
  void testNarrowFieldWritesOnlyReplaceFirstBytesInNativeOrder() throws Exception {
    var instance = new PrimitiveFields();
    var longOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("longField"));
    var intOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("intField"));

    instance.longField = 0x1122334455667788L;
    UnsafeReplacementDelegate.unsafePutInt(instance, longOffset, 0x7F);
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x112233440000007FL : 0x0000007F55667788L, instance.longField);
    UnsafeReplacementDelegate.unsafePutByteVolatile(instance, longOffset, (byte) -1);
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x11223344000000FFL : 0xFF00007F55667788L, instance.longField);

    instance.intField = 0x11223344;
    UnsafeReplacementDelegate.unsafePutByte(instance, intOffset, (byte) 0x7F);
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x1122337F : 0x7F223344, instance.intField);

    // read-modify-write operations only operate on the first bytes in native memory order as well
    instance.longField = LITTLE_ENDIAN ? 0x00000001FFFFFFFFL : 0xFFFFFFFF00000001L;
    Assertions.assertEquals(-1, UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, longOffset, 1));
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x0000000100000000L : 0x0000000000000001L, instance.longField);
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(instance, longOffset, 0, 5));
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x0000000100000005L : 0x0000000500000001L, instance.longField);
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(instance, longOffset, 0, 7));
    Assertions.assertEquals(5, UnsafeReplacementDelegate.unsafeGetAndSetInt(instance, longOffset, 9));
    Assertions.assertEquals(LITTLE_ENDIAN ? 0x0000000100000009L : 0x0000000900000001L, instance.longField);
  }

  @Test
  void testFailingReadModifyWriteOperationsThrow() throws Exception {
    var instance = new PrimitiveFields();
    var invalidOffset = 1_000_000L;
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeCasInt(instance, invalidOffset, 0, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddLong(instance, invalidOffset, 1));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(instance, invalidOffset, null));

    // reference field accessed as int and vice versa
    var refOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("refField"));
    var intOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("intField"));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeCasInt(instance, refOffset, 0, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeCasObject(instance, intOffset, null, null));

    // reference value of wrong type
    Assertions.assertThrows(
      ClassCastException.class,
      () -> UnsafeReplacementDelegate.unsafeCasObject(instance, refOffset, instance.refField, 1));

    // plain gets and puts stay lenient
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(instance, invalidOffset));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafePutInt(instance, invalidOffset, 1));
  }

  @Test
  void testInvalidOffsetsAreRejected() throws Exception {
    var instance = new PrimitiveFields();
    var misalignedOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("intField")) + 4;
    var invalidOffsets = new long[]{-1L, 0L, misalignedOffset, 50_000_000L, Integer.MAX_VALUE, Long.MAX_VALUE};
    for (var offset : invalidOffsets) {
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> FieldOffsetOps.fieldFromOffset(instance, offset));
      Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(instance, offset));
    }

    // static offset that points to an instance field
    var intField = PrimitiveFields.class.getDeclaredField("intField");
    var instanceFieldOffset = instanceFieldOffsetOf(intField) + FieldOffsetOps.CLASS_FIELD_COUNT * FieldOffsetOps.FIELD_OFFSET_SCALE;
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> FieldOffsetOps.fieldFromOffset(PrimitiveFields.class, instanceFieldOffset));

    // instance offset that points to a static field
    var staticField = PrimitiveFields.class.getDeclaredField("STATIC_FIELD");
    var staticFieldIndex = staticFieldOffsetOf(staticField) - FieldOffsetOps.CLASS_FIELD_COUNT * FieldOffsetOps.FIELD_OFFSET_SCALE;
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> FieldOffsetOps.fieldFromOffset(instance, staticFieldIndex));
  }

  @Test
  void testGetAndAddOnFloatingPointFieldsAddsRawBits() throws Exception {
    var instance = new PrimitiveFields();
    var floatOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("floatField"));
    var doubleOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("doubleField"));

    Assertions.assertEquals(
      Float.floatToRawIntBits(2.5F),
      UnsafeReplacementDelegate.unsafeGetAndAddInt(instance, floatOffset, 1));
    Assertions.assertEquals(Float.intBitsToFloat(Float.floatToRawIntBits(2.5F) + 1), instance.floatField);
    Assertions.assertEquals(
      Double.doubleToRawLongBits(1.5D),
      UnsafeReplacementDelegate.unsafeGetAndAddLong(instance, doubleOffset, 1));
    Assertions.assertEquals(Double.longBitsToDouble(Double.doubleToRawLongBits(1.5D) + 1), instance.doubleField);

    // final fields use the emulated read-modify-write operations
    var finalInstance = new FinalFloatingPointFields();
    var finalFloatField = FinalFloatingPointFields.class.getDeclaredField("floatField");
    var finalFloatOffset = instanceFieldOffsetOf(finalFloatField);
    Assertions.assertEquals(
      Float.floatToRawIntBits(2.5F),
      UnsafeReplacementDelegate.unsafeGetAndAddInt(finalInstance, finalFloatOffset, 1));
    Assertions.assertEquals(
      Float.intBitsToFloat(Float.floatToRawIntBits(2.5F) + 1),
      UnsafeReplacementDelegate.unsafeGetFloat(finalInstance, finalFloatOffset));
  }

  @Test
  void testCompareAndSetWithExpectedValueOfOtherTypeFails() throws Exception {
    var instance = new PrimitiveFields();
    var refOffset = instanceFieldOffsetOf(PrimitiveFields.class.getDeclaredField("refField"));
    var current = instance.refField;

    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasObject(instance, refOffset, 1, "other"));
    Assertions.assertSame(current, instance.refField);
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(instance, refOffset, current, "other"));
    Assertions.assertEquals("other", instance.refField);
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(instance, refOffset, "other", null));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(instance, refOffset, null, current));
    Assertions.assertSame(current, instance.refField);
  }

  @Test
  void testInheritedFieldsShareAccessorsBetweenSubclasses() throws Exception {
    var offset = instanceFieldOffsetOf(SharedFieldBase.class.getDeclaredField("sharedField"));
    var first = new FirstSharedFieldSubclass();
    var second = new SecondSharedFieldSubclass();

    var baseAccessor = FieldOffsetOps.fieldFromOffset(new SharedFieldBase(), offset);
    Assertions.assertSame(baseAccessor, FieldOffsetOps.fieldFromOffset(first, offset));
    Assertions.assertSame(baseAccessor, FieldOffsetOps.fieldFromOffset(second, offset));

    UnsafeReplacementDelegate.unsafePutInt(first, offset, 1);
    UnsafeReplacementDelegate.unsafePutInt(second, offset, 2);
    Assertions.assertEquals(1, first.sharedField);
    Assertions.assertEquals(2, second.sharedField);

    // fields declared in the subclasses still have their own offsets
    var firstOffset = instanceFieldOffsetOf(FirstSharedFieldSubclass.class.getDeclaredField("firstField"));
    var secondOffset = instanceFieldOffsetOf(SecondSharedFieldSubclass.class.getDeclaredField("secondField"));
    UnsafeReplacementDelegate.unsafePutLong(first, firstOffset, 3L);
    UnsafeReplacementDelegate.unsafePutObject(second, secondOffset, "4");
    Assertions.assertEquals(3L, first.firstField);
    Assertions.assertEquals("4", second.secondField);
  }

  private interface InterfaceWithStaticField {

    Object VALUE = new Object();
  }

  private interface FieldInterfaceWithPrimitiveConstants {

    // initialized from non-constant expressions to prevent inlining
    int INT_VALUE = Integer.parseInt("42");
    long LONG_VALUE = Long.parseLong("-42");
    boolean BOOLEAN_VALUE = Boolean.parseBoolean("true");
  }

  private static final class FieldInterfaceImplementation implements InterfaceWithStaticField {

  }

  private record FieldRecord(int value) {

    private static int staticValue;
  }

  private static class SharedFieldBase {

    int sharedField;
  }

  private static final class FirstSharedFieldSubclass extends SharedFieldBase {

    private long firstField;
  }

  private static final class SecondSharedFieldSubclass extends SharedFieldBase {

    private Object secondField;
  }

  private static final class PrimitiveFields {

    private static int STATIC_FIELD = 1;

    private String refField = "ref";
    private double doubleField = 1.5D;
    private float floatField = 2.5F;
    private char charField = 'ሴ';
    private int intField = 7;
    private long longField = 8;
    private boolean boolField = true;
  }

  private static final class FinalFloatingPointFields {

    // initialized from a non-constant expression to prevent constant folding of reads
    private final float floatField = Float.parseFloat("2.5");
  }

  // the field kind fixtures declare one field of each kind, initialized to 7 (or true)
  private static class FieldKinds {

    byte byteField = 7;
    short shortField = 7;
    char charField = '7';
    int intField = 7;
    long longField = 7;
    float floatField = 7;
    double doubleField = 7;
    boolean booleanField = true;
    Object objectField = "7";
  }

  private static final class FieldKindsSubclass extends FieldKinds {

  }

  private static final class FieldFinalKinds {

    // initialized from non-constant expressions to prevent inlining
    final byte byteField = Byte.parseByte("7");
    final short shortField = Short.parseShort("7");
    final char charField = "7".charAt(0);
    final int intField = Integer.parseInt("7");
    final long longField = Long.parseLong("7");
    final float floatField = Float.parseFloat("7");
    final double doubleField = Double.parseDouble("7");
    final boolean booleanField = Boolean.parseBoolean("true");
    final Object objectField = String.valueOf(7);
  }

  private static final class FieldStaticKinds {

    static byte byteField = 7;
    static short shortField = 7;
    static char charField = '7';
    static int intField = 7;
    static long longField = 7;
    static float floatField = 7;
    static double doubleField = 7;
    static boolean booleanField = true;
    static Object objectField = "7";
  }

  private static final class FieldStaticFinalKinds {

    // initialized from non-constant expressions to prevent inlining
    static final byte BYTE_FIELD = Byte.parseByte("7");
    static final short SHORT_FIELD = Short.parseShort("7");
    static final char CHAR_FIELD = "7".charAt(0);
    static final int INT_FIELD = Integer.parseInt("7");
    static final long LONG_FIELD = Long.parseLong("7");
    static final float FLOAT_FIELD = Float.parseFloat("7");
    static final double DOUBLE_FIELD = Double.parseDouble("7");
    static final boolean BOOLEAN_FIELD = Boolean.parseBoolean("true");
    static final Object OBJECT_FIELD = String.valueOf(7);
  }

  private static final class FieldStringFields {

    private final String finalStringField;
    private String stringField = "string";

    FieldStringFields() {
      this.finalStringField = "final";
    }
  }
  //</editor-fold>

  @Test
  void testPutGetAddress() {
    var addressSize = UnsafeReplacementDelegate.unsafeAddressSize();
    Assertions.assertTrue(addressSize == 4 || addressSize == 8);

    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(addressSize);
    UnsafeReplacementDelegate.unsafePutAddress(address, address);
    Assertions.assertEquals(address, UnsafeReplacementDelegate.unsafeGetAddress(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);
  }

  @Test
  void testPutGetByte() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(1);
    UnsafeReplacementDelegate.unsafePutByte(address, (byte) 123);
    Assertions.assertEquals(123, UnsafeReplacementDelegate.unsafeGetByte(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);

    var arr = new byte[]{-123, -100, 0, 100, 123};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(arr.getClass());
    for (var index = 0; index < arr.length; index++) {
      var off = (long) index * (scale + offset);
      if (index % 2 == 0) {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetByte(arr, off));
      } else {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetByteVolatile(arr, off));
      }
    }

    UnsafeReplacementDelegate.unsafePutByte(arr, scale + offset, (byte) -45);
    Assertions.assertEquals(-45, arr[1]);
    UnsafeReplacementDelegate.unsafePutByteVolatile(arr, scale + offset, (byte) 45);
    Assertions.assertEquals(45, arr[1]);
  }

  @Test
  void testPutGetChar() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(2);
    UnsafeReplacementDelegate.unsafePutChar(address, '\u0001');
    Assertions.assertEquals('\u0001', UnsafeReplacementDelegate.unsafeGetChar(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);

    var arr = new char[]{'a', 'ü', '6', '?', '^'};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(arr.getClass());
    for (var index = 0; index < arr.length; index++) {
      var off = (long) index * (scale + offset);
      if (index % 2 == 0) {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetChar(arr, off));
      } else {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetCharVolatile(arr, off));
      }
    }

    UnsafeReplacementDelegate.unsafePutChar(arr, scale + offset, '%');
    Assertions.assertEquals('%', arr[1]);
    UnsafeReplacementDelegate.unsafePutCharVolatile(arr, scale + offset, '}');
    Assertions.assertEquals('}', arr[1]);
  }

  @Test
  void testPutGetBoolean() {
    var arr = new boolean[]{true, false, true, true, false};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(arr.getClass());
    for (var index = 0; index < arr.length; index++) {
      var off = (long) index * (scale + offset);
      if (index % 2 == 0) {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetBoolean(arr, off));
      } else {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetBooleanVolatile(arr, off));
      }
    }

    UnsafeReplacementDelegate.unsafePutBoolean(arr, scale + offset, true);
    Assertions.assertTrue(arr[1]);
    UnsafeReplacementDelegate.unsafePutBooleanVolatile(arr, scale + offset, false);
    Assertions.assertFalse(arr[1]);
  }

  @Test
  void testPutGetInt() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(4);
    UnsafeReplacementDelegate.unsafePutInt(address, 696969);
    Assertions.assertEquals(696969, UnsafeReplacementDelegate.unsafeGetInt(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);

    var arr = new int[]{-696969, -1337, 0, 69, 1337};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(arr.getClass());
    for (var index = 0; index < arr.length; index++) {
      var off = (long) index * (scale + offset);
      if (index % 2 == 0) {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetInt(arr, off));
      } else {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetIntVolatile(arr, off));
      }
    }

    UnsafeReplacementDelegate.unsafePutInt(arr, scale + offset, 69);
    Assertions.assertEquals(69, arr[1]);
    UnsafeReplacementDelegate.unsafePutIntVolatile(arr, scale + offset, -69);
    Assertions.assertEquals(-69L, arr[1]);
    UnsafeReplacementDelegate.unsafePutOrderedInt(arr, scale + offset, 1337);
    Assertions.assertEquals(1337, arr[1]);
  }

  @Test
  void testPutGetLong() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    UnsafeReplacementDelegate.unsafePutLong(address, 696969L);
    Assertions.assertEquals(696969L, UnsafeReplacementDelegate.unsafeGetLong(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);

    var arr = new long[]{-696969L, -1337L, 0L, 69L, 1337L};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(arr.getClass());
    for (var index = 0; index < arr.length; index++) {
      var off = (long) index * (scale + offset);
      if (index % 2 == 0) {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetLong(arr, off));
      } else {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetLongVolatile(arr, off));
      }
    }

    UnsafeReplacementDelegate.unsafePutLong(arr, scale + offset, 69L);
    Assertions.assertEquals(69L, arr[1]);
    UnsafeReplacementDelegate.unsafePutLongVolatile(arr, scale + offset, -69L);
    Assertions.assertEquals(-69L, arr[1]);
    UnsafeReplacementDelegate.unsafePutOrderedLong(arr, scale + offset, 1337L);
    Assertions.assertEquals(1337L, arr[1]);
  }

  @Test
  void testPutGetDouble() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    UnsafeReplacementDelegate.unsafePutDouble(address, 69.6969D);
    Assertions.assertEquals(69.6969D, UnsafeReplacementDelegate.unsafeGetDouble(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);

    var arr = new double[]{-69.6969D, -13.37D, 0.0D, 6.9D, 133.7D};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(arr.getClass());
    for (var index = 0; index < arr.length; index++) {
      var off = (long) index * (scale + offset);
      if (index % 2 == 0) {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetDouble(arr, off));
      } else {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetDoubleVolatile(arr, off));
      }
    }

    UnsafeReplacementDelegate.unsafePutDouble(arr, scale + offset, 6.9D);
    Assertions.assertEquals(6.9D, arr[1]);
    UnsafeReplacementDelegate.unsafePutDoubleVolatile(arr, scale + offset, -6.9D);
    Assertions.assertEquals(-6.9D, arr[1]);
  }

  @Test
  void testPutGetObject() {
    var arr = new Object[]{new Object(), null, StackWalker.getInstance(), ClassLoader.getSystemClassLoader()};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(arr.getClass());
    for (var index = 0; index < arr.length; index++) {
      var off = (long) index * (scale + offset);
      if (index % 2 == 0) {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetObject(arr, off));
      } else {
        Assertions.assertEquals(arr[index], UnsafeReplacementDelegate.unsafeGetObjectVolatile(arr, off));
      }
    }

    var theObject = new Object();
    UnsafeReplacementDelegate.unsafePutObject(arr, scale + offset, theObject);
    Assertions.assertEquals(theObject, arr[1]);
    UnsafeReplacementDelegate.unsafePutObjectVolatile(arr, scale + offset, new Object());
    Assertions.assertNotSame(theObject, arr[1]);
    UnsafeReplacementDelegate.unsafePutOrderedObject(arr, scale + offset, ClassLoader.getSystemClassLoader());
    Assertions.assertInstanceOf(ClassLoader.class, arr[1]);
  }

  @Test
  void testOffHeapMemAccessUnaligned() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(9);
    UnsafeReplacementDelegate.unsafePutLong(address + 1, 123456789987654321L);
    Assertions.assertEquals(123456789987654321L, UnsafeReplacementDelegate.unsafeGetLong(address + 1));
    Assertions.assertEquals(123456789987654321L, UnsafeReplacementDelegate.unsafeGetAndSetLong(null, address + 1, -5L));
    Assertions.assertEquals(-5L, UnsafeReplacementDelegate.unsafeGetAndAddLong(null, address + 1, 10));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(null, address + 1, 5L, 1337L));
    Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetLongVolatile(null, address + 1));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);
  }

  @Test
  void testPutIntIntoBooleanArray() {
    var arr = new boolean[4];
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    UnsafeReplacementDelegate.unsafePutInt(arr, 0, 0x01010001);
    Assertions.assertEquals(0x01010001, UnsafeReplacementDelegate.unsafeGetInt(arr, 0));
    for (var index = 0L; index < 4; index++) {
      var val = UnsafeReplacementDelegate.unsafeGetBoolean(arr, scale * index);
      Assertions.assertEquals(index != 1, val);
    }
  }

  @Test
  void testPutLongIntoShortArray() {
    var arr = new short[8];
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    UnsafeReplacementDelegate.unsafePutOrderedLong(arr, scale, 123456789987654321L);
    Assertions.assertEquals(123456789987654321L, UnsafeReplacementDelegate.unsafeGetLong(arr, scale));
  }

  @Test
  void testPutValuesOfDifferentKindsInDifferentArrays() {
    // each array must be big enough to fit a long from index 3
    var arrays = List.of(
      new byte[11], // 1
      new short[7], // 2
      new int[5], // 4
      new long[4], // 8
      new float[5], // 4
      new double[4], // 8
      new Object[4] // -
    );
    var values = new Object[]{
      ValueTypeKind.BYTE, (byte) 123,
      ValueTypeKind.SHORT, (short) 12345,
      ValueTypeKind.INT, 123456789,
      ValueTypeKind.LONG, 123456789987654321L,
      ValueTypeKind.FLOAT, 12345.6789F,
      ValueTypeKind.DOUBLE, 123456789.7654321D,
      ValueTypeKind.BOOL, true,
      ValueTypeKind.CHAR, 'C',
    };

    for (var array : arrays) {
      var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(array.getClass());
      var offset = UnsafeReplacementDelegate.unsafeArrayBaseOffset(array.getClass());
      Assertions.assertTrue(scale > 0);
      Assertions.assertEquals(0, offset);

      for (var index = 0; index < values.length; index += 2) {
        var value = values[index + 1];
        var kind = (ValueTypeKind) values[index];
        var arrayValueOffset = 3L * scale;
        switch (kind) {
          case BYTE -> {
            var val = (byte) value;
            UnsafeReplacementDelegate.unsafePutByte(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetByte(array, arrayValueOffset));
            if (array.getClass() != Object[].class) {
              Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetAndAddInt(array, arrayValueOffset, 2));
              Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(array, arrayValueOffset, val + 2, val - 5));
              Assertions.assertEquals(val - 5, UnsafeReplacementDelegate.unsafeGetByte(array, arrayValueOffset));
            }
          }
          case SHORT -> {
            var val = (short) value;
            UnsafeReplacementDelegate.unsafePutShort(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetShort(array, arrayValueOffset));
            if (array.getClass() != Object[].class) {
              Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetAndAddInt(array, arrayValueOffset, 2));
              Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(array, arrayValueOffset, val + 2, val - 5));
              Assertions.assertEquals(val - 5, UnsafeReplacementDelegate.unsafeGetShort(array, arrayValueOffset));
            }
          }
          case INT -> {
            var val = (int) value;
            UnsafeReplacementDelegate.unsafePutInt(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetInt(array, arrayValueOffset));
            if (array.getClass() != Object[].class) {
              Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetAndAddInt(array, arrayValueOffset, 2));
              Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(array, arrayValueOffset, val + 2, val - 5));
              Assertions.assertEquals(val - 5, UnsafeReplacementDelegate.unsafeGetInt(array, arrayValueOffset));
            }
          }
          case LONG -> {
            var val = (long) value;
            UnsafeReplacementDelegate.unsafePutLong(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetLong(array, arrayValueOffset));
            if (array.getClass() != Object[].class) {
              Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetAndAddLong(array, arrayValueOffset, 2));
              Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(array, arrayValueOffset, val + 2, val - 5));
              Assertions.assertEquals(val - 5, UnsafeReplacementDelegate.unsafeGetLong(array, arrayValueOffset));
            }
          }
          case FLOAT -> {
            var val = (float) value;
            UnsafeReplacementDelegate.unsafePutFloat(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetFloat(array, arrayValueOffset));
          }
          case DOUBLE -> {
            var val = (double) value;
            UnsafeReplacementDelegate.unsafePutDouble(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetDouble(array, arrayValueOffset));
          }
          case BOOL -> {
            var val = (boolean) value;
            UnsafeReplacementDelegate.unsafePutBoolean(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetBoolean(array, arrayValueOffset));
          }
          case CHAR -> {
            var val = (char) value;
            UnsafeReplacementDelegate.unsafePutChar(array, arrayValueOffset, val);
            Assertions.assertEquals(val, UnsafeReplacementDelegate.unsafeGetChar(array, arrayValueOffset));
          }
        }
      }
    }
  }

  @Test
  void testCompareAndSwapInt() {
    var arr = new int[]{696969, 1337};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(arr, 0, 696969, 127));
    Assertions.assertEquals(127, arr[0]);
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(arr, scale, 696969, 127));
    Assertions.assertEquals(1337, arr[1]);

    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(4);
    UnsafeReplacementDelegate.unsafePutInt(address, 696969);
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(null, address, 696969, 127));
    Assertions.assertEquals(127, UnsafeReplacementDelegate.unsafeGetInt(address));
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(null, address, 696969, 127));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);
  }

  @Test
  void testCompareAndSwapObject() {
    var someObject = new Object();
    var arr = new Object[]{null, someObject};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(arr, 0, null, new Object()));
    Assertions.assertNotNull(arr[0]);
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(arr, scale, someObject, null));
    Assertions.assertNull(arr[1]);
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasObject(arr, scale, someObject, new Object()));
    Assertions.assertNull(arr[1]);
  }

  @Test
  void testGetAndAddLong() {
    var arr = new long[]{696969L, 1337L};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    Assertions.assertEquals(696969L, UnsafeReplacementDelegate.unsafeGetAndAddLong(arr, 0, 6));
    Assertions.assertEquals(696975L, arr[0]);
    Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetAndAddLong(arr, scale, 3));
    Assertions.assertEquals(1340L, arr[1]);
    Assertions.assertEquals(1340L, UnsafeReplacementDelegate.unsafeGetAndAddLong(arr, scale, -3));
    Assertions.assertEquals(1337L, arr[1]);

    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    UnsafeReplacementDelegate.unsafePutLong(address, 1337L);
    Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetAndAddLong(null, address, 3));
    Assertions.assertEquals(1340L, UnsafeReplacementDelegate.unsafeGetLong(address));
    Assertions.assertEquals(1340L, UnsafeReplacementDelegate.unsafeGetAndAddLong(null, address, -3));
    Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetLong(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);
  }

  @Test
  void testGetAndSetInt() {
    var arr = new int[]{696969, 1337};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    Assertions.assertEquals(696969, UnsafeReplacementDelegate.unsafeGetAndSetInt(arr, 0, 69));
    Assertions.assertEquals(69, arr[0]);
    Assertions.assertEquals(1337, UnsafeReplacementDelegate.unsafeGetAndSetInt(arr, scale, 127));
    Assertions.assertEquals(127, arr[1]);
    Assertions.assertEquals(127, UnsafeReplacementDelegate.unsafeGetAndSetInt(arr, scale, 69));
    Assertions.assertEquals(69, arr[1]);

    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(4);
    UnsafeReplacementDelegate.unsafePutInt(address, 696969);
    Assertions.assertEquals(696969, UnsafeReplacementDelegate.unsafeGetAndSetInt(null, address, 127));
    Assertions.assertEquals(127, UnsafeReplacementDelegate.unsafeGetInt(address));
    Assertions.assertEquals(127, UnsafeReplacementDelegate.unsafeGetAndSetInt(null, address, 69));
    Assertions.assertEquals(69, UnsafeReplacementDelegate.unsafeGetInt(address));
    UnsafeReplacementDelegate.unsafeFreeMemory(address);
  }

  @Test
  void testGetAndSetObject() {
    var someObject = new Object();
    var arr = new Object[]{new Object(), null, StackWalker.getInstance()};
    var scale = UnsafeReplacementDelegate.unsafeArrayIndexScale(arr.getClass());
    Assertions.assertSame(arr[0], UnsafeReplacementDelegate.unsafeGetAndSetObject(arr, 0, new Object()));
    Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetAndSetObject(arr, scale, someObject));
    Assertions.assertSame(someObject, arr[1]);
    Assertions.assertSame(arr[2], UnsafeReplacementDelegate.unsafeGetAndSetObject(arr, 2L * scale, null));
    Assertions.assertNull(arr[2]);
  }

  // counts the plain long writes into the given array at the given offset that are lost while another thread
  // concurrently does get-and-add operations on the same bytes
  private static int countLostPlainWrites(Object array, long offset) throws InterruptedException {
    var stop = new AtomicBoolean();
    var adder = Thread.ofPlatform().start(() -> {
      while (!stop.get()) {
        UnsafeReplacementDelegate.unsafeGetAndAddLong(array, offset, 1);
      }
    });

    try {
      var lostWrites = 0;
      for (var index = 0; index < 50_000; index++) {
        var sentinel = (long) index << 32;
        UnsafeReplacementDelegate.unsafePutLong(array, offset, sentinel);
        if (UnsafeReplacementDelegate.unsafeGetLongVolatile(array, offset) < sentinel) {
          lostWrites++;
        }
      }
      return lostWrites;
    } finally {
      stop.set(true);
      adder.join();
    }
  }

  // creates fresh primitive arrays (all types except boolean) that each span the given number of bytes, the order of
  // the returned arrays is byte, short, char, int, long, float, double
  private static List<Object> newPrimitiveMemoryArrays(int byteSize) {
    return List.of(
      new byte[byteSize],
      new short[byteSize / 2],
      new char[byteSize / 2],
      new int[byteSize / 4],
      new long[byteSize / 8],
      new float[byteSize / 4],
      new double[byteSize / 8]);
  }

  // wraps the given primitive (non-boolean) array into a memory segment, used as an oracle for the raw array bytes
  private static MemorySegment memorySegmentOfArray(Object array) {
    return switch (array) {
      case byte[] values -> MemorySegment.ofArray(values);
      case short[] values -> MemorySegment.ofArray(values);
      case char[] values -> MemorySegment.ofArray(values);
      case int[] values -> MemorySegment.ofArray(values);
      case long[] values -> MemorySegment.ofArray(values);
      case float[] values -> MemorySegment.ofArray(values);
      case double[] values -> MemorySegment.ofArray(values);
      default -> throw new IllegalArgumentException("unsupported array type: " + array.getClass());
    };
  }

  private static byte[] memoryBytesOfArray(Object array) {
    return memorySegmentOfArray(array).toArray(ValueLayout.JAVA_BYTE);
  }

  // wraps the given off-heap memory region into a memory segment, used as an oracle for the raw memory bytes
  @SuppressWarnings("restricted")
  private static MemorySegment offHeapMemorySegment(long address, long byteSize) {
    return MemorySegment.ofAddress(address).reinterpret(byteSize);
  }

  // creates a byte buffer in native byte order that contains the given value at the given offset, zero elsewhere
  private static byte[] expectedMemoryBytes(int byteSize, int offset, Object value) {
    var buffer = ByteBuffer.allocate(byteSize).order(ByteOrder.nativeOrder());
    switch (value) {
      case Byte b -> buffer.put(offset, b);
      case Short s -> buffer.putShort(offset, s);
      case Character c -> buffer.putChar(offset, c);
      case Integer i -> buffer.putInt(offset, i);
      case Long l -> buffer.putLong(offset, l);
      case Float f -> buffer.putFloat(offset, f);
      case Double d -> buffer.putDouble(offset, d);
      case Boolean b -> buffer.put(offset, (byte) (b ? 1 : 0));
      default -> throw new IllegalArgumentException("unsupported value: " + value);
    }
    return buffer.array();
  }

  // all primitive value kinds with a test value and their accessors in the delegate
  private static List<MemoryAccessKind> memoryAccessKinds() {
    return List.of(
      new MemoryAccessKind(
        "byte",
        1,
        (byte) 0xA5,
        byte[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetByte, UnsafeReplacementDelegate::unsafeGetByteVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutByte(base, offset, (byte) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutByteVolatile(base, offset, (byte) value)),
        List.of(UnsafeReplacementDelegate::unsafeGetByte),
        List.of((address, value) -> UnsafeReplacementDelegate.unsafePutByte(address, (byte) value))),
      new MemoryAccessKind(
        "short",
        2,
        (short) 0xA1B2,
        short[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetShort, UnsafeReplacementDelegate::unsafeGetShortVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutShort(base, offset, (short) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutShortVolatile(base, offset, (short) value)),
        List.of(UnsafeReplacementDelegate::unsafeGetShort),
        List.of((address, value) -> UnsafeReplacementDelegate.unsafePutShort(address, (short) value))),
      new MemoryAccessKind(
        "char",
        2,
        (char) 0xC3D4,
        char[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetChar, UnsafeReplacementDelegate::unsafeGetCharVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutChar(base, offset, (char) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutCharVolatile(base, offset, (char) value)),
        List.of(UnsafeReplacementDelegate::unsafeGetChar),
        List.of((address, value) -> UnsafeReplacementDelegate.unsafePutChar(address, (char) value))),
      new MemoryAccessKind(
        "int",
        4,
        0x89ABCDEF,
        int[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetInt, UnsafeReplacementDelegate::unsafeGetIntVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutInt(base, offset, (int) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutIntVolatile(base, offset, (int) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutOrderedInt(base, offset, (int) value)),
        List.of(UnsafeReplacementDelegate::unsafeGetInt),
        List.of((address, value) -> UnsafeReplacementDelegate.unsafePutInt(address, (int) value))),
      new MemoryAccessKind(
        "long",
        8,
        0xFEDCBA9876543210L,
        long[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetLong, UnsafeReplacementDelegate::unsafeGetLongVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutLong(base, offset, (long) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutLongVolatile(base, offset, (long) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutOrderedLong(base, offset, (long) value)),
        List.of(UnsafeReplacementDelegate::unsafeGetLong),
        List.of((address, value) -> UnsafeReplacementDelegate.unsafePutLong(address, (long) value))),
      new MemoryAccessKind(
        "float",
        4,
        -12345.678F,
        float[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetFloat, UnsafeReplacementDelegate::unsafeGetFloatVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutFloat(base, offset, (float) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutFloatVolatile(base, offset, (float) value)),
        List.of(UnsafeReplacementDelegate::unsafeGetFloat),
        List.of((address, value) -> UnsafeReplacementDelegate.unsafePutFloat(address, (float) value))),
      new MemoryAccessKind(
        "double",
        8,
        -9876.54321E123D,
        double[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetDouble, UnsafeReplacementDelegate::unsafeGetDoubleVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutDouble(base, offset, (double) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutDoubleVolatile(base, offset, (double) value)),
        List.of(UnsafeReplacementDelegate::unsafeGetDouble),
        List.of((address, value) -> UnsafeReplacementDelegate.unsafePutDouble(address, (double) value))),
      new MemoryAccessKind(
        "boolean",
        1,
        true,
        boolean[]::new,
        List.of(UnsafeReplacementDelegate::unsafeGetBoolean, UnsafeReplacementDelegate::unsafeGetBooleanVolatile),
        List.of(
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutBoolean(base, offset, (boolean) value),
          (base, offset, value) -> UnsafeReplacementDelegate.unsafePutBooleanVolatile(base, offset, (boolean) value)),
        List.of(),
        List.of()));
  }

  // runs compare-and-swap, get-and-add (with overflow) and get-and-set int operations at the given location which must
  // initially hold 0, the location holds 0x89ABCDEF afterward
  private static void assertMemoryIntAtomics(Object base, long offset, String context) {
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(base, offset, 0, Integer.MAX_VALUE), context);
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(base, offset, 0, 1), context);
    Assertions.assertEquals(Integer.MAX_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddInt(base, offset, 1), context);
    Assertions.assertEquals(Integer.MIN_VALUE, UnsafeReplacementDelegate.unsafeGetIntVolatile(base, offset), context);
    Assertions.assertEquals(
      Integer.MIN_VALUE,
      UnsafeReplacementDelegate.unsafeGetAndSetInt(base, offset, 0x89ABCDEF),
      context);
    Assertions.assertEquals(0x89ABCDEF, UnsafeReplacementDelegate.unsafeGetInt(base, offset), context);
  }

  // runs compare-and-swap, get-and-add (with overflow) and get-and-set long operations at the given location which must
  // initially hold 0, the location holds 0xFEDCBA9876543210 afterward
  private static void assertMemoryLongAtomics(Object base, long offset, String context) {
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(base, offset, 0L, Long.MAX_VALUE), context);
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasLong(base, offset, 0L, 1L), context);
    Assertions.assertEquals(Long.MAX_VALUE, UnsafeReplacementDelegate.unsafeGetAndAddLong(base, offset, 1L), context);
    Assertions.assertEquals(Long.MIN_VALUE, UnsafeReplacementDelegate.unsafeGetLongVolatile(base, offset), context);
    Assertions.assertEquals(
      Long.MIN_VALUE,
      UnsafeReplacementDelegate.unsafeGetAndSetLong(base, offset, 0xFEDCBA9876543210L),
      context);
    Assertions.assertEquals(0xFEDCBA9876543210L, UnsafeReplacementDelegate.unsafeGetLong(base, offset), context);
  }

  @Test
  void testArrayAndAddressSizeConstants() {
    Assertions.assertEquals(ValueLayout.ADDRESS.byteSize(), UnsafeReplacementDelegate.unsafeAddressSize());
    Assertions.assertEquals(UnsafeReplacementDelegate.unsafeAddressSize(), UnsafeReplacementDelegate.UNSAFE_ADDRESS_SIZE);

    var arrayTypes = List.<Class<?>>of(
      boolean[].class,
      byte[].class,
      short[].class,
      char[].class,
      int[].class,
      long[].class,
      float[].class,
      double[].class,
      Object[].class);
    var expectedScales = new int[]{1, 1, 2, 2, 4, 8, 4, 8, 4};
    var scaleConstants = new int[]{
      UnsafeReplacementDelegate.UNSAFE_ARRAY_BOOLEAN_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_BYTE_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_SHORT_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_CHAR_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_INT_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_LONG_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_FLOAT_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_DOUBLE_INDEX_SCALE,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_OBJECT_INDEX_SCALE,
    };
    var baseOffsetConstants = new int[]{
      UnsafeReplacementDelegate.UNSAFE_ARRAY_BOOLEAN_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_BYTE_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_SHORT_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_CHAR_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_INT_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_LONG_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_FLOAT_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_DOUBLE_BASE_OFFSET,
      UnsafeReplacementDelegate.UNSAFE_ARRAY_OBJECT_BASE_OFFSET,
    };
    for (var index = 0; index < arrayTypes.size(); index++) {
      var arrayType = arrayTypes.get(index);
      Assertions.assertEquals(expectedScales[index], scaleConstants[index], arrayType.getName());
      Assertions.assertEquals(expectedScales[index], UnsafeReplacementDelegate.unsafeArrayIndexScale(arrayType));
      Assertions.assertEquals(0, baseOffsetConstants[index], arrayType.getName());
      Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeArrayBaseOffset(arrayType));
    }

    // all other reference arrays (including multidimensional arrays) are treated like Object[]
    for (var arrayType : List.of(String[].class, Runnable[].class, int[][].class, Object[][].class, long[][][].class)) {
      Assertions.assertEquals(
        UnsafeReplacementDelegate.UNSAFE_ARRAY_OBJECT_INDEX_SCALE,
        UnsafeReplacementDelegate.unsafeArrayIndexScale(arrayType));
      Assertions.assertEquals(
        UnsafeReplacementDelegate.UNSAFE_ARRAY_OBJECT_BASE_OFFSET,
        UnsafeReplacementDelegate.unsafeArrayBaseOffset(arrayType));
    }

    // the index scale is only available for array types
    for (var type : List.of(Object.class, String.class, int.class, void.class, Runnable.class)) {
      var thrown = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeArrayIndexScale(type));
      Assertions.assertEquals(type.getName(), thrown.getMessage());
    }
    Assertions.assertThrows(NullPointerException.class, () -> UnsafeReplacementDelegate.unsafeArrayIndexScale(null));
  }

  @Test
  void testArrayAccessWithMatchingElementType() {
    for (var kind : memoryAccessKinds()) {
      var defaultValue = Array.get(kind.newArray().apply(1), 0);
      for (var index : new int[]{0, 2, 4}) {
        var offset = (long) index * kind.byteSize();
        for (var writer : kind.writers()) {
          var array = kind.newArray().apply(5);
          writer.write(array, offset, kind.value());
          for (var element = 0; element < 5; element++) {
            var expected = element == index ? kind.value() : defaultValue;
            Assertions.assertEquals(expected, Array.get(array, element), kind.name() + "[" + element + "]");
          }

          for (var reader : kind.readers()) {
            Assertions.assertEquals(kind.value(), reader.read(array, offset), kind.name() + "[" + index + "]");
          }
        }
      }
    }
  }

  @Test
  void testReferenceArrayAccess() {
    for (var array : List.<Object[]>of(new Object[5], new String[5], new CharSequence[5])) {
      for (var index : new int[]{0, 2, 4}) {
        var offset = index * 4L;
        var value = "value" + index;
        UnsafeReplacementDelegate.unsafePutObject(array, offset, value);
        Assertions.assertSame(value, array[index]);
        Assertions.assertSame(value, UnsafeReplacementDelegate.unsafeGetObject(array, offset));
        UnsafeReplacementDelegate.unsafePutObjectVolatile(array, offset, null);
        Assertions.assertNull(array[index]);
        Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObjectVolatile(array, offset));
        UnsafeReplacementDelegate.unsafePutOrderedObject(array, offset, value);
        Assertions.assertSame(value, array[index]);
        Assertions.assertSame(value, UnsafeReplacementDelegate.unsafeGetObjectVolatile(array, offset));
      }
      Assertions.assertArrayEquals(new Object[]{"value0", null, "value2", null, "value4"}, array);
    }

    // multidimensional arrays are reference arrays as well
    var matrix = new int[3][];
    var row = new int[]{1, 2};
    UnsafeReplacementDelegate.unsafePutObject(matrix, 8, row);
    Assertions.assertSame(row, matrix[2]);
    Assertions.assertSame(row, UnsafeReplacementDelegate.unsafeGetObject(matrix, 8));

    // writes of values that cannot be stored in a typed array are ignored
    var strings = new String[]{"a", "b"};
    UnsafeReplacementDelegate.unsafePutObject(strings, 0, 5);
    UnsafeReplacementDelegate.unsafePutObjectVolatile(strings, 4, new Object());
    UnsafeReplacementDelegate.unsafePutOrderedObject(strings, 0, new StringBuilder("c"));
    UnsafeReplacementDelegate.unsafePutObject(matrix, 0, new long[1]);
    Assertions.assertArrayEquals(new String[]{"a", "b"}, strings);
    Assertions.assertNull(matrix[0]);

    // atomic writes of values that cannot be stored in a typed array throw
    Assertions.assertThrows(
      ArrayStoreException.class,
      () -> UnsafeReplacementDelegate.unsafeCasObject(strings, 0, strings[0], 5));
    Assertions.assertThrows(
      ArrayStoreException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(matrix, 0, new long[1]));
    Assertions.assertArrayEquals(new String[]{"a", "b"}, strings);
    Assertions.assertNull(matrix[0]);

    // misaligned offsets into reference arrays are ignored by plain access, rejected by atomic access
    Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObject(strings, 2));
    Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObjectVolatile(strings, 5));
    UnsafeReplacementDelegate.unsafePutObject(strings, 2, "c");
    UnsafeReplacementDelegate.unsafePutObjectVolatile(strings, 1, "c");
    UnsafeReplacementDelegate.unsafePutOrderedObject(strings, 7, "c");
    Assertions.assertArrayEquals(new String[]{"a", "b"}, strings);
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeCasObject(strings, 2, "a", "c"));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(strings, 6, "c"));
    Assertions.assertArrayEquals(new String[]{"a", "b"}, strings);
  }

  @Test
  void testPrimitiveArrayAccessWithOtherValueTypes() {
    // aligned (0, 8, 24) and misaligned (1, 6, 13) offsets into arrays of every primitive type
    var offsets = new int[]{0, 8, 24, 1, 6, 13};
    for (var arrayIndex = 0; arrayIndex < 7; arrayIndex++) {
      for (var kind : memoryAccessKinds()) {
        for (var offset : offsets) {
          var expectedBytes = expectedMemoryBytes(32, offset, kind.value());
          for (var writer : kind.writers()) {
            var array = newPrimitiveMemoryArrays(32).get(arrayIndex);
            var context = kind.name() + " at offset " + offset + " in " + array.getClass().getSimpleName();
            writer.write(array, offset, kind.value());
            Assertions.assertArrayEquals(expectedBytes, memoryBytesOfArray(array), context);
            for (var reader : kind.readers()) {
              Assertions.assertEquals(kind.value(), reader.read(array, offset), context);
            }
          }
        }
      }
    }
  }

  @Test
  void testBooleanArrayAccessWithOtherValueTypesIsNormalized() {
    // the values used below have a symmetric byte pattern, the result is independent of the native byte order
    var bools = new boolean[]{true, false, false, true, true, true, false, false, true, false, false, false, false, false,
      false, true};
    Assertions.assertEquals(1, UnsafeReplacementDelegate.unsafeGetByte(bools, 0));
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetByteVolatile(bools, 1));
    Assertions.assertEquals(0x01000001, UnsafeReplacementDelegate.unsafeGetInt(bools, 0));
    Assertions.assertEquals(0x01000001, UnsafeReplacementDelegate.unsafeGetIntVolatile(bools, 0));
    Assertions.assertEquals((short) 0x0101, UnsafeReplacementDelegate.unsafeGetShort(bools, 4));
    Assertions.assertEquals((char) 0x0101, UnsafeReplacementDelegate.unsafeGetCharVolatile(bools, 4));
    Assertions.assertEquals(0x0100000000000001L, UnsafeReplacementDelegate.unsafeGetLong(bools, 8));
    Assertions.assertEquals(0x0100000000000001L, UnsafeReplacementDelegate.unsafeGetLongVolatile(bools, 8));
    Assertions.assertEquals(Float.intBitsToFloat(0x01000001), UnsafeReplacementDelegate.unsafeGetFloat(bools, 0));
    Assertions.assertEquals(
      Double.longBitsToDouble(0x0100000000000001L),
      UnsafeReplacementDelegate.unsafeGetDoubleVolatile(bools, 8));

    // writes store every byte as a boolean, so non-zero bytes are read back as 1
    var bytes = new boolean[6];
    UnsafeReplacementDelegate.unsafePutByte(bytes, 1, (byte) 0x40);
    Assertions.assertArrayEquals(new boolean[]{false, true, false, false, false, false}, bytes);
    Assertions.assertEquals(1, UnsafeReplacementDelegate.unsafeGetByte(bytes, 1));
    UnsafeReplacementDelegate.unsafePutByteVolatile(bytes, 1, (byte) 0);
    Assertions.assertArrayEquals(new boolean[6], bytes);

    var shorts = new boolean[6];
    UnsafeReplacementDelegate.unsafePutShort(shorts, 3, (short) 0x8003);
    Assertions.assertArrayEquals(new boolean[]{false, false, false, true, true, false}, shorts);
    Assertions.assertEquals((short) 0x0101, UnsafeReplacementDelegate.unsafeGetShort(shorts, 3));
    UnsafeReplacementDelegate.unsafePutCharVolatile(shorts, 2, (char) 0x0202);
    Assertions.assertArrayEquals(new boolean[]{false, false, true, true, true, false}, shorts);
    UnsafeReplacementDelegate.unsafePutShortVolatile(shorts, 2, (short) 0);
    UnsafeReplacementDelegate.unsafePutChar(shorts, 4, (char) 0);
    Assertions.assertArrayEquals(new boolean[6], shorts);

    var ints = new boolean[6];
    UnsafeReplacementDelegate.unsafePutInt(ints, 1, 0x05000080);
    Assertions.assertArrayEquals(new boolean[]{false, true, false, false, true, false}, ints);
    Assertions.assertEquals(0x01000001, UnsafeReplacementDelegate.unsafeGetInt(ints, 1));
    UnsafeReplacementDelegate.unsafePutIntVolatile(ints, 1, 0x00FFFF00);
    Assertions.assertArrayEquals(new boolean[]{false, false, true, true, false, false}, ints);
    Assertions.assertEquals(0x00010100, UnsafeReplacementDelegate.unsafeGetIntVolatile(ints, 1));
    UnsafeReplacementDelegate.unsafePutOrderedInt(ints, 2, 0);
    Assertions.assertArrayEquals(new boolean[6], ints);
    UnsafeReplacementDelegate.unsafePutFloat(ints, 0, Float.intBitsToFloat(0x05000080));
    Assertions.assertArrayEquals(new boolean[]{true, false, false, true, false, false}, ints);
    UnsafeReplacementDelegate.unsafePutFloatVolatile(ints, 0, 0F);
    Assertions.assertArrayEquals(new boolean[6], ints);

    var longs = new boolean[10];
    UnsafeReplacementDelegate.unsafePutLong(longs, 1, 0x7F000000000000FEL);
    Assertions.assertArrayEquals(
      new boolean[]{false, true, false, false, false, false, false, false, true, false},
      longs);
    Assertions.assertEquals(0x0100000000000001L, UnsafeReplacementDelegate.unsafeGetLong(longs, 1));
    UnsafeReplacementDelegate.unsafePutLongVolatile(longs, 1, 0x000000FFFF000000L);
    Assertions.assertArrayEquals(
      new boolean[]{false, false, false, false, true, true, false, false, false, false},
      longs);
    UnsafeReplacementDelegate.unsafePutOrderedLong(longs, 2, 0L);
    Assertions.assertArrayEquals(new boolean[10], longs);
    UnsafeReplacementDelegate.unsafePutDouble(longs, 2, Double.longBitsToDouble(0x7F000000000000FEL));
    Assertions.assertArrayEquals(
      new boolean[]{false, false, true, false, false, false, false, false, false, true},
      longs);
    UnsafeReplacementDelegate.unsafePutDoubleVolatile(longs, 2, 0D);
    Assertions.assertArrayEquals(new boolean[10], longs);

    // atomic operations work on the normalized values as well
    var atomics = new boolean[8];
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(atomics, 0, 0, 0x05000080));
    Assertions.assertArrayEquals(new boolean[]{true, false, false, true, false, false, false, false}, atomics);
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasInt(atomics, 0, 0x05000080, 0));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(atomics, 0, 0x01000001, 0x00FFFF00));
    Assertions.assertEquals(0x00010100, UnsafeReplacementDelegate.unsafeGetAndAddInt(atomics, 0, 0x01000001));
    Assertions.assertEquals(0x01010101, UnsafeReplacementDelegate.unsafeGetAndSetInt(atomics, 0, 0x00020200));
    Assertions.assertArrayEquals(new boolean[]{false, true, true, false, false, false, false, false}, atomics);
    Assertions.assertEquals(0x00010100, UnsafeReplacementDelegate.unsafeGetAndSetInt(atomics, 0, 0));

    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(atomics, 0, 0L, 0x7F000000000000FEL));
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasLong(atomics, 0, 0x7F000000000000FEL, 0L));
    Assertions.assertEquals(
      0x0100000000000001L,
      UnsafeReplacementDelegate.unsafeGetAndAddLong(atomics, 0, 0x0001000000000100L));
    Assertions.assertEquals(
      0x0101000000000101L,
      UnsafeReplacementDelegate.unsafeGetAndSetLong(atomics, 0, 0x000000FFFF000000L));
    Assertions.assertArrayEquals(new boolean[]{false, false, false, true, true, false, false, false}, atomics);

    // boolean reads from other primitive arrays treat any non-zero byte as true
    var raw = new byte[]{0, 1, 2, -1};
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeGetBoolean(raw, 0));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeGetBoolean(raw, 1));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeGetBoolean(raw, 2));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeGetBooleanVolatile(raw, 3));
  }

  @Test
  void testPrimitiveAccessOnReferenceArrays() {
    // primitive reads convert the (boxed) element at the offset, non-primitive elements are read as 0
    var refs = new Object[]{5, 0x100000002L, 1.5D, true, "string", null, 'c', 2.5F};
    Assertions.assertEquals(5, UnsafeReplacementDelegate.unsafeGetInt(refs, 0));
    Assertions.assertEquals(5, UnsafeReplacementDelegate.unsafeGetIntVolatile(refs, 0));
    Assertions.assertEquals(5, UnsafeReplacementDelegate.unsafeGetByte(refs, 0));
    Assertions.assertEquals(5, UnsafeReplacementDelegate.unsafeGetShortVolatile(refs, 0));
    Assertions.assertEquals(0x100000002L, UnsafeReplacementDelegate.unsafeGetLong(refs, 4));
    Assertions.assertEquals(2, UnsafeReplacementDelegate.unsafeGetInt(refs, 4));
    Assertions.assertEquals(1.5D, UnsafeReplacementDelegate.unsafeGetDouble(refs, 8));
    Assertions.assertEquals(Double.doubleToRawLongBits(1.5D), UnsafeReplacementDelegate.unsafeGetLongVolatile(refs, 8));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeGetBoolean(refs, 12));
    Assertions.assertEquals(1, UnsafeReplacementDelegate.unsafeGetInt(refs, 12));
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(refs, 16));
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeGetLong(refs, 20));
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeGetBooleanVolatile(refs, 20));
    Assertions.assertEquals('c', UnsafeReplacementDelegate.unsafeGetChar(refs, 24));
    Assertions.assertEquals(2.5F, UnsafeReplacementDelegate.unsafeGetFloatVolatile(refs, 28));
    Assertions.assertEquals(Float.floatToRawIntBits(2.5F), UnsafeReplacementDelegate.unsafeGetInt(refs, 28));

    // primitive writes store the boxed value
    UnsafeReplacementDelegate.unsafePutInt(refs, 0, 7);
    UnsafeReplacementDelegate.unsafePutLongVolatile(refs, 4, 8L);
    UnsafeReplacementDelegate.unsafePutDouble(refs, 8, -1.5D);
    UnsafeReplacementDelegate.unsafePutBoolean(refs, 12, false);
    UnsafeReplacementDelegate.unsafePutByteVolatile(refs, 16, (byte) 9);
    UnsafeReplacementDelegate.unsafePutShort(refs, 20, (short) 10);
    UnsafeReplacementDelegate.unsafePutCharVolatile(refs, 24, 'x');
    UnsafeReplacementDelegate.unsafePutFloat(refs, 28, -2.5F);
    Assertions.assertArrayEquals(new Object[]{7, 8L, -1.5D, false, (byte) 9, (short) 10, 'x', -2.5F}, refs);
    UnsafeReplacementDelegate.unsafePutOrderedInt(refs, 0, 11);
    UnsafeReplacementDelegate.unsafePutOrderedLong(refs, 4, 12L);
    UnsafeReplacementDelegate.unsafePutBooleanVolatile(refs, 12, true);
    UnsafeReplacementDelegate.unsafePutFloatVolatile(refs, 16, 13F);
    UnsafeReplacementDelegate.unsafePutDoubleVolatile(refs, 20, 14D);
    Assertions.assertArrayEquals(new Object[]{11, 12L, -1.5D, true, 13F, 14D, 'x', -2.5F}, refs);

    // misaligned offsets are ignored
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(refs, 2));
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeGetLongVolatile(refs, 5));
    UnsafeReplacementDelegate.unsafePutInt(refs, 2, 1);
    UnsafeReplacementDelegate.unsafePutLongVolatile(refs, 7, 1L);
    Assertions.assertArrayEquals(new Object[]{11, 12L, -1.5D, true, 13F, 14D, 'x', -2.5F}, refs);

    // primitive writes into typed reference arrays are ignored, reads return 0
    var strings = new String[]{"a"};
    UnsafeReplacementDelegate.unsafePutInt(strings, 0, 1);
    UnsafeReplacementDelegate.unsafePutLongVolatile(strings, 0, 1L);
    Assertions.assertArrayEquals(new String[]{"a"}, strings);
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(strings, 0));
    var numbers = new Number[]{1};
    UnsafeReplacementDelegate.unsafePutLong(numbers, 0, 2L);
    Assertions.assertArrayEquals(new Number[]{2L}, numbers);

    // atomic primitive operations on reference arrays are not supported
    var atomicRefs = new Object[]{0, 0L};
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeCasInt(atomicRefs, 0, 0, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeCasLong(atomicRefs, 4, 0L, 1L));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddInt(atomicRefs, 0, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddLong(atomicRefs, 4, 1L));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetInt(atomicRefs, 0, 1));
    Assertions.assertThrows(
      UnsupportedOperationException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetLong(atomicRefs, 4, 1L));
    Assertions.assertArrayEquals(new Object[]{0, 0L}, atomicRefs);
  }

  @Test
  void testReferenceAccessOnPrimitiveArrays() {
    var arrays = new ArrayList<>(newPrimitiveMemoryArrays(16));
    arrays.add(new boolean[16]);
    for (var array : arrays) {
      if (array instanceof boolean[] bools) {
        Arrays.fill(bools, true);
      } else {
        memorySegmentOfArray(array).fill((byte) 0x11);
      }
      var snapshot = Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
      System.arraycopy(array, 0, snapshot, 0, Array.getLength(array));

      // plain reference access is ignored, atomic reference access is rejected
      Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObject(array, 0));
      Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObjectVolatile(array, 8));
      UnsafeReplacementDelegate.unsafePutObject(array, 0, "value");
      UnsafeReplacementDelegate.unsafePutObjectVolatile(array, 4, 5);
      UnsafeReplacementDelegate.unsafePutOrderedObject(array, 8, null);
      Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> UnsafeReplacementDelegate.unsafeCasObject(array, 0, null, "value"));
      Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(array, 4, "value"));
      Assertions.assertTrue(Objects.deepEquals(snapshot, array), array.getClass().getSimpleName());
    }
  }

  @Test
  void testArrayAccessOutOfBoundsIsLenient() {
    for (var kind : memoryAccessKinds()) {
      var array = kind.newArray().apply(4);
      var scale = kind.byteSize();
      var defaultValue = Array.get(array, 0);
      var offsets = new long[]{4L * scale, 3L * scale + 1, -scale, -1, 1L << 40, Long.MIN_VALUE, Long.MAX_VALUE};
      for (var offset : offsets) {
        for (var reader : kind.readers()) {
          Assertions.assertEquals(defaultValue, reader.read(array, offset), kind.name() + " at " + offset);
        }
        for (var writer : kind.writers()) {
          writer.write(array, offset, kind.value());
        }
      }

      for (var index = 0; index < 4; index++) {
        Assertions.assertEquals(defaultValue, Array.get(array, index), kind.name() + "[" + index + "]");
      }
    }

    // reference arrays
    var refs = new Object[4];
    for (var offset : new long[]{16, -4, 1L << 40, Long.MIN_VALUE, Long.MAX_VALUE}) {
      Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObject(refs, offset));
      Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObjectVolatile(refs, offset));
      UnsafeReplacementDelegate.unsafePutObject(refs, offset, "value");
      UnsafeReplacementDelegate.unsafePutObjectVolatile(refs, offset, "value");
      UnsafeReplacementDelegate.unsafePutOrderedObject(refs, offset, "value");
    }
    Assertions.assertArrayEquals(new Object[4], refs);
  }

  @Test
  void testArrayAtomicOperationsOutOfBoundsThrow() {
    var arrays = new ArrayList<>(newPrimitiveMemoryArrays(8));
    arrays.add(new boolean[8]);
    for (var array : arrays) {
      // fully and partially out of bounds offsets
      for (var offset : new long[]{8, 6, -4, -8, 1L << 40, Long.MIN_VALUE, Long.MAX_VALUE - 7}) {
        Assertions.assertThrows(
          IndexOutOfBoundsException.class,
          () -> UnsafeReplacementDelegate.unsafeCasInt(array, offset, 0, 1));
        Assertions.assertThrows(
          IndexOutOfBoundsException.class,
          () -> UnsafeReplacementDelegate.unsafeGetAndAddInt(array, offset, 1));
        Assertions.assertThrows(
          IndexOutOfBoundsException.class,
          () -> UnsafeReplacementDelegate.unsafeGetAndSetInt(array, offset, 1));
      }
      for (var offset : new long[]{8, 4, 1, -8, 1L << 40, Long.MIN_VALUE, Long.MAX_VALUE - 7}) {
        Assertions.assertThrows(
          IndexOutOfBoundsException.class,
          () -> UnsafeReplacementDelegate.unsafeCasLong(array, offset, 0L, 1L));
        Assertions.assertThrows(
          IndexOutOfBoundsException.class,
          () -> UnsafeReplacementDelegate.unsafeGetAndAddLong(array, offset, 1L));
        Assertions.assertThrows(
          IndexOutOfBoundsException.class,
          () -> UnsafeReplacementDelegate.unsafeGetAndSetLong(array, offset, 1L));
      }

      if (array instanceof boolean[] bools) {
        Assertions.assertArrayEquals(new boolean[8], bools);
      } else {
        Assertions.assertArrayEquals(new byte[8], memoryBytesOfArray(array), array.getClass().getSimpleName());
      }
    }

    var refs = new Object[4];
    for (var offset : new long[]{16, -4, 1L << 40, Long.MIN_VALUE}) {
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> UnsafeReplacementDelegate.unsafeCasObject(refs, offset, null, "value"));
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(refs, offset, "value"));
    }
    Assertions.assertArrayEquals(new Object[4], refs);
  }

  @Test
  void testArrayAtomicOperationsWithMatchingElementType() {
    var ints = new int[5];
    var longs = new long[5];
    for (var index : new int[]{0, 2, 4}) {
      assertMemoryIntAtomics(ints, index * 4L, "int[" + index + "]");
      Assertions.assertEquals(0x89ABCDEF, ints[index]);
      assertMemoryLongAtomics(longs, index * 8L, "long[" + index + "]");
      Assertions.assertEquals(0xFEDCBA9876543210L, longs[index]);
    }
    Assertions.assertArrayEquals(new int[]{0x89ABCDEF, 0, 0x89ABCDEF, 0, 0x89ABCDEF}, ints);
    Assertions.assertArrayEquals(
      new long[]{0xFEDCBA9876543210L, 0, 0xFEDCBA9876543210L, 0, 0xFEDCBA9876543210L},
      longs);

    // reference arrays compare by identity
    for (var array : List.<Object[]>of(new Object[5], new String[5], new CharSequence[5])) {
      for (var index : new int[]{0, 2, 4}) {
        var offset = index * 4L;
        var first = "first" + index;
        var second = "second" + index;
        Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(array, offset, null, first));
        Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasObject(array, offset, null, second));
        Assertions.assertFalse(UnsafeReplacementDelegate.unsafeCasObject(array, offset, new String(first), second));
        Assertions.assertSame(first, array[index]);
        Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasObject(array, offset, first, second));
        Assertions.assertSame(second, UnsafeReplacementDelegate.unsafeGetAndSetObject(array, offset, first));
        Assertions.assertSame(first, array[index]);
      }
      Assertions.assertArrayEquals(new Object[]{"first0", null, "first2", null, "first4"}, array);
    }
  }

  @Test
  void testArrayAtomicOperationsWithOtherElementTypes() {
    // aligned (0, 4, 8, 24) and misaligned (1, 6, 13) offsets into arrays of every primitive type, depending on the
    // array type the operations are either done atomically or emulated
    var offsets = new int[]{0, 4, 8, 24, 1, 6, 13};
    for (var arrayIndex = 0; arrayIndex < 7; arrayIndex++) {
      for (var offset : offsets) {
        var intArray = newPrimitiveMemoryArrays(32).get(arrayIndex);
        var intContext = "int at offset " + offset + " in " + intArray.getClass().getSimpleName();
        assertMemoryIntAtomics(intArray, offset, intContext);
        Assertions.assertArrayEquals(
          expectedMemoryBytes(32, offset, 0x89ABCDEF),
          memoryBytesOfArray(intArray),
          intContext);

        var longArray = newPrimitiveMemoryArrays(32).get(arrayIndex);
        var longContext = "long at offset " + offset + " in " + longArray.getClass().getSimpleName();
        assertMemoryLongAtomics(longArray, offset, longContext);
        Assertions.assertArrayEquals(
          expectedMemoryBytes(32, offset, 0xFEDCBA9876543210L),
          memoryBytesOfArray(longArray),
          longContext);
      }
    }

    // atomic int and long operations on floating point arrays work on the raw bits
    var floats = new float[]{1.5F, 2.5F};
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(
      floats,
      4,
      Float.floatToRawIntBits(2.5F),
      Float.floatToRawIntBits(-3.5F)));
    Assertions.assertEquals(Float.floatToRawIntBits(1.5F), UnsafeReplacementDelegate.unsafeGetAndAddInt(floats, 0, 1));
    Assertions.assertArrayEquals(new float[]{Math.nextUp(1.5F), -3.5F}, floats);
    var doubles = new double[]{1.5D, 2.5D};
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasLong(
      doubles,
      8,
      Double.doubleToRawLongBits(2.5D),
      Double.doubleToRawLongBits(-3.5D)));
    Assertions.assertEquals(
      Double.doubleToRawLongBits(1.5D),
      UnsafeReplacementDelegate.unsafeGetAndAddLong(doubles, 0, 1L));
    Assertions.assertArrayEquals(new double[]{Math.nextUp(1.5D), -3.5D}, doubles);

    // int operations on a long array only change the addressed half
    var halves = new long[1];
    UnsafeReplacementDelegate.unsafePutInt(halves, 0, -1);
    UnsafeReplacementDelegate.unsafePutInt(halves, 4, 7);
    Assertions.assertEquals(-1, UnsafeReplacementDelegate.unsafeGetAndAddInt(halves, 0, 1));
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(halves, 0));
    Assertions.assertEquals(7, UnsafeReplacementDelegate.unsafeGetAndSetInt(halves, 4, 8));
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetInt(halves, 0));
    Assertions.assertEquals(8, UnsafeReplacementDelegate.unsafeGetInt(halves, 4));
  }

  @Test
  void testOffHeapAccessWithAllValueTypes() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    try {
      var memory = offHeapMemorySegment(address, 32);
      for (var kind : memoryAccessKinds()) {
        // writes through the address only overloads and the overloads with a null base
        var writers = new ArrayList<>(kind.addressWriters());
        kind.writers().forEach(writer -> writers.add((target, value) -> writer.write(null, target, value)));
        for (var offset : new int[]{0, 8, 24, 1, 6, 13}) {
          var context = kind.name() + " at offset " + offset;
          var expectedBytes = expectedMemoryBytes(32, offset, kind.value());
          for (var writer : writers) {
            memory.fill((byte) 0);
            writer.write(address + offset, kind.value());
            Assertions.assertArrayEquals(expectedBytes, memory.toArray(ValueLayout.JAVA_BYTE), context);
            for (var reader : kind.readers()) {
              Assertions.assertEquals(kind.value(), reader.read(null, address + offset), context);
            }
            for (var reader : kind.addressReaders()) {
              Assertions.assertEquals(kind.value(), reader.apply(address + offset), context);
            }
          }
        }
      }
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testOffHeapReferenceAccess() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(16);
    try {
      var memory = offHeapMemorySegment(address, 16);
      memory.fill((byte) 0x11);

      // plain reference access is ignored, atomic reference access is rejected
      Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObject(null, address));
      Assertions.assertNull(UnsafeReplacementDelegate.unsafeGetObjectVolatile(null, address + 8));
      UnsafeReplacementDelegate.unsafePutObject(null, address, "value");
      UnsafeReplacementDelegate.unsafePutObjectVolatile(null, address + 4, "value");
      UnsafeReplacementDelegate.unsafePutOrderedObject(null, address + 8, "value");
      Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> UnsafeReplacementDelegate.unsafeCasObject(null, address, null, "value"));
      Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(null, address, "value"));

      var expected = new byte[16];
      Arrays.fill(expected, (byte) 0x11);
      Assertions.assertArrayEquals(expected, memory.toArray(ValueLayout.JAVA_BYTE));
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testOffHeapAddressAccess() {
    var addressSize = UnsafeReplacementDelegate.unsafeAddressSize();
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    try {
      var memory = offHeapMemorySegment(address, 32);
      for (var offset : new int[]{0, 8, 3, 13}) {
        memory.fill((byte) 0);
        UnsafeReplacementDelegate.unsafePutAddress(address + offset, 0x0123456789ABCDEFL);
        if (addressSize == 8) {
          Assertions.assertEquals(0x0123456789ABCDEFL, UnsafeReplacementDelegate.unsafeGetAddress(address + offset));
          Assertions.assertArrayEquals(
            expectedMemoryBytes(32, offset, 0x0123456789ABCDEFL),
            memory.toArray(ValueLayout.JAVA_BYTE));
        } else {
          // only the lower 4 bytes are stored, they are read back as an unsigned value
          Assertions.assertEquals(0x89ABCDEFL, UnsafeReplacementDelegate.unsafeGetAddress(address + offset));
          Assertions.assertArrayEquals(
            expectedMemoryBytes(32, offset, 0x89ABCDEF),
            memory.toArray(ValueLayout.JAVA_BYTE));
        }

        // an actual address can be stored and loaded
        UnsafeReplacementDelegate.unsafePutAddress(address + offset, address);
        Assertions.assertEquals(address, UnsafeReplacementDelegate.unsafeGetAddress(address + offset));
      }
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testOffHeapAtomicOperations() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    try {
      var memory = offHeapMemorySegment(address, 32);
      for (var offset : new int[]{0, 4, 8, 24, 1, 6, 13}) {
        memory.fill((byte) 0);
        assertMemoryIntAtomics(null, address + offset, "int at off-heap offset " + offset);
        Assertions.assertArrayEquals(
          expectedMemoryBytes(32, offset, 0x89ABCDEF),
          memory.toArray(ValueLayout.JAVA_BYTE));

        memory.fill((byte) 0);
        assertMemoryLongAtomics(null, address + offset, "long at off-heap offset " + offset);
        Assertions.assertArrayEquals(
          expectedMemoryBytes(32, offset, 0xFEDCBA9876543210L),
          memory.toArray(ValueLayout.JAVA_BYTE));
      }
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testOffHeapMisalignedVolatileReadsDoNotObservePartialStores() throws InterruptedException {
    try (var arena = Arena.ofShared()) {
      var memory = arena.allocate(256, 64);
      // Cross cache-line boundaries so unaligned native loads and stores can tear.
      var intAddress = memory.address() + 63;
      var longAddress = memory.address() + 191;
      var stop = new AtomicBoolean();
      var writer = Thread.ofPlatform().start(() -> {
        while (!stop.get()) {
          UnsafeReplacementDelegate.unsafePutIntVolatile(null, intAddress, -1);
          UnsafeReplacementDelegate.unsafePutLongVolatile(null, longAddress, -1L);
          UnsafeReplacementDelegate.unsafePutIntVolatile(null, intAddress, 0);
          UnsafeReplacementDelegate.unsafePutLongVolatile(null, longAddress, 0L);
        }
      });

      try {
        for (var index = 0; index < 1_000_000; index++) {
          var intValue = UnsafeReplacementDelegate.unsafeGetIntVolatile(null, intAddress);
          var longValue = UnsafeReplacementDelegate.unsafeGetLongVolatile(null, longAddress);
          Assertions.assertTrue(intValue == 0 || intValue == -1, () -> "Partial int store: " + intValue);
          Assertions.assertTrue(longValue == 0 || longValue == -1L, () -> "Partial long store: " + longValue);
        }
      } finally {
        stop.set(true);
        writer.join();
      }
    }
  }

  @Test
  void testOffHeapAtomicOperationsAreAtomic() throws InterruptedException {
    var iterations = 20_000;
    var threadCount = 4;
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    try {
      UnsafeReplacementDelegate.unsafeSetMemory(address, 32, (byte) 0);

      // aligned and misaligned get-and-add, misaligned cas loop
      startConcurrently(threadCount, () -> {
        for (var index = 0; index < iterations; index++) {
          UnsafeReplacementDelegate.unsafeGetAndAddInt(null, address, 1);
          UnsafeReplacementDelegate.unsafeGetAndAddInt(null, address + 9, 1);
          UnsafeReplacementDelegate.unsafeGetAndAddLong(null, address + 17, 1);
          int current;
          do {
            current = UnsafeReplacementDelegate.unsafeGetIntVolatile(null, address + 26);
          } while (!UnsafeReplacementDelegate.unsafeCasInt(null, address + 26, current, current + 1));
        }
      }).join();

      var expected = iterations * threadCount;
      Assertions.assertEquals(expected, UnsafeReplacementDelegate.unsafeGetInt(address));
      Assertions.assertEquals(expected, UnsafeReplacementDelegate.unsafeGetInt(address + 9));
      Assertions.assertEquals(expected, UnsafeReplacementDelegate.unsafeGetLong(address + 17));
      Assertions.assertEquals(expected, UnsafeReplacementDelegate.unsafeGetInt(address + 26));
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testAllocateMemoryEdgeCases() {
    // zero bytes return the null address
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeAllocateMemory(0));

    for (var invalidSize : new long[]{-1, Integer.MIN_VALUE, Long.MIN_VALUE}) {
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeAllocateMemory(invalidSize));
    }
    for (var hugeSize : new long[]{Long.MAX_VALUE, 1L << 62}) {
      var thrown = Assertions.assertThrows(
        OutOfMemoryError.class,
        () -> UnsafeReplacementDelegate.unsafeAllocateMemory(hugeSize));
      Assertions.assertEquals("Unable to allocate " + hugeSize + " bytes", thrown.getMessage());
    }

    var small = UnsafeReplacementDelegate.unsafeAllocateMemory(1);
    var large = UnsafeReplacementDelegate.unsafeAllocateMemory(1 << 20);
    try {
      Assertions.assertNotEquals(0L, small);
      Assertions.assertNotEquals(0L, large);
      Assertions.assertNotEquals(small, large);

      // the complete allocated range is accessible
      UnsafeReplacementDelegate.unsafePutByte(small, (byte) 0x7F);
      UnsafeReplacementDelegate.unsafePutLong(large, -1L);
      UnsafeReplacementDelegate.unsafePutLong(large + (1 << 20) - 8, 1337L);
      Assertions.assertEquals(0x7F, UnsafeReplacementDelegate.unsafeGetByte(small));
      Assertions.assertEquals(-1L, UnsafeReplacementDelegate.unsafeGetLong(large));
      Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetLong(large + (1 << 20) - 8));
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(small);
      UnsafeReplacementDelegate.unsafeFreeMemory(large);
    }

    // freeing the null address does nothing
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeFreeMemory(0));
  }

  @Test
  void testReallocateMemoryEdgeCases() {
    // re-allocating the null address to zero bytes does nothing
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeReallocateMemory(0, 0));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeReallocateMemory(0, -1));

    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(16);
    try {
      for (var index = 0; index < 16; index++) {
        UnsafeReplacementDelegate.unsafePutByte(address + index, (byte) (index + 1));
      }

      // growing keeps the existing content
      address = UnsafeReplacementDelegate.unsafeReallocateMemory(address, 4096);
      Assertions.assertNotEquals(0L, address);
      for (var index = 0; index < 16; index++) {
        Assertions.assertEquals(index + 1, UnsafeReplacementDelegate.unsafeGetByte(address + index));
      }
      UnsafeReplacementDelegate.unsafePutLong(address + 4088, 1337L);
      Assertions.assertEquals(1337L, UnsafeReplacementDelegate.unsafeGetLong(address + 4088));

      // shrinking keeps the content up to the new size
      address = UnsafeReplacementDelegate.unsafeReallocateMemory(address, 4);
      Assertions.assertNotEquals(0L, address);
      for (var index = 0; index < 4; index++) {
        Assertions.assertEquals(index + 1, UnsafeReplacementDelegate.unsafeGetByte(address + index));
      }

      // failed re-allocations keep the old block
      var current = address;
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeReallocateMemory(current, -1));
      Assertions.assertThrows(
        OutOfMemoryError.class,
        () -> UnsafeReplacementDelegate.unsafeReallocateMemory(current, Long.MAX_VALUE));
      for (var index = 0; index < 4; index++) {
        Assertions.assertEquals(index + 1, UnsafeReplacementDelegate.unsafeGetByte(address + index));
      }
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }

    // re-allocating to zero bytes frees the block
    var freed = UnsafeReplacementDelegate.unsafeAllocateMemory(8);
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeReallocateMemory(freed, 0));
  }

  @Test
  void testSetMemoryOnPrimitiveArrays() {
    for (var arrayIndex = 0; arrayIndex < 7; arrayIndex++) {
      var array = newPrimitiveMemoryArrays(16).get(arrayIndex);
      var context = array.getClass().getSimpleName();
      var expected = new byte[16];

      UnsafeReplacementDelegate.unsafeSetMemory(array, 3, 9, (byte) 0x5A);
      Arrays.fill(expected, 3, 12, (byte) 0x5A);
      Assertions.assertArrayEquals(expected, memoryBytesOfArray(array), context);
      UnsafeReplacementDelegate.unsafeSetMemory(array, 0, 1, (byte) -1);
      UnsafeReplacementDelegate.unsafeSetMemory(array, 15, 1, (byte) -2);
      expected[0] = -1;
      expected[15] = -2;
      Assertions.assertArrayEquals(expected, memoryBytesOfArray(array), context);

      // zero bytes do nothing, even at invalid offsets
      UnsafeReplacementDelegate.unsafeSetMemory(array, 4, 0, (byte) 1);
      UnsafeReplacementDelegate.unsafeSetMemory(array, -1, 0, (byte) 1);
      UnsafeReplacementDelegate.unsafeSetMemory(array, 1L << 40, 0, (byte) 1);
      Assertions.assertArrayEquals(expected, memoryBytesOfArray(array), context);

      // out of bounds ranges throw without modifying the array
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> UnsafeReplacementDelegate.unsafeSetMemory(array, 8, 9, (byte) 1));
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> UnsafeReplacementDelegate.unsafeSetMemory(array, -1, 2, (byte) 1));
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> UnsafeReplacementDelegate.unsafeSetMemory(array, 1L << 40, 1, (byte) 1));
      Assertions.assertArrayEquals(expected, memoryBytesOfArray(array), context);

      // filling the complete array
      UnsafeReplacementDelegate.unsafeSetMemory(array, 0, 16, (byte) 0x33);
      Arrays.fill(expected, (byte) 0x33);
      Assertions.assertArrayEquals(expected, memoryBytesOfArray(array), context);
    }
  }

  @Test
  void testSetMemoryOnBooleanArrays() {
    var bools = new boolean[8];
    UnsafeReplacementDelegate.unsafeSetMemory(bools, 2, 4, (byte) 0x40);
    Assertions.assertArrayEquals(new boolean[]{false, false, true, true, true, true, false, false}, bools);
    UnsafeReplacementDelegate.unsafeSetMemory(bools, 3, 2, (byte) 0);
    Assertions.assertArrayEquals(new boolean[]{false, false, true, false, false, true, false, false}, bools);
    UnsafeReplacementDelegate.unsafeSetMemory(bools, 0, 0, (byte) 1);
    UnsafeReplacementDelegate.unsafeSetMemory(bools, -1, 0, (byte) 1);
    Assertions.assertArrayEquals(new boolean[]{false, false, true, false, false, true, false, false}, bools);
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeSetMemory(bools, -1, 2, (byte) 1));
    Assertions.assertArrayEquals(new boolean[]{false, false, true, false, false, true, false, false}, bools);
    UnsafeReplacementDelegate.unsafeSetMemory(bools, 0, 8, (byte) -1);
    Assertions.assertArrayEquals(new boolean[]{true, true, true, true, true, true, true, true}, bools);
  }

  @Test
  void testSetMemoryRejectsInvalidBases() {
    for (var base : List.of(new Object[4], new String[4], new int[1][4], new Object(), "string", 5)) {
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeSetMemory(base, 0, 4, (byte) 1));
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeSetMemory(base, 0, 0, (byte) 1));
    }
  }

  @Test
  void testSetOffHeapMemoryRanges() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(16);
    try {
      var memory = offHeapMemorySegment(address, 16);
      memory.fill((byte) 0);

      UnsafeReplacementDelegate.unsafeSetMemory(address + 3, 5, (byte) 0x5A);
      UnsafeReplacementDelegate.unsafeSetMemory(null, address + 10, 6, (byte) -1);
      UnsafeReplacementDelegate.unsafeSetMemory(address, 0, (byte) 1);
      UnsafeReplacementDelegate.unsafeSetMemory(null, address, 0, (byte) 1);
      UnsafeReplacementDelegate.unsafeSetMemory(0, 0, (byte) 1);
      UnsafeReplacementDelegate.unsafeSetMemory(null, 0, 0, (byte) 1);

      var expected = new byte[16];
      Arrays.fill(expected, 3, 8, (byte) 0x5A);
      Arrays.fill(expected, 10, 16, (byte) -1);
      Assertions.assertArrayEquals(expected, memory.toArray(ValueLayout.JAVA_BYTE));
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testCopyMemoryBetweenPrimitiveArrays() {
    var pattern = new byte[24];
    for (var index = 0; index < pattern.length; index++) {
      pattern[index] = (byte) (index * 7 + 1);
    }

    for (var srcIndex = 0; srcIndex < 7; srcIndex++) {
      for (var dstIndex = 0; dstIndex < 7; dstIndex++) {
        var src = newPrimitiveMemoryArrays(24).get(srcIndex);
        var dst = newPrimitiveMemoryArrays(24).get(dstIndex);
        var context = src.getClass().getSimpleName() + " -> " + dst.getClass().getSimpleName();
        memorySegmentOfArray(src).copyFrom(MemorySegment.ofArray(pattern));

        UnsafeReplacementDelegate.unsafeCopyMemory(src, 3, dst, 5, 13);
        var expected = new byte[24];
        System.arraycopy(pattern, 3, expected, 5, 13);
        Assertions.assertArrayEquals(expected, memoryBytesOfArray(dst), context);
        Assertions.assertArrayEquals(pattern, memoryBytesOfArray(src), context);
      }
    }
  }

  @Test
  void testCopyMemoryWithBooleanArrays() {
    // bytes copied into a boolean array are normalized
    var bytes = new byte[]{0, 1, 2, -1, 0, 0x40};
    var bools = new boolean[8];
    UnsafeReplacementDelegate.unsafeCopyMemory(bytes, 1, bools, 2, 5);
    Assertions.assertArrayEquals(new boolean[]{false, false, true, true, true, false, true, false}, bools);

    // boolean arrays are copied as 0 and 1 bytes
    var target = new byte[]{9, 9, 9, 9, 9, 9, 9, 9, 9};
    UnsafeReplacementDelegate.unsafeCopyMemory(bools, 0, target, 1, 8);
    Assertions.assertArrayEquals(new byte[]{9, 0, 0, 1, 1, 1, 0, 1, 0}, target);
    var ints = new int[2];
    UnsafeReplacementDelegate.unsafeCopyMemory(bools, 0, ints, 0, 8);
    Assertions.assertArrayEquals(new byte[]{0, 0, 1, 1, 1, 0, 1, 0}, memoryBytesOfArray(ints));

    var boolTarget = new boolean[]{true, true, true, true};
    UnsafeReplacementDelegate.unsafeCopyMemory(bools, 4, boolTarget, 1, 3);
    Assertions.assertArrayEquals(new boolean[]{true, true, false, true}, boolTarget);
  }

  @Test
  void testCopyMemoryBetweenHeapAndOffHeap() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    try {
      var memory = offHeapMemorySegment(address, 32);
      memory.fill((byte) 0);

      var src = new int[]{0x01020304, 0x05060708, 0x090A0B0C, 0x0D0E0F10};
      var srcBytes = memoryBytesOfArray(src);
      UnsafeReplacementDelegate.unsafeCopyMemory(src, 2, null, address + 5, 11);
      var expectedMemory = new byte[32];
      System.arraycopy(srcBytes, 2, expectedMemory, 5, 11);
      Assertions.assertArrayEquals(expectedMemory, memory.toArray(ValueLayout.JAVA_BYTE));

      // copy the data back into an array of another type at an offset
      var dst = new short[8];
      UnsafeReplacementDelegate.unsafeCopyMemory(null, address + 5, dst, 3, 11);
      var expectedDst = new byte[16];
      System.arraycopy(srcBytes, 2, expectedDst, 3, 11);
      Assertions.assertArrayEquals(expectedDst, memoryBytesOfArray(dst));

      // boolean arrays are copied as 0 and 1 bytes
      var bools = new boolean[]{false, true, true, false};
      UnsafeReplacementDelegate.unsafeCopyMemory(bools, 1, null, address + 20, 3);
      expectedMemory[20] = 1;
      expectedMemory[21] = 1;
      expectedMemory[22] = 0;
      Assertions.assertArrayEquals(expectedMemory, memory.toArray(ValueLayout.JAVA_BYTE));
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testCopyOffHeapMemoryRanges() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(32);
    try {
      var memory = offHeapMemorySegment(address, 32);
      var pattern = new byte[32];
      for (var index = 0; index < 16; index++) {
        pattern[index] = (byte) (index + 1);
      }
      memory.copyFrom(MemorySegment.ofArray(pattern));

      UnsafeReplacementDelegate.unsafeCopyMemory(address + 2, address + 17, 5);
      UnsafeReplacementDelegate.unsafeCopyMemory(null, address + 10, null, address + 26, 6);
      UnsafeReplacementDelegate.unsafeCopyMemory(address, address + 16, 0);
      UnsafeReplacementDelegate.unsafeCopyMemory(0, 0, 0);
      UnsafeReplacementDelegate.unsafeCopyMemory(null, 0, null, 0, 0);

      var expected = pattern.clone();
      System.arraycopy(pattern, 2, expected, 17, 5);
      System.arraycopy(pattern, 10, expected, 26, 6);
      Assertions.assertArrayEquals(expected, memory.toArray(ValueLayout.JAVA_BYTE));
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testCopyOverlappingMemory() {
    var pattern = new byte[16];
    for (var index = 0; index < pattern.length; index++) {
      pattern[index] = (byte) (index + 1);
    }

    // on-heap, destination after and before the source
    for (var direction : new int[][]{{0, 3}, {3, 0}}) {
      var srcOffset = direction[0];
      var dstOffset = direction[1];
      var expected = pattern.clone();
      System.arraycopy(pattern, srcOffset, expected, dstOffset, 10);

      var bytes = pattern.clone();
      UnsafeReplacementDelegate.unsafeCopyMemory(bytes, srcOffset, bytes, dstOffset, 10);
      Assertions.assertArrayEquals(expected, bytes);

      var longs = new long[2];
      memorySegmentOfArray(longs).copyFrom(MemorySegment.ofArray(pattern));
      UnsafeReplacementDelegate.unsafeCopyMemory(longs, srcOffset, longs, dstOffset, 10);
      Assertions.assertArrayEquals(expected, memoryBytesOfArray(longs));

      var bools = new boolean[]{true, false, true, true, false, false, true, false, true, true, true, false, false};
      var expectedBools = bools.clone();
      System.arraycopy(bools, srcOffset, expectedBools, dstOffset, 10);
      UnsafeReplacementDelegate.unsafeCopyMemory(bools, srcOffset, bools, dstOffset, 10);
      Assertions.assertArrayEquals(expectedBools, bools);

      // off-heap
      var address = UnsafeReplacementDelegate.unsafeAllocateMemory(16);
      try {
        var memory = offHeapMemorySegment(address, 16);
        memory.copyFrom(MemorySegment.ofArray(pattern));
        UnsafeReplacementDelegate.unsafeCopyMemory(address + srcOffset, address + dstOffset, 10);
        Assertions.assertArrayEquals(expected, memory.toArray(ValueLayout.JAVA_BYTE));
      } finally {
        UnsafeReplacementDelegate.unsafeFreeMemory(address);
      }
    }
  }

  @Test
  void testCopyMemoryWithZeroSizeOrInvalidBases() {
    var src = new int[]{1, 2};
    var dst = new int[2];

    // zero bytes do nothing, even at invalid offsets
    UnsafeReplacementDelegate.unsafeCopyMemory(src, 1L << 40, dst, -8, 0);
    UnsafeReplacementDelegate.unsafeCopyMemory(src, 0, null, 0, 0);
    UnsafeReplacementDelegate.unsafeCopyMemory(null, 0, dst, 0, 0);
    Assertions.assertArrayEquals(new int[2], dst);

    // out of bounds ranges throw without modifying the target
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeCopyMemory(src, 4, dst, 0, 8));
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeCopyMemory(src, 0, dst, 4, 8));
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeCopyMemory(src, -1, dst, 0, 4));
    Assertions.assertArrayEquals(new int[2], dst);

    // only primitive arrays (or null for off-heap memory) are valid bases, regardless of the size
    for (var base : List.of(new Object[2], new String[2], new int[1][2], new Object(), "string", 5)) {
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeCopyMemory(base, 0, dst, 0, 4));
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeCopyMemory(src, 0, base, 0, 4));
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeCopyMemory(base, 0, null, 0, 0));
      Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeCopyMemory(null, 0, base, 0, 0));
    }
    Assertions.assertArrayEquals(new int[2], dst);
  }

  @Test
  void testAtomicOperationsOnNarrowerArraysAreAtomic() throws InterruptedException {
    var iterations = 50_000;
    var threadCount = 4;

    // aligned and misaligned get-and-add on byte array
    var bytes = new byte[32];
    startConcurrently(threadCount, () -> {
      for (var index = 0; index < iterations; index++) {
        UnsafeReplacementDelegate.unsafeGetAndAddLong(bytes, 8, 1);
        UnsafeReplacementDelegate.unsafeGetAndAddInt(bytes, 21, 1);
      }
    }).join();
    Assertions.assertEquals((long) iterations * threadCount, UnsafeReplacementDelegate.unsafeGetLong(bytes, 8));
    Assertions.assertEquals(iterations * threadCount, UnsafeReplacementDelegate.unsafeGetInt(bytes, 21));

    // cas loop on short array
    var shorts = new short[8];
    startConcurrently(threadCount, () -> {
      for (var index = 0; index < iterations; index++) {
        int current;
        do {
          current = UnsafeReplacementDelegate.unsafeGetIntVolatile(shorts, 4);
        } while (!UnsafeReplacementDelegate.unsafeCasInt(shorts, 4, current, current + 1));
      }
    }).join();
    Assertions.assertEquals(iterations * threadCount, UnsafeReplacementDelegate.unsafeGetInt(shorts, 4));
  }

  @Test
  void testEmulatedVolatileReadsDoNotObservePartialStores() throws InterruptedException {
    var bools = new boolean[16];
    var stop = new AtomicBoolean();
    var writer = Thread.ofPlatform().start(() -> {
      while (!stop.get()) {
        UnsafeReplacementDelegate.unsafePutIntVolatile(bools, 1, 0x01010101);
        UnsafeReplacementDelegate.unsafePutLongVolatile(bools, 7, 0x0101010101010101L);
        UnsafeReplacementDelegate.unsafePutIntVolatile(bools, 1, 0);
        UnsafeReplacementDelegate.unsafePutLongVolatile(bools, 7, 0);
      }
    });

    try {
      var partialReads = 0;
      for (var index = 0; index < 1_000_000; index++) {
        var intValue = UnsafeReplacementDelegate.unsafeGetIntVolatile(bools, 1);
        var longValue = UnsafeReplacementDelegate.unsafeGetLongVolatile(bools, 7);
        if (intValue != 0 && intValue != 0x01010101) {
          partialReads++;
        }
        if (longValue != 0 && longValue != 0x0101010101010101L) {
          partialReads++;
        }
      }
      Assertions.assertEquals(0, partialReads);
    } finally {
      stop.set(true);
      writer.join();
    }
  }

  @Test
  void testEmulatedAtomicOperationsDoNotOverwriteConcurrentStores() throws InterruptedException {
    var bytes = new byte[16];
    var stop = new AtomicBoolean();
    var adder = Thread.ofPlatform().start(() -> {
      while (!stop.get()) {
        UnsafeReplacementDelegate.unsafeGetAndAddLong(bytes, 0, 1);
      }
    });

    try {
      var lostWrites = 0;
      for (var index = 0; index < 20_000; index++) {
        var sentinel = (long) index << 32;
        UnsafeReplacementDelegate.unsafePutLongVolatile(bytes, 0, sentinel);
        if (UnsafeReplacementDelegate.unsafeGetLongVolatile(bytes, 0) < sentinel) {
          lostWrites++;
        }
      }
      Assertions.assertEquals(0, lostWrites);
    } finally {
      stop.set(true);
      adder.join();
    }
  }

  @Test
  void testByteArrayMultiByteAccess() {
    var bytes = new byte[16];
    UnsafeReplacementDelegate.unsafePutLong(bytes, 3, 0x0102030405060708L);
    Assertions.assertEquals(0x0102030405060708L, UnsafeReplacementDelegate.unsafeGetLong(bytes, 3));
    UnsafeReplacementDelegate.unsafePutLongVolatile(bytes, 8, -5L);
    Assertions.assertEquals(-5L, UnsafeReplacementDelegate.unsafeGetLongVolatile(bytes, 8));
    UnsafeReplacementDelegate.unsafePutOrderedInt(bytes, 1, 1337);
    Assertions.assertEquals(1337, UnsafeReplacementDelegate.unsafeGetIntVolatile(bytes, 1));
    Assertions.assertTrue(UnsafeReplacementDelegate.unsafeCasInt(bytes, 1, 1337, 42));
    Assertions.assertEquals(42, UnsafeReplacementDelegate.unsafeGetAndSetInt(bytes, 1, 7));
    Assertions.assertEquals(7, UnsafeReplacementDelegate.unsafeGetInt(bytes, 1));
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeCasLong(bytes, 12, 0, 1));
    Assertions.assertEquals(0L, UnsafeReplacementDelegate.unsafeGetLong(bytes, 12));
  }

  @Test
  void testHugeArrayOffsetsAreOutOfBounds() {
    var hugeOffset = 1L << 40;
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeCasInt(new int[4], hugeOffset, 0, 1));
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndAddLong(new long[4], hugeOffset, 1));
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeGetAndSetObject(new Object[4], hugeOffset, null));
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeCasInt(new byte[4], hugeOffset, 0, 1));

    // boolean arrays are bounds checked before any element is written
    var bools = new boolean[4];
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeSetMemory(bools, 2, 4, (byte) 1));
    Assertions.assertArrayEquals(new boolean[4], bools);
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeSetMemory(bools, hugeOffset, 1, (byte) 1));
    UnsafeReplacementDelegate.unsafeSetMemory(bools, 1, 2, (byte) 1);
    Assertions.assertArrayEquals(new boolean[]{false, true, true, false}, bools);
  }

  @Test
  void testCopyOffHeapMemoryIntoBooleanArrayWithOffset() {
    var address = UnsafeReplacementDelegate.unsafeAllocateMemory(4);
    try {
      for (var index = 0; index < 4; index++) {
        UnsafeReplacementDelegate.unsafePutByte(address + index, (byte) (index % 2));
      }

      var target = new boolean[]{true, true, true, true, true, true, true, true};
      UnsafeReplacementDelegate.unsafeCopyMemory(null, address, target, 4, 4);
      Assertions.assertArrayEquals(new boolean[]{true, true, true, true, false, true, false, true}, target);
    } finally {
      UnsafeReplacementDelegate.unsafeFreeMemory(address);
    }
  }

  @Test
  void testEmulatedPlainWritesAreNotLostDuringReadModifyWriteOperations() throws InterruptedException {
    // wider than the array element: emulated access, plain writes must use the lock as well
    Assertions.assertEquals(0, countLostPlainWrites(new byte[32], 8));
    Assertions.assertEquals(0, countLostPlainWrites(new byte[32], 1));
    Assertions.assertEquals(0, countLostPlainWrites(new short[16], 8));
    Assertions.assertEquals(0, countLostPlainWrites(new int[8], 4));
  }

  @FunctionalInterface
  private interface MemoryReader {

    Object read(Object base, long offset);
  }

  @FunctionalInterface
  private interface MemoryWriter {

    void write(Object base, long offset, Object value);
  }

  @FunctionalInterface
  private interface MemoryAddressWriter {

    void write(long address, Object value);
  }

  // a primitive value kind with its matching array type and all delegate accessors for it. the readers are the plain
  // and volatile getters, the writers are the plain, volatile and (if available) ordered setters that take a base
  // object. the address readers and writers are the overloads that only take an off-heap address
  private record MemoryAccessKind(
    String name,
    int byteSize,
    Object value,
    IntFunction<Object> newArray,
    List<MemoryReader> readers,
    List<MemoryWriter> writers,
    List<LongFunction<Object>> addressReaders,
    List<MemoryAddressWriter> addressWriters
  ) {

  }
  //</editor-fold>

  @Test
  void testDefineClass() {
    var className = "eu.cloudnetservice.wrapper.test.TestClass";
    var classData = ClassFile.of().build(
      ClassDesc.of(className),
      classBuilder -> classBuilder.withMethod(
        "test",
        MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_short),
        AccessFlag.PUBLIC.mask() | AccessFlag.STATIC.mask(),
        methodBuilder -> methodBuilder.withCode(codeBuilder -> codeBuilder
          .iconst_5()
          .iload(0)
          .iadd()
          .ireturn())));
    var classLoader = UnsafeReplacementDelegateTest.class.getClassLoader();
    var protectionDomain = new ProtectionDomain(null, null);
    var clazz = UnsafeReplacementDelegate.unsafeDefineClass(
      className,
      classData,
      0,
      classData.length,
      classLoader,
      protectionDomain);
    Assertions.assertEquals(classLoader, clazz.getClassLoader());
    Assertions.assertEquals(protectionDomain, clazz.getProtectionDomain());

    var method = Assertions.assertDoesNotThrow(() -> clazz.getMethod("test", short.class));
    var result = Assertions.assertDoesNotThrow(() -> (int) method.invoke(null, (short) 1332));
    Assertions.assertEquals(1337, result);
  }

  @Test
  void testInvokeCleaner() {
    // can't really test if it worked, but can at least test if the method handle init works
    {
      var buffer = ByteBuffer.allocate(1);
      var thrown = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
      Assertions.assertNotNull(thrown.getMessage());
      Assertions.assertEquals("buffer is non-direct", thrown.getMessage());
    }

    {
      var buffer = ByteBuffer.allocateDirect(5);
      var bufferSlice = buffer.slice(2, 2);
      var thrown = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeInvokeCleaner(bufferSlice));
      Assertions.assertNotNull(thrown.getMessage());
      Assertions.assertEquals("duplicate or slice", thrown.getMessage());
      Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
    }

    {
      var cleanerConsumer = UnsafeReplacementDelegate.BB_CLEANER.get();
      Assertions.assertNotSame(UnsafeReplacementDelegate.BB_CLEANER_NOOP, cleanerConsumer);
    }
  }

  @Test
  void testLoadAvg() {
    var loadAvg = new double[3];
    var ret = UnsafeReplacementDelegate.unsafeGetLoadAverage(loadAvg, 3);
    if (ret != -1) {
      Assertions.assertEquals(1, ret);
      Assertions.assertTrue(loadAvg[0] >= 0);
      Assertions.assertTrue(loadAvg[0] <= 100);
      Assertions.assertEquals(0.0D, loadAvg[1]);
      Assertions.assertEquals(0.0D, loadAvg[2]);
    }

    // no requested samples writes nothing
    var untouched = new double[]{-2.0D};
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetLoadAverage(untouched, 0));
    Assertions.assertEquals(-2.0D, untouched[0]);
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetLoadAverage(new double[0], 0));
  }

  @Test
  void testThreadParkingAbsolute() throws InterruptedException {
    miscAssertParkedUntilUnpark(
      Thread.ofPlatform().daemon(true), true, System.currentTimeMillis() + 30_000L, Thread.State.TIMED_WAITING);
  }

  @Test
  void testThreadParkingNanos() throws InterruptedException {
    miscAssertParkedUntilUnpark(
      Thread.ofPlatform().daemon(true), false, TimeUnit.SECONDS.toNanos(30), Thread.State.TIMED_WAITING);
  }

  @Test
  void testEnsureClassInitialized() {
    final class Test {

      static {
        // tricks the compiler into thinking that this initializer can complete normally
        if (true) {
          throw new IllegalStateException("init called");
        }
      }
    }

    var thrown = Assertions.assertThrows(
      ExceptionInInitializerError.class,
      () -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(Test.class));
    Assertions.assertNotNull(thrown.getCause());
    var ise = Assertions.assertInstanceOf(IllegalStateException.class, thrown.getCause());
    Assertions.assertEquals("init called", ise.getMessage());
  }

  //<editor-fold defaultstate="collapsed" desc="Misc Operation Tests">
  // classes whose static initializer ran, reported by the static initializers of generated classes
  private static final List<Class<?>> MISC_INITIALIZED_CLASSES = new CopyOnWriteArrayList<>();
  private static final ClassDesc MISC_TEST_CLASS_DESC = ClassDesc.of(UnsafeReplacementDelegateTest.class.getName());

  // called from the static initializer of classes generated by miscRecordingInitializer
  public static void miscRecordClassInitialization(Class<?> type) {
    MISC_INITIALIZED_CLASSES.add(type);
  }

  // private member that is only accessible for nestmates of this class
  private static int miscNestmateSecret() {
    return 1337;
  }

  private static int miscInitializationCount(Class<?> type) {
    return Collections.frequency(MISC_INITIALIZED_CLASSES, type);
  }

  // static initializer that reports the initialization of the class with the given name
  private static Consumer<CodeBuilder> miscRecordingInitializer(String className) {
    return codeBuilder -> codeBuilder
      .ldc(ClassDesc.of(className))
      .invokestatic(
        MISC_TEST_CLASS_DESC,
        "miscRecordClassInitialization",
        MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Class))
      .return_();
  }

  // static initializer that always throws an IllegalStateException
  private static Consumer<CodeBuilder> miscFailingInitializer() {
    var iseDesc = ClassDesc.of(IllegalStateException.class.getName());
    return codeBuilder -> codeBuilder
      .new_(iseDesc)
      .dup()
      .ldc("misc init failure")
      .invokespecial(iseDesc, ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String))
      .athrow();
  }

  // generates a public class or interface with a static 'value' method returning 42 and an optional static initializer
  private static byte[] miscGenerateClass(String className, boolean isInterface, Consumer<CodeBuilder> initializer) {
    return ClassFile.of().build(ClassDesc.of(className), classBuilder -> {
      classBuilder.withFlags(isInterface
        ? ClassFile.ACC_PUBLIC | ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT
        : ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL);
      classBuilder.withMethod(
        "value",
        MethodTypeDesc.of(ConstantDescs.CD_int),
        ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
        methodBuilder -> methodBuilder.withCode(codeBuilder -> codeBuilder.bipush(42).ireturn()));
      if (initializer != null) {
        classBuilder.withMethod(
          ConstantDescs.CLASS_INIT_NAME,
          ConstantDescs.MTD_void,
          ClassFile.ACC_STATIC,
          methodBuilder -> methodBuilder.withCode(initializer));
      }
    });
  }

  // generates a class with a static 'secret' method that returns the value of the private miscNestmateSecret method
  private static byte[] miscGenerateNestmateClass(String className) {
    return ClassFile.of().build(ClassDesc.of(className), classBuilder -> classBuilder
      .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL)
      .withMethod(
        "secret",
        MethodTypeDesc.of(ConstantDescs.CD_int),
        ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
        methodBuilder -> methodBuilder.withCode(codeBuilder -> codeBuilder
          .invokestatic(MISC_TEST_CLASS_DESC, "miscNestmateSecret", MethodTypeDesc.of(ConstantDescs.CD_int))
          .ireturn()))
      .withMethod(
        ConstantDescs.CLASS_INIT_NAME,
        ConstantDescs.MTD_void,
        ClassFile.ACC_STATIC,
        methodBuilder -> methodBuilder.withCode(miscRecordingInitializer(className))));
  }

  // defines the given class data in a new class loader without initializing the class
  private static Class<?> miscDefineInNewLoader(byte[] classData) {
    var classLoader = new ClassLoader(UnsafeReplacementDelegateTest.class.getClassLoader()) {
      Class<?> define() {
        return this.defineClass(null, classData, 0, classData.length);
      }
    };
    return classLoader.define();
  }

  private static ClassLoader miscNewClassLoader() {
    return new ClassLoader(UnsafeReplacementDelegateTest.class.getClassLoader()) {
    };
  }

  // waits until the given thread reaches the given state, fails if the thread terminates or the state is not reached
  private static void miscAwaitThreadState(Thread thread, Thread.State state) throws InterruptedException {
    var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    while (thread.getState() != state) {
      Assertions.assertNotEquals(Thread.State.TERMINATED, thread.getState());
      Assertions.assertTrue(System.nanoTime() < deadline, () -> "thread did not reach state " + state);
      Thread.sleep(1);
    }
  }

  // runs the given task in a new thread and waits for it to complete, rethrowing assertion failures of the task
  private static void miscRunInThread(Thread.Builder builder, Runnable task) throws InterruptedException {
    var failure = new AtomicReference<Throwable>();
    var thread = builder.uncaughtExceptionHandler((_, throwable) -> failure.set(throwable)).start(task);
    if (!thread.join(Duration.ofSeconds(15))) {
      thread.interrupt();
      Assertions.fail("task did not complete in time");
    }

    if (failure.get() != null) {
      Assertions.fail(failure.get());
    }
  }

  // parks a new thread until released and unparked, retrying after spurious returns
  private static void miscAssertParkedUntilUnpark(
    Thread.Builder builder,
    boolean isAbsolute,
    long time,
    Thread.State parkedState
  ) throws InterruptedException {
    var released = new AtomicBoolean();
    var parkReturned = new AtomicBoolean();
    var thread = builder.start(() -> {
      while (!released.get()) {
        UnsafeReplacementDelegate.unsafePark(isAbsolute, time);
      }
      parkReturned.set(true);
    });

    miscAwaitThreadState(thread, parkedState);
    Assertions.assertFalse(parkReturned.get());
    released.set(true);
    UnsafeReplacementDelegate.unsafeUnpark(thread);
    Assertions.assertTrue(thread.join(Duration.ofSeconds(15)));
    Assertions.assertTrue(parkReturned.get());
  }

  // parks a new thread with the given arguments and ensures that interrupting it returns from park
  private static void miscAssertInterruptWakesPark(
    Thread.Builder builder,
    boolean isAbsolute,
    long time,
    Thread.State parkedState
  ) throws InterruptedException {
    var parkReturned = new AtomicBoolean();
    var interruptedAfterPark = new AtomicBoolean();
    var thread = builder.start(() -> {
      do {
        UnsafeReplacementDelegate.unsafePark(isAbsolute, time);
      } while (!Thread.currentThread().isInterrupted());
      interruptedAfterPark.set(Thread.currentThread().isInterrupted());
      parkReturned.set(true);
    });

    miscAwaitThreadState(thread, parkedState);
    thread.interrupt();
    Assertions.assertTrue(thread.join(Duration.ofSeconds(15)));
    Assertions.assertTrue(parkReturned.get());
    Assertions.assertTrue(interruptedAfterPark.get()); // park does not clear the interrupt status
  }

  @Test
  void testFenceOperations() {
    Assertions.assertDoesNotThrow(UnsafeReplacementDelegate::unsafeLoadFence);
    Assertions.assertDoesNotThrow(UnsafeReplacementDelegate::unsafeStoreFence);
    Assertions.assertDoesNotThrow(UnsafeReplacementDelegate::unsafeFullFence);
  }

  @Test
  void testAddressSize() {
    var addressSize = UnsafeReplacementDelegate.unsafeAddressSize();
    Assertions.assertEquals(ValueLayout.ADDRESS.byteSize(), addressSize);
    Assertions.assertEquals(addressSize, UnsafeReplacementDelegate.UNSAFE_ADDRESS_SIZE);
  }

  @Test
  void testParkWithZeroRelativeTimeParksIndefinitely() throws InterruptedException {
    var released = new AtomicBoolean();
    var parkReturned = new AtomicBoolean();
    var thread = Thread.ofPlatform().daemon(true).start(() -> {
      while (!released.get()) {
        UnsafeReplacementDelegate.unsafePark(false, 0L);
      }
      parkReturned.set(true);
    });

    // wait until the thread parked, then ensure that it keeps waiting for release
    miscAwaitThreadState(thread, Thread.State.WAITING);
    Thread.sleep(250);
    Assertions.assertFalse(parkReturned.get());
    miscAwaitThreadState(thread, Thread.State.WAITING);

    released.set(true);
    UnsafeReplacementDelegate.unsafeUnpark(thread);
    Assertions.assertTrue(thread.join(Duration.ofSeconds(15)));
    Assertions.assertTrue(parkReturned.get());
  }

  @Test
  void testParkUntilUnparked() throws InterruptedException {
    var farFutureMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
    var builder = Thread.ofPlatform().daemon(true);
    miscAssertParkedUntilUnpark(builder, false, 0L, Thread.State.WAITING);
    miscAssertParkedUntilUnpark(builder, false, TimeUnit.MINUTES.toNanos(5), Thread.State.TIMED_WAITING);
    miscAssertParkedUntilUnpark(builder, true, farFutureMillis, Thread.State.TIMED_WAITING);
  }

  @Test
  void testParkReturnsAfterTimeout() throws InterruptedException {
    // relative time in nanoseconds
    miscRunInThread(
      Thread.ofPlatform().daemon(true),
      () -> UnsafeReplacementDelegate.unsafePark(false, TimeUnit.MILLISECONDS.toNanos(50)));

    // absolute deadline in epoch millis
    miscRunInThread(
      Thread.ofPlatform().daemon(true),
      () -> UnsafeReplacementDelegate.unsafePark(true, System.currentTimeMillis() + 50L));
  }

  @Test
  void testParkWithExpiredDeadlineReturnsImmediately() throws InterruptedException {
    // negative relative time
    miscRunInThread(Thread.ofPlatform().daemon(true), () -> UnsafeReplacementDelegate.unsafePark(false, -1L));
    miscRunInThread(Thread.ofPlatform().daemon(true), () -> UnsafeReplacementDelegate.unsafePark(false, Long.MIN_VALUE));

    // absolute deadline at or before the epoch
    miscRunInThread(Thread.ofPlatform().daemon(true), () -> UnsafeReplacementDelegate.unsafePark(true, 0L));
    miscRunInThread(Thread.ofPlatform().daemon(true), () -> UnsafeReplacementDelegate.unsafePark(true, -1L));

    // absolute deadline in the past
    miscRunInThread(
      Thread.ofPlatform().daemon(true),
      () -> UnsafeReplacementDelegate.unsafePark(true, System.currentTimeMillis() - 10_000L));
  }

  @Test
  void testParkConsumesPermitFromPrecedingUnpark() throws InterruptedException {
    var farFutureMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
    miscRunInThread(Thread.ofPlatform().daemon(true), () -> {
      UnsafeReplacementDelegate.unsafeUnpark(Thread.currentThread());
      UnsafeReplacementDelegate.unsafePark(false, 0L);
      UnsafeReplacementDelegate.unsafeUnpark(Thread.currentThread());
      UnsafeReplacementDelegate.unsafePark(false, TimeUnit.MINUTES.toNanos(5));
      UnsafeReplacementDelegate.unsafeUnpark(Thread.currentThread());
      UnsafeReplacementDelegate.unsafePark(true, farFutureMillis);
    });
  }

  @Test
  void testInterruptWakesParkedThread() throws InterruptedException {
    var farFutureMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
    var builder = Thread.ofPlatform().daemon(true);
    miscAssertInterruptWakesPark(builder, false, 0L, Thread.State.WAITING);
    miscAssertInterruptWakesPark(builder, false, TimeUnit.MINUTES.toNanos(5), Thread.State.TIMED_WAITING);
    miscAssertInterruptWakesPark(builder, true, farFutureMillis, Thread.State.TIMED_WAITING);

    // already interrupted thread does not park
    miscRunInThread(Thread.ofPlatform().daemon(true), () -> {
      Thread.currentThread().interrupt();
      UnsafeReplacementDelegate.unsafePark(false, 0L);
      Assertions.assertTrue(Thread.currentThread().isInterrupted());
    });
  }

  @Test
  void testUnparkIgnoresNonThreadArguments() throws InterruptedException {
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeUnpark(null));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeUnpark(new Object()));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeUnpark("thread"));

    // unstarted and terminated threads
    var thread = Thread.ofPlatform().daemon(true).unstarted(() -> {
    });
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeUnpark(thread));
    thread.start();
    Assertions.assertTrue(thread.join(Duration.ofSeconds(15)));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeUnpark(thread));

    // non-thread arguments do not make a permit available for the calling thread
    var released = new AtomicBoolean();
    var parkingThread = Thread.ofPlatform().daemon(true).start(() -> {
      UnsafeReplacementDelegate.unsafeUnpark(null);
      UnsafeReplacementDelegate.unsafeUnpark(new Object());
      while (!released.get()) {
        UnsafeReplacementDelegate.unsafePark(false, 0L);
      }
    });
    miscAwaitThreadState(parkingThread, Thread.State.WAITING);
    released.set(true);
    UnsafeReplacementDelegate.unsafeUnpark(parkingThread);
    Assertions.assertTrue(parkingThread.join(Duration.ofSeconds(15)));
  }

  @Test
  void testParkAndUnparkVirtualThreads() throws InterruptedException {
    var farFutureMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
    miscAssertParkedUntilUnpark(Thread.ofVirtual(), false, 0L, Thread.State.WAITING);
    miscAssertParkedUntilUnpark(Thread.ofVirtual(), false, TimeUnit.MINUTES.toNanos(5), Thread.State.TIMED_WAITING);
    miscAssertParkedUntilUnpark(Thread.ofVirtual(), true, farFutureMillis, Thread.State.TIMED_WAITING);
    miscAssertInterruptWakesPark(Thread.ofVirtual(), false, 0L, Thread.State.WAITING);

    // timeouts, expired deadlines and available permits
    miscRunInThread(Thread.ofVirtual(), () -> {
      UnsafeReplacementDelegate.unsafePark(false, TimeUnit.MILLISECONDS.toNanos(50));
      UnsafeReplacementDelegate.unsafePark(true, System.currentTimeMillis() + 50L);
      UnsafeReplacementDelegate.unsafePark(false, -1L);
      UnsafeReplacementDelegate.unsafePark(true, 0L);
      UnsafeReplacementDelegate.unsafeUnpark(Thread.currentThread());
      UnsafeReplacementDelegate.unsafePark(false, 0L);
    });
  }

  @Test
  void testShouldBeInitializedAlwaysReturnsFalse() {
    var className = "eu.cloudnetservice.wrapper.test.MiscShouldBeInitialized";
    var type = miscDefineInNewLoader(miscGenerateClass(className, false, miscRecordingInitializer(className)));

    // uninitialized class, does not trigger the initialization
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeShouldBeInitialized(type));
    Assertions.assertEquals(0, miscInitializationCount(type));

    // initialized class
    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type);
    Assertions.assertEquals(1, miscInitializationCount(type));
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeShouldBeInitialized(type));

    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeShouldBeInitialized(String.class));
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeShouldBeInitialized(int.class));
    Assertions.assertFalse(UnsafeReplacementDelegate.unsafeShouldBeInitialized(int[].class));
  }

  @Test
  void testEnsureClassInitializedRunsStaticInitializerOnce() {
    var className = "eu.cloudnetservice.wrapper.test.MiscEnsureInitializedOnce";
    var type = miscDefineInNewLoader(miscGenerateClass(className, false, miscRecordingInitializer(className)));
    Assertions.assertEquals(0, miscInitializationCount(type));

    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type);
    Assertions.assertEquals(1, miscInitializationCount(type));
    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type);
    Assertions.assertEquals(1, miscInitializationCount(type));
  }

  @Test
  void testEnsureClassInitializedInitializesInterfaces() {
    var className = "eu.cloudnetservice.wrapper.test.MiscEnsureInitializedInterface";
    var type = miscDefineInNewLoader(miscGenerateClass(className, true, miscRecordingInitializer(className)));
    Assertions.assertTrue(type.isInterface());
    Assertions.assertEquals(0, miscInitializationCount(type));

    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type);
    Assertions.assertEquals(1, miscInitializationCount(type));
    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type);
    Assertions.assertEquals(1, miscInitializationCount(type));

    // interface without static initializer
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(Runnable.class));
  }

  @Test
  void testEnsureClassInitializedIgnoresPrimitivesAndArrays() {
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(int.class));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(void.class));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(long[].class));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(String[][].class));

    // array of an uninitialized class does not initialize the component type
    var className = "eu.cloudnetservice.wrapper.test.MiscEnsureInitializedArrayComponent";
    var type = miscDefineInNewLoader(miscGenerateClass(className, false, miscRecordingInitializer(className)));
    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type.arrayType());
    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type.arrayType().arrayType());
    Assertions.assertEquals(0, miscInitializationCount(type));

    // array of a class with a failing initializer
    var failingName = "eu.cloudnetservice.wrapper.test.MiscEnsureInitializedFailingArrayComponent";
    var failingType = miscDefineInNewLoader(miscGenerateClass(failingName, false, miscFailingInitializer()));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(failingType.arrayType()));
  }

  @Test
  void testEnsureClassInitializedOnInitializedClasses() {
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(Object.class));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(String.class));
    Assertions.assertDoesNotThrow(
      () -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(UnsafeReplacementDelegateTest.class));
    Assertions.assertDoesNotThrow(
      () -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(UnsafeReplacementDelegate.class));
  }

  @Test
  void testEnsureClassInitializedRejectsNull() {
    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(null));
  }

  @Test
  void testEnsureClassInitializedWithFailingInitializer() {
    var className = "eu.cloudnetservice.wrapper.test.MiscEnsureInitializedFailing";
    var type = miscDefineInNewLoader(miscGenerateClass(className, false, miscFailingInitializer()));

    // first attempt reports the exception thrown by the initializer
    var thrown = Assertions.assertThrows(
      ExceptionInInitializerError.class,
      () -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type));
    var ise = Assertions.assertInstanceOf(IllegalStateException.class, thrown.getCause());
    Assertions.assertEquals("misc init failure", ise.getMessage());

    // the class is in an erroneous state afterward
    var secondThrown = Assertions.assertThrows(
      NoClassDefFoundError.class,
      () -> UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type));
    Assertions.assertNotNull(secondThrown.getMessage());
    Assertions.assertTrue(secondThrown.getMessage().contains(className));
  }

  @Test
  void testEnsureClassInitializedOnHiddenClass() throws IllegalAccessException {
    var className = UnsafeReplacementDelegateTest.class.getPackageName() + ".MiscEnsureInitializedHidden";
    var classData = miscGenerateClass(className, false, miscRecordingInitializer(className));
    var type = MethodHandles.lookup().defineHiddenClass(classData, false).lookupClass();
    Assertions.assertTrue(type.isHidden());
    Assertions.assertEquals(0, miscInitializationCount(type));

    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type);
    Assertions.assertEquals(1, miscInitializationCount(type));
    UnsafeReplacementDelegate.unsafeEnsureClassInitialized(type);
    Assertions.assertEquals(1, miscInitializationCount(type));
  }

  @Test
  void testDefineClassInCustomClassLoader() throws ReflectiveOperationException {
    var className = "eu.cloudnetservice.wrapper.test.MiscDefinedInCustomLoader";
    var classData = miscGenerateClass(className, false, miscRecordingInitializer(className));
    var classLoader = miscNewClassLoader();
    var type = UnsafeReplacementDelegate.unsafeDefineClass(
      className,
      classData,
      0,
      classData.length,
      classLoader,
      null);
    Assertions.assertEquals(className, type.getName());
    Assertions.assertSame(classLoader, type.getClassLoader());
    Assertions.assertFalse(type.isHidden());
    Assertions.assertEquals(0, miscInitializationCount(type)); // defining does not initialize
    Assertions.assertSame(type, Class.forName(className, false, classLoader));
    Assertions.assertEquals(42, type.getMethod("value").invoke(null));
    Assertions.assertEquals(1, miscInitializationCount(type));
    Assertions.assertThrows(
      ClassNotFoundException.class,
      () -> Class.forName(className, false, UnsafeReplacementDelegateTest.class.getClassLoader()));

    // the same class can be defined in another class loader
    var otherClassLoader = miscNewClassLoader();
    var otherType = UnsafeReplacementDelegate.unsafeDefineClass(
      className,
      classData,
      0,
      classData.length,
      otherClassLoader,
      null);
    Assertions.assertNotSame(type, otherType);
    Assertions.assertSame(otherClassLoader, otherType.getClassLoader());
  }

  @Test
  void testDefineClassInBootstrapClassLoader() throws ReflectiveOperationException {
    var className = "eu.cloudnetservice.wrapper.test.MiscDefinedInBootstrapLoader";
    var classData = miscGenerateClass(className, false, null);
    var type = UnsafeReplacementDelegate.unsafeDefineClass(className, classData, 0, classData.length, null, null);
    Assertions.assertEquals(className, type.getName());
    Assertions.assertNull(type.getClassLoader());
    Assertions.assertSame(type, Class.forName(className, false, null));
    Assertions.assertEquals(42, type.getMethod("value").invoke(null));
  }

  @Test
  void testDefineClassFromArrayRegion() throws ReflectiveOperationException {
    var className = "eu.cloudnetservice.wrapper.test.MiscDefinedFromArrayRegion";
    var classData = miscGenerateClass(className, false, null);
    var paddedData = new byte[classData.length + 64];
    Arrays.fill(paddedData, (byte) 0x7F);
    System.arraycopy(classData, 0, paddedData, 32, classData.length);

    var type = UnsafeReplacementDelegate.unsafeDefineClass(
      className,
      paddedData,
      32,
      classData.length,
      miscNewClassLoader(),
      null);
    Assertions.assertEquals(className, type.getName());
    Assertions.assertEquals(42, type.getMethod("value").invoke(null));
  }

  @Test
  void testDefineClassProtectionDomain() throws Exception {
    // given protection domain is used
    {
      var className = "eu.cloudnetservice.wrapper.test.MiscDefinedWithProtectionDomain";
      var classData = miscGenerateClass(className, false, null);
      var codeSource = new CodeSource(URI.create("file:/misc/defined/").toURL(), (Certificate[]) null);
      var protectionDomain = new ProtectionDomain(codeSource, null);
      var type = UnsafeReplacementDelegate.unsafeDefineClass(
        className,
        classData,
        0,
        classData.length,
        miscNewClassLoader(),
        protectionDomain);
      Assertions.assertSame(protectionDomain, type.getProtectionDomain());
      Assertions.assertSame(codeSource, type.getProtectionDomain().getCodeSource());
    }

    // no protection domain results in a default domain without code source
    {
      var className = "eu.cloudnetservice.wrapper.test.MiscDefinedWithoutProtectionDomain";
      var classData = miscGenerateClass(className, false, null);
      var type = UnsafeReplacementDelegate.unsafeDefineClass(
        className,
        classData,
        0,
        classData.length,
        miscNewClassLoader(),
        null);
      Assertions.assertNotNull(type.getProtectionDomain());
      Assertions.assertNull(type.getProtectionDomain().getCodeSource());
    }
  }

  @Test
  void testDefineClassWithoutNameOrWithInternalName() {
    // name is taken from the class data
    {
      var className = "eu.cloudnetservice.wrapper.test.MiscDefinedWithoutName";
      var classData = miscGenerateClass(className, false, null);
      var type = UnsafeReplacementDelegate.unsafeDefineClass(
        null,
        classData,
        0,
        classData.length,
        miscNewClassLoader(),
        null);
      Assertions.assertEquals(className, type.getName());
    }

    // name using '/' as package separator
    {
      var className = "eu.cloudnetservice.wrapper.test.MiscDefinedWithInternalName";
      var classData = miscGenerateClass(className, false, null);
      var type = UnsafeReplacementDelegate.unsafeDefineClass(
        className.replace('.', '/'),
        classData,
        0,
        classData.length,
        miscNewClassLoader(),
        null);
      Assertions.assertEquals(className, type.getName());
    }
  }

  @Test
  void testDefineClassRethrowsLinkageErrors() {
    var className = "eu.cloudnetservice.wrapper.test.DuplicateTestClass";
    var classData = ClassFile.of().build(ClassDesc.of(className), _ -> {
    });

    var classLoader = new ClassLoader(UnsafeReplacementDelegateTest.class.getClassLoader()) {
    };
    var protectionDomain = new ProtectionDomain(null, null);
    UnsafeReplacementDelegate.unsafeDefineClass(
      className,
      classData,
      0,
      classData.length,
      classLoader,
      protectionDomain);
    Assertions.assertThrows(
      LinkageError.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(
        className,
        classData,
        0,
        classData.length,
        classLoader,
        protectionDomain));
  }

  @Test
  void testDefineClassRejectsMalformedClassData() {
    var className = "eu.cloudnetservice.wrapper.test.MiscDefinedFromMalformedData";
    var classData = miscGenerateClass(className, false, null);
    var classLoader = miscNewClassLoader();

    // invalid magic
    var invalidData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    Assertions.assertThrows(
      ClassFormatError.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(className, invalidData, 0, invalidData.length, classLoader, null));

    // truncated class data
    Assertions.assertThrows(
      ClassFormatError.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(className, classData, 0, classData.length / 2, classLoader, null));

    // the failed attempts did not define the class
    var type = UnsafeReplacementDelegate.unsafeDefineClass(className, classData, 0, classData.length, classLoader, null);
    Assertions.assertEquals(className, type.getName());
  }

  @Test
  void testDefineClassRejectsMismatchingName() {
    var className = "eu.cloudnetservice.wrapper.test.MiscDefinedWithMismatchingName";
    var classData = miscGenerateClass(className, false, null);
    var classLoader = miscNewClassLoader();
    Assertions.assertThrows(
      NoClassDefFoundError.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(
        className + "Other",
        classData,
        0,
        classData.length,
        classLoader,
        null));

    // the failed attempt did not define the class
    var type = UnsafeReplacementDelegate.unsafeDefineClass(className, classData, 0, classData.length, classLoader, null);
    Assertions.assertEquals(className, type.getName());
  }

  @Test
  void testDefineClassRejectsInvalidArrayRegion() {
    var className = "eu.cloudnetservice.wrapper.test.MiscDefinedFromInvalidRegion";
    var classData = miscGenerateClass(className, false, null);
    var classLoader = miscNewClassLoader();

    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(className, null, 0, 0, classLoader, null));
    Assertions.assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(className, classData, 0, -1, classLoader, null));
    Assertions.assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(className, classData, -1, classData.length, classLoader, null));
    Assertions.assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(className, classData, 1, classData.length, classLoader, null));
    Assertions.assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(className, classData, 0, classData.length + 1, classLoader, null));
  }

  @Test
  void testDefineClassRejectsProhibitedPackage() {
    var className = "java.misc.MiscProhibitedPackage";
    var classData = miscGenerateClass(className, false, null);
    Assertions.assertThrows(
      SecurityException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineClass(
        className,
        classData,
        0,
        classData.length,
        miscNewClassLoader(),
        null));
  }

  @Test
  void testDefineAnonymousClassDefinesInitializedHiddenNestmate() throws ReflectiveOperationException {
    var host = UnsafeReplacementDelegateTest.class;
    var className = host.getPackageName() + ".MiscAnonymousNestmate";
    var classData = miscGenerateNestmateClass(className);

    var type = UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, classData, null);
    Assertions.assertTrue(type.isHidden());
    Assertions.assertTrue(type.getName().startsWith(className + "/"));
    Assertions.assertSame(host.getClassLoader(), type.getClassLoader());
    Assertions.assertEquals(host.getPackageName(), type.getPackageName());
    Assertions.assertSame(host, type.getNestHost());
    Assertions.assertTrue(host.isNestmateOf(type));
    Assertions.assertEquals(1, miscInitializationCount(type));
    Assertions.assertEquals(1337, type.getMethod("secret").invoke(null));
    Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName(className, false, host.getClassLoader()));

    // each definition results in a new class
    var otherType = UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, classData, null);
    Assertions.assertNotSame(type, otherType);
    Assertions.assertTrue(otherType.isHidden());
    Assertions.assertEquals(1, miscInitializationCount(otherType));
    Assertions.assertEquals(1, miscInitializationCount(type));
  }

  @Test
  void testDefineAnonymousClassWithNestedHost() throws ReflectiveOperationException {
    var host = MiscAnonymousClassHost.class;
    var className = host.getPackageName() + ".MiscAnonymousNestedHostNestmate";
    var type = UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, miscGenerateNestmateClass(className), null);
    Assertions.assertTrue(type.isHidden());
    Assertions.assertSame(UnsafeReplacementDelegateTest.class, type.getNestHost());
    Assertions.assertTrue(host.isNestmateOf(type));
    Assertions.assertEquals(1337, type.getMethod("secret").invoke(null));
  }

  @Test
  void testDefineAnonymousClassIgnoresPatches() throws ReflectiveOperationException {
    var host = UnsafeReplacementDelegateTest.class;
    var className = host.getPackageName() + ".MiscAnonymousWithPatches";
    var classData = miscGenerateClass(className, false, null);
    var patches = new Object[]{"patch", 1337, null};

    var type = UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, classData, patches);
    Assertions.assertTrue(type.isHidden());
    Assertions.assertEquals(42, type.getMethod("value").invoke(null));
    var emptyPatchesType = UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, classData, new Object[0]);
    Assertions.assertEquals(42, emptyPatchesType.getMethod("value").invoke(null));
  }

  @Test
  void testDefineAnonymousClassRequiresPackageOfHost() {
    var host = UnsafeReplacementDelegateTest.class;

    // class in another package
    var otherPackageData = miscGenerateClass("eu.cloudnetservice.wrapper.test.MiscAnonymousOtherPackage", false, null);
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, otherPackageData, null));

    // class in the unnamed package
    var unnamedPackageData = miscGenerateClass("MiscAnonymousUnnamedPackage", false, null);
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, unnamedPackageData, null));
  }

  @Test
  void testDefineAnonymousClassRejectsInvalidInput() {
    var host = UnsafeReplacementDelegateTest.class;
    var classData = miscGenerateClass(host.getPackageName() + ".MiscAnonymousInvalidInput", false, null);
    Assertions.assertThrows(
      ClassFormatError.class,
      () -> UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, null));
    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineAnonymousClass(host, null, null));
    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeDefineAnonymousClass(null, classData, null));
  }

  @Test
  void testInvokeCleanerOnDirectBufferTwice() {
    var buffer = ByteBuffer.allocateDirect(16);
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
  }

  @Test
  void testInvokeCleanerRejectsHeapBuffers() {
    var heapBuffers = List.of(
      ByteBuffer.allocate(16),
      ByteBuffer.wrap(new byte[16]),
      ByteBuffer.allocate(16).slice(2, 4),
      ByteBuffer.allocate(16).asReadOnlyBuffer());
    for (var buffer : heapBuffers) {
      var thrown = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
      Assertions.assertEquals("buffer is non-direct", thrown.getMessage());
    }
  }

  @Test
  void testInvokeCleanerRejectsDirectBufferViews() {
    var buffer = ByteBuffer.allocateDirect(16);
    var views = List.of(
      buffer.slice(),
      buffer.slice(2, 4),
      buffer.duplicate(),
      buffer.asReadOnlyBuffer(),
      buffer.duplicate().slice());
    for (var view : views) {
      var thrown = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeInvokeCleaner(view));
      Assertions.assertEquals("duplicate or slice", thrown.getMessage());
    }

    // the views did not clean the original buffer
    buffer.putLong(0, 1337L);
    Assertions.assertEquals(1337L, buffer.getLong(0));
    Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
  }

  @Test
  void testInvokeCleanerRejectsNull() {
    Assertions.assertThrows(
      NullPointerException.class,
      () -> UnsafeReplacementDelegate.unsafeInvokeCleaner(null));
  }

  @Test
  void testInvokeCleanerOnSegmentBackedBufferDoesNothing() {
    try (var arena = Arena.ofConfined()) {
      var segment = arena.allocate(8);
      var buffer = segment.asByteBuffer();
      Assertions.assertTrue(buffer.isDirect());
      Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));

      // the memory is still owned by the arena and accessible
      segment.set(ValueLayout.JAVA_BYTE, 0, (byte) 123);
      Assertions.assertEquals(123, buffer.get(0));
    }
  }

  @Test
  void testInvokeCleanerOnMappedBuffer(@TempDir Path tempDir) throws IOException {
    var file = Files.write(tempDir.resolve("misc-mapped-buffer"), new byte[16]);
    try (var channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      var buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 16);
      buffer.put(0, (byte) 123);

      var thrown = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer.slice(1, 4)));
      Assertions.assertEquals("duplicate or slice", thrown.getMessage());

      // unmaps the buffer
      Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
      Assertions.assertDoesNotThrow(() -> UnsafeReplacementDelegate.unsafeInvokeCleaner(buffer));
    }

    Assertions.assertEquals(123, Files.readAllBytes(file)[0]);
  }

  @Test
  void testGetLoadAverageSampleCounts() {
    var loadAverageAvailable = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() >= 0;
    for (var sampleCount = 1; sampleCount <= 3; sampleCount++) {
      var loadAverage = new double[]{-2.0D, -2.0D, -2.0D, -2.0D};
      var ret = UnsafeReplacementDelegate.unsafeGetLoadAverage(loadAverage, sampleCount);
      if (loadAverageAvailable) {
        // only the load average of the last minute is available
        Assertions.assertEquals(1, ret);
        Assertions.assertTrue(loadAverage[0] >= 0);
        Assertions.assertArrayEquals(new double[]{-2.0D, -2.0D, -2.0D}, Arrays.copyOfRange(loadAverage, 1, 4));
      } else {
        Assertions.assertEquals(-1, ret);
        Assertions.assertArrayEquals(new double[]{-2.0D, -2.0D, -2.0D, -2.0D}, loadAverage);
      }
    }

    // exact array length
    var exact = new double[1];
    Assertions.assertEquals(loadAverageAvailable ? 1 : -1, UnsafeReplacementDelegate.unsafeGetLoadAverage(exact, 1));
  }

  @Test
  void testGetLoadAverageWithZeroSamples() {
    var loadAverage = new double[]{-2.0D, -2.0D, -2.0D};
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetLoadAverage(loadAverage, 0));
    Assertions.assertArrayEquals(new double[]{-2.0D, -2.0D, -2.0D}, loadAverage);
    Assertions.assertEquals(0, UnsafeReplacementDelegate.unsafeGetLoadAverage(new double[0], 0));
  }

  @Test
  void testGetLoadAverageRejectsInvalidSampleCounts() {
    var loadAverage = new double[]{-2.0D, -2.0D, -2.0D, -2.0D};
    for (var sampleCount : new int[]{-1, 4, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      Assertions.assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> UnsafeReplacementDelegate.unsafeGetLoadAverage(loadAverage, sampleCount));
    }
    Assertions.assertArrayEquals(new double[]{-2.0D, -2.0D, -2.0D, -2.0D}, loadAverage);

    // more samples than the array can hold
    var smallLoadAverage = new double[]{-2.0D};
    Assertions.assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeGetLoadAverage(smallLoadAverage, 2));
    Assertions.assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeGetLoadAverage(new double[0], 1));
    Assertions.assertEquals(-2.0D, smallLoadAverage[0]);
  }

  @Test
  void testGetLoadAverageRejectsNullArray() {
    for (var sampleCount = 0; sampleCount <= 3; sampleCount++) {
      var count = sampleCount;
      Assertions.assertThrows(
        NullPointerException.class,
        () -> UnsafeReplacementDelegate.unsafeGetLoadAverage(null, count));
    }

    // the sample count is validated before the array is accessed
    Assertions.assertThrows(
      ArrayIndexOutOfBoundsException.class,
      () -> UnsafeReplacementDelegate.unsafeGetLoadAverage(null, -1));
  }

  private static final class MiscAnonymousClassHost {

  }
  //</editor-fold>
}
