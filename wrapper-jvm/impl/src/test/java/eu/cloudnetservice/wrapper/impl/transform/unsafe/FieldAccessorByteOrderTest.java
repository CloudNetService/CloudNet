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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FieldAccessorByteOrderTest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testNarrowReadsAndWrites(boolean bigEndian) throws Exception {
    var order = bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    var instance = new Fields();
    for (var field : Fields.class.getDeclaredFields()) {
      var accessor = FieldAccessor.make(field, order);
      var kind = ValueTypeKind.of(field.getType());
      var bytes = kind.byteSize();
      var model = ByteBuffer.allocate(Long.BYTES).order(order);
      accessor.putLong(instance, 0x9122334455667788L, OpConstants.MODE_PLAIN);
      switch (bytes) {
        case 2 -> model.putShort(0, (short) 0x7788);
        case 4 -> model.putInt(0, 0x55667788);
        case 8 -> model.putLong(0, 0x9122334455667788L);
        default -> throw new AssertionError(bytes);
      }

      for (var mode = OpConstants.MODE_PLAIN; mode <= OpConstants.MODE_VOLATILE; mode++) {
        Assertions.assertEquals(model.get(0), accessor.getByte(instance, mode), field.toString());
        Assertions.assertEquals(model.getShort(0), accessor.getShort(instance, mode), field.toString());
        Assertions.assertEquals(model.getChar(0), accessor.getChar(instance, mode), field.toString());
        Assertions.assertEquals(model.get(0) != 0, accessor.getBoolean(instance, mode), field.toString());
        if (bytes >= Integer.BYTES) {
          Assertions.assertEquals(model.getInt(0), accessor.getInt(instance, mode), field.toString());
          Assertions.assertEquals(model.getFloat(0), accessor.getFloat(instance, mode), field.toString());
        }
      }

      for (var mode = OpConstants.MODE_PLAIN; mode <= OpConstants.MODE_RELEASE; mode++) {
        accessor.putByte(instance, (byte) -1, mode);
        model.put(0, (byte) -1);
        assertBits(field, instance, model);
        accessor.putBoolean(instance, false, mode);
        model.put(0, (byte) 0);
        assertBits(field, instance, model);
        accessor.putBoolean(instance, true, mode);
        model.put(0, (byte) 1);
        assertBits(field, instance, model);
        accessor.putShort(instance, (short) -2, mode);
        model.putShort(0, (short) -2);
        assertBits(field, instance, model);
        accessor.putChar(instance, (char) 0xABCD, mode);
        model.putChar(0, (char) 0xABCD);
        assertBits(field, instance, model);
        if (bytes >= Integer.BYTES) {
          accessor.putInt(instance, -3, mode);
          model.putInt(0, -3);
          assertBits(field, instance, model);
          accessor.putFloat(instance, -1.5F, mode);
          model.putFloat(0, -1.5F);
          assertBits(field, instance, model);
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testNarrowAtomicsPreserveOtherBytes(boolean bigEndian) throws Exception {
    var order = bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    var instance = new Fields();
    for (var field : Fields.class.getDeclaredFields()) {
      if (ValueTypeKind.of(field.getType()).byteSize() != Long.BYTES) {
        continue;
      }
      var accessor = FieldAccessor.make(field, order);
      var model = ByteBuffer.allocate(Long.BYTES).order(order);
      model.putLong(0, bigEndian ? 0xFFFFFFFF11223344L : 0x11223344FFFFFFFFL);
      accessor.putLong(instance, model.getLong(0), OpConstants.MODE_PLAIN);

      Assertions.assertFalse(accessor.casInt(instance, 0, 5), field.toString());
      assertBits(field, instance, model);
      Assertions.assertEquals(-1, accessor.getAndAddInt(instance, 1), field.toString());
      model.putInt(0, 0);
      assertBits(field, instance, model);
      Assertions.assertEquals(0, accessor.getAndAddInt(instance, -1), field.toString());
      model.putInt(0, -1);
      assertBits(field, instance, model);
      Assertions.assertTrue(accessor.casInt(instance, -1, Integer.MAX_VALUE), field.toString());
      model.putInt(0, Integer.MAX_VALUE);
      assertBits(field, instance, model);
      Assertions.assertEquals(Integer.MAX_VALUE, accessor.getAndAddInt(instance, 1), field.toString());
      model.putInt(0, Integer.MIN_VALUE);
      assertBits(field, instance, model);
      Assertions.assertEquals(Integer.MIN_VALUE, accessor.getAndSetInt(instance, 42), field.toString());
      model.putInt(0, 42);
      assertBits(field, instance, model);
    }
  }

  private static void assertBits(Field field, Object instance, ByteBuffer model) throws IllegalAccessException {
    var kind = ValueTypeKind.of(field.getType());
    var expected = switch (kind.byteSize()) {
      case 2 -> model.getShort(0);
      case 4 -> model.getInt(0);
      case 8 -> model.getLong(0);
      default -> throw new AssertionError(kind);
    };
    Assertions.assertEquals(
      expected & kind.bitMask(), ValueTypeKind.toBits(field.get(instance)) & kind.bitMask(), field.toString());
  }

  private static final class Fields {

    short shortField;
    char charField;
    int intField;
    float floatField;
    long longField;
    double doubleField;
    final long finalLongField = Long.parseLong("0");
    static long staticLongField;
    static final long STATIC_FINAL_LONG_FIELD = Long.parseLong("0");
  }
}
