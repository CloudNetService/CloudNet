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

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
public class UnsafeReplacementDelegateTest {

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
      var offset = switch (isStatic) {
        case true -> UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
        case false -> UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      };
      Assertions.assertTrue(offset >= 0);
      Assertions.assertTrue(offset <= 25);
      Assertions.assertTrue(seenOffsets.add(offset));

      var base = switch (isStatic) {
        case true -> UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
        case false -> inst;
      };
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
        var offset = switch (isStatic) {
          case true -> UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
          case false -> UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
        };
        Assertions.assertTrue(offset >= 0);
        Assertions.assertTrue(offset <= 250);
        Assertions.assertTrue(seenOffsets.add(offset));

        var base = switch (isStatic) {
          case true -> UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
          case false -> inst;
        };
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
      Assertions.assertEquals(1, inst.getFieldValues()[1]);
      UnsafeReplacementDelegate.unsafePutInt(base, offset, oldVal); // reset value
    }

    // write char into long field
    {
      var field = ClassWithFields.class.getDeclaredField("longField");
      var base = UnsafeReplacementDelegate.unsafeStaticFieldBase(field);
      var offset = UnsafeReplacementDelegate.unsafeStaticFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetLong(base, offset);
      UnsafeReplacementDelegate.unsafePutChar(base, offset, 'C');
      Assertions.assertEquals(67L, inst.getFieldValues()[2]);
      UnsafeReplacementDelegate.unsafePutLong(base, offset, oldVal); // reset value
    }

    // write long into boolean field
    {
      var field = ClassWithFields.class.getDeclaredField("instBoolField");
      var offset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetBoolean(inst, offset);
      UnsafeReplacementDelegate.unsafePutLong(inst, offset, 123456789987654321L);
      Assertions.assertEquals(false, inst.getFieldValues()[6]);
      UnsafeReplacementDelegate.unsafePutBoolean(inst, offset, oldVal); // reset value
    }

    // write boolean into char field
    {
      var field = ClassWithFields.class.getDeclaredField("instCharField");
      var offset = UnsafeReplacementDelegate.unsafeObjectFieldOffset(field);
      var oldVal = UnsafeReplacementDelegate.unsafeGetChar(inst, offset);
      UnsafeReplacementDelegate.unsafePutBoolean(inst, offset, true);
      Assertions.assertEquals('\1', inst.getFieldValues()[5]);
      UnsafeReplacementDelegate.unsafePutChar(inst, offset, oldVal); // reset value
    }
  }

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
  }

  @Test
  void testThreadParkingAbsolute() {
    var unparkIAmReady = new CountDownLatch(1);
    var thread = Thread.ofPlatform().daemon(false).start(() -> {
      unparkIAmReady.countDown();
      UnsafeReplacementDelegate.unsafePark(true, System.currentTimeMillis() + 30_000L);
    });
    Assertions.assertDoesNotThrow(() -> unparkIAmReady.await(15, TimeUnit.SECONDS)); // wait for the thread to start
    Assertions.assertDoesNotThrow(() -> Thread.sleep(1000)); // give some time to execute the next step
    Assertions.assertTrue(thread.isAlive());
    Assertions.assertEquals(Thread.State.TIMED_WAITING, thread.getState());
    UnsafeReplacementDelegate.unsafeUnpark(thread);
    Assertions.assertDoesNotThrow(() -> Thread.sleep(1000)); // give some time to unpark
    Assertions.assertEquals(Thread.State.TERMINATED, thread.getState());
  }

  @Test
  void testThreadParkingNanos() {
    var unparkIAmReady = new CountDownLatch(1);
    var thread = Thread.ofPlatform().daemon(false).start(() -> {
      unparkIAmReady.countDown();
      UnsafeReplacementDelegate.unsafePark(false, TimeUnit.SECONDS.toNanos(30));
    });
    Assertions.assertDoesNotThrow(() -> unparkIAmReady.await(15, TimeUnit.SECONDS)); // wait for the thread to start
    Assertions.assertDoesNotThrow(() -> Thread.sleep(1000)); // give some time to execute the next step
    Assertions.assertTrue(thread.isAlive());
    Assertions.assertEquals(Thread.State.TIMED_WAITING, thread.getState());
    UnsafeReplacementDelegate.unsafeUnpark(thread);
    Assertions.assertDoesNotThrow(() -> Thread.sleep(1000)); // give some time to unpark
    Assertions.assertEquals(Thread.State.TERMINATED, thread.getState());
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
}
