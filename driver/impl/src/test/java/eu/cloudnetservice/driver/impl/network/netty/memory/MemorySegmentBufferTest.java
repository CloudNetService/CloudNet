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

package eu.cloudnetservice.driver.impl.network.netty.memory;

import com.google.common.primitives.Primitives;
import eu.cloudnetservice.driver.impl.network.netty.NettyUtil;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.buffer.BufferClosedException;
import io.netty5.buffer.BufferReadOnlyException;
import io.netty5.buffer.MemoryManager;
import io.netty5.buffer.bytebuffer.ByteBufferMemoryManager;
import io.netty5.buffer.internal.InternalBufferUtils;
import io.netty5.buffer.internal.ResourceSupport;
import java.io.IOException;
import java.lang.classfile.TypeKind;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MemorySegmentBufferTest {

  private static final VarHandle BYTE_ARRAY_AS_LONG =
    MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
  private static final VarHandle BYTE_ARRAY_AS_SHORT =
    MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);

  private static final OpenOption[] RW_TRUNCATE_OPTIONS = new OpenOption[]{
    StandardOpenOption.READ,
    StandardOpenOption.WRITE,
    StandardOpenOption.TRUNCATE_EXISTING,
  };

  // allocator to allocate memory segment backed buffers
  private static BufferAllocator onHeapAllocator;
  private static BufferAllocator offHeapAllocator;

  @BeforeAll
  static void setup() {
    var _ = NettyUtil.selectedBufferAllocator(); // init memory manager
    onHeapAllocator = BufferAllocator.onHeapUnpooled();
    offHeapAllocator = BufferAllocator.offHeapUnpooled();

    // validate that both allocators are actually producing memory-segment backed buffers
    try (var testBuffer = onHeapAllocator.allocate(0)) {
      Assertions.assertInstanceOf(MemorySegmentBuffer.class, testBuffer);
    }
    try (var testBuffer = offHeapAllocator.allocate(0)) {
      Assertions.assertInstanceOf(MemorySegmentBuffer.class, testBuffer);
    }
  }

  @AfterAll
  static void teardown() {
    onHeapAllocator.close();
    offHeapAllocator.close();
  }

  // parameterized test source to provide buffers of the on-heap and off-heap type
  static Stream<Buffer> bufferTypes() {
    return Stream.of(onHeapAllocator.allocate(0), offHeapAllocator.allocate(0));
  }

  // get a byte array of the given byte count, pre-filled with random bytes
  static byte[] randomBytes(int count) {
    var bytes = new byte[count];
    ThreadLocalRandom.current().nextBytes(bytes);
    return bytes;
  }

  @Test
  void testBasics() {
    try (var buffer = onHeapAllocator.allocate(10)) {
      Assertions.assertFalse(buffer.isDirect());
      Assertions.assertEquals(10, buffer.capacity());
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(0, buffer.writerOffset());
      Assertions.assertEquals(0, buffer.readableBytes());
      Assertions.assertEquals(10, buffer.writableBytes());

      buffer.writeInt(1234);
      Assertions.assertEquals(10, buffer.capacity());
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());
      Assertions.assertEquals(4, buffer.readableBytes());
      Assertions.assertEquals(6, buffer.writableBytes());

      buffer.readInt();
      Assertions.assertEquals(10, buffer.capacity());
      Assertions.assertEquals(4, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());
      Assertions.assertEquals(0, buffer.readableBytes());
      Assertions.assertEquals(6, buffer.writableBytes());
    }

    try (var buffer = offHeapAllocator.allocate(10)) {
      Assertions.assertTrue(buffer.isDirect());
      Assertions.assertEquals(10, buffer.capacity());
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(0, buffer.writerOffset());
      Assertions.assertEquals(0, buffer.readableBytes());
      Assertions.assertEquals(10, buffer.writableBytes());

      buffer.writeInt(1234);
      Assertions.assertEquals(10, buffer.capacity());
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());
      Assertions.assertEquals(4, buffer.readableBytes());
      Assertions.assertEquals(6, buffer.writableBytes());

      buffer.readInt();
      Assertions.assertEquals(10, buffer.capacity());
      Assertions.assertEquals(4, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());
      Assertions.assertEquals(0, buffer.readableBytes());
      Assertions.assertEquals(6, buffer.writableBytes());
    }
  }

  @Test
  void testOffsetsMoving() {
    try (var buffer = offHeapAllocator.allocate(16)) {
      buffer.writeInt(Integer.MAX_VALUE);
      buffer.writeInt(Integer.MIN_VALUE);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      Assertions.assertEquals(Integer.MAX_VALUE, buffer.readInt());
      Assertions.assertEquals(Integer.MIN_VALUE, buffer.readInt());
      Assertions.assertEquals(8, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      buffer.writerOffset(11);
      buffer.readerOffset(11);
      Assertions.assertEquals(11, buffer.readerOffset());
      Assertions.assertEquals(11, buffer.writerOffset());

      buffer.writeInt(191919);
      Assertions.assertEquals(15, buffer.writerOffset());
      Assertions.assertEquals(191919, buffer.readInt());

      buffer.readerOffset(0);
      Assertions.assertEquals(Integer.MAX_VALUE, buffer.readInt());

      // moving the writer index back is not allowed
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writerOffset(0));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writerOffset(-5));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.readerOffset(-5));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writerOffset(20));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.readerOffset(20));
    }
  }

  @Test
  void testByteWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeByte((byte) 1);
      buffer.writeByte((byte) 111);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(2, buffer.writerOffset());

      Assertions.assertEquals(1, buffer.readByte());
      Assertions.assertEquals(111, buffer.readByte());
      Assertions.assertEquals(2, buffer.readerOffset());
      Assertions.assertEquals(2, buffer.writerOffset());

      buffer.setByte(1, (byte) 55);
      Assertions.assertEquals(55, buffer.getByte(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readByte);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getByte(3));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setByte(3, (byte) 1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getByte(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setByte(-1, (byte) 1));
    }
  }

  @Test
  void testUnsignedByteWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeUnsignedByte((byte) 1);
      buffer.writeUnsignedByte((byte) 111);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(2, buffer.writerOffset());

      Assertions.assertEquals(1, buffer.readUnsignedByte());
      Assertions.assertEquals(111, buffer.readUnsignedByte());
      Assertions.assertEquals(2, buffer.readerOffset());
      Assertions.assertEquals(2, buffer.writerOffset());

      buffer.setUnsignedByte(1, (byte) 55);
      Assertions.assertEquals(55, buffer.getUnsignedByte(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readUnsignedByte);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedByte(3));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedByte(3, (byte) 1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedByte(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedByte(-1, (byte) 1));
    }
  }

  @Test
  void testCharWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeChar('Z');
      buffer.writeChar('Ü');
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());

      Assertions.assertEquals('Z', buffer.readChar());
      Assertions.assertEquals('Ü', buffer.readChar());
      Assertions.assertEquals(4, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());

      buffer.setChar(1, 'A'); // unaligned
      Assertions.assertEquals('A', buffer.getChar(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readChar);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getChar(3));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setChar(3, 'F'));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getChar(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setChar(-1, 'F'));
    }
  }

  @Test
  void testShortWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeShort((short) 1289);
      buffer.writeShort((short) -1389);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());

      Assertions.assertEquals(1289, buffer.readShort());
      Assertions.assertEquals(-1389, buffer.readShort());
      Assertions.assertEquals(4, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());

      buffer.setShort(1, (short) 13289); // unaligned
      Assertions.assertEquals(13289, buffer.getShort(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readShort);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getShort(3));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setShort(3, (short) 50));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getShort(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setShort(-1, (short) 50));
    }
  }

  @Test
  void testUnsignedShortWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeUnsignedShort(35129);
      buffer.writeUnsignedShort(60999);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());

      Assertions.assertEquals(35129, buffer.readUnsignedShort());
      Assertions.assertEquals(60999, buffer.readUnsignedShort());
      Assertions.assertEquals(4, buffer.readerOffset());
      Assertions.assertEquals(4, buffer.writerOffset());

      buffer.setUnsignedShort(1, 56900); // unaligned
      Assertions.assertEquals(56900, buffer.getUnsignedShort(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readUnsignedShort);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedShort(3));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedShort(3, 50));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedShort(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedShort(-1, 50));
    }
  }

  @Test
  void testMediumWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeMedium(70986);
      buffer.writeMedium(-69690);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(6, buffer.writerOffset());

      Assertions.assertEquals(70986, buffer.readMedium());
      Assertions.assertEquals(-69690, buffer.readMedium());
      Assertions.assertEquals(6, buffer.readerOffset());
      Assertions.assertEquals(6, buffer.writerOffset());

      buffer.setMedium(1, 72920); // unaligned
      Assertions.assertEquals(72920, buffer.getMedium(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readMedium);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getMedium(10));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setMedium(10, 50));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getMedium(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setMedium(-1, 50));
    }
  }

  @Test
  void testUnsignedMediumWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeUnsignedMedium(70986);
      buffer.writeUnsignedMedium(69690);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(6, buffer.writerOffset());

      Assertions.assertEquals(70986, buffer.readUnsignedMedium());
      Assertions.assertEquals(69690, buffer.readUnsignedMedium());
      Assertions.assertEquals(6, buffer.readerOffset());
      Assertions.assertEquals(6, buffer.writerOffset());

      buffer.setUnsignedMedium(1, 72920); // unaligned
      Assertions.assertEquals(72920, buffer.getUnsignedMedium(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readUnsignedMedium);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedMedium(10));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedMedium(10, 50));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedMedium(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedMedium(-1, 50));
    }
  }

  @Test
  void testIntWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeInt(1234567);
      buffer.writeInt(-7654321);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      Assertions.assertEquals(1234567, buffer.readInt());
      Assertions.assertEquals(-7654321, buffer.readInt());
      Assertions.assertEquals(8, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      buffer.setInt(1, 123454321); // unaligned
      Assertions.assertEquals(123454321, buffer.getInt(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readInt);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getInt(100));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setInt(100, 50));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getInt(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setInt(-1, 50));
    }
  }

  @Test
  void testUnsignedIntWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeUnsignedInt(1234567);
      buffer.writeUnsignedInt(7654321);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      Assertions.assertEquals(1234567, buffer.readUnsignedInt());
      Assertions.assertEquals(7654321, buffer.readUnsignedInt());
      Assertions.assertEquals(8, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      buffer.setUnsignedInt(1, 123454321); // unaligned
      Assertions.assertEquals(123454321, buffer.getUnsignedInt(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readUnsignedInt);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedInt(100));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedInt(100, 50));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getUnsignedInt(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setUnsignedInt(-1, 50));
    }
  }

  @Test
  void testFloatWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeFloat(123.45F);
      buffer.writeFloat(453.2451F);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      Assertions.assertEquals(123.45F, buffer.readFloat());
      Assertions.assertEquals(453.2451F, buffer.readFloat());
      Assertions.assertEquals(8, buffer.readerOffset());
      Assertions.assertEquals(8, buffer.writerOffset());

      buffer.setFloat(1, 1984.236F); // unaligned
      Assertions.assertEquals(1984.236F, buffer.getFloat(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readFloat);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getFloat(100));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setFloat(100, 50.1F));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getFloat(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setFloat(-1, 50.1F));
    }
  }

  @Test
  void testLongWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeLong(123456789987654321L);
      buffer.writeLong(-7324872348329L);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(16, buffer.writerOffset());

      Assertions.assertEquals(123456789987654321L, buffer.readLong());
      Assertions.assertEquals(-7324872348329L, buffer.readLong());
      Assertions.assertEquals(16, buffer.readerOffset());
      Assertions.assertEquals(16, buffer.writerOffset());

      buffer.setLong(1, 23478742378437823L); // unaligned
      Assertions.assertEquals(23478742378437823L, buffer.getLong(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readLong);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getLong(100));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setLong(100, 50L));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getLong(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setLong(-1, 50L));
    }
  }

  @Test
  void testDoubleWriting() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      buffer.writeDouble(3754328723.34332445D);
      buffer.writeDouble(-74382734.74138D);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(16, buffer.writerOffset());

      Assertions.assertEquals(3754328723.34332445D, buffer.readDouble());
      Assertions.assertEquals(-74382734.74138D, buffer.readDouble());
      Assertions.assertEquals(16, buffer.readerOffset());
      Assertions.assertEquals(16, buffer.writerOffset());

      buffer.setDouble(1, 4389945.845942D); // unaligned
      Assertions.assertEquals(4389945.845942D, buffer.getDouble(1));

      Assertions.assertThrows(IndexOutOfBoundsException.class, buffer::readDouble);
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getDouble(100));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setDouble(100, 50.1D));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.getDouble(-1));
      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.setDouble(-1, 50.1D));
    }
  }

  @RepeatedTest(50)
  void testFuzzWriting() {
    var written = new Object[50];
    var random = ThreadLocalRandom.current();

    try (var buffer = offHeapAllocator.allocate(0)) {
      // write random types into the buffer
      for (int index = 0; index < written.length; index++) {
        var type = random.nextInt(0, 8);
        switch (type) {
          case 0 -> {
            var val = random.nextBoolean();
            buffer.writeBoolean(val);
            written[index] = val;
          }
          case 1 -> {
            var val = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            buffer.writeByte(val);
            written[index] = val;
          }
          case 2 -> {
            var val = (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
            buffer.writeShort(val);
            written[index] = val;
          }
          case 3 -> {
            var val = random.nextInt();
            buffer.writeInt(val);
            written[index] = val;
          }
          case 4 -> {
            var val = random.nextLong();
            buffer.writeLong(val);
            written[index] = val;
          }
          case 5 -> {
            var val = random.nextFloat();
            buffer.writeFloat(val);
            written[index] = val;
          }
          case 6 -> {
            var val = random.nextDouble();
            buffer.writeDouble(val);
            written[index] = val;
          }
          case 7 -> {
            var val = (char) random.nextInt(Character.MIN_VALUE, Character.MAX_VALUE);
            buffer.writeChar(val);
            written[index] = val;
          }
        }
      }

      // read each written value from the buffer
      for (var obj : written) {
        var desc = Primitives.unwrap(obj.getClass()).descriptorString();
        var kind = TypeKind.fromDescriptor(desc);

        var roff = buffer.readerOffset();
        switch (kind) {
          case BOOLEAN -> {
            var read = buffer.readBoolean();
            var get = buffer.getBoolean(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
          case BYTE -> {
            var read = buffer.readByte();
            var get = buffer.getByte(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
          case SHORT -> {
            var read = buffer.readShort();
            var get = buffer.getShort(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
          case INT -> {
            var read = buffer.readInt();
            var get = buffer.getInt(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
          case LONG -> {
            var read = buffer.readLong();
            var get = buffer.getLong(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
          case FLOAT -> {
            var read = buffer.readFloat();
            var get = buffer.getFloat(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
          case DOUBLE -> {
            var read = buffer.readDouble();
            var get = buffer.getDouble(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
          case CHAR -> {
            var read = buffer.readChar();
            var get = buffer.getChar(roff);
            Assertions.assertEquals(obj, read);
            Assertions.assertEquals(obj, get);
          }
        }
      }

      Assertions.assertTrue(buffer.writerOffset() >= 50 * Byte.BYTES);
      Assertions.assertEquals(buffer.writerOffset(), buffer.readerOffset());
    }
  }

  @Test
  void testFill() {
    try (var buffer = offHeapAllocator.allocate(8)) {
      Assertions.assertEquals(0, buffer.getLong(0));
      buffer.fill((byte) 1);
      Assertions.assertEquals(0x0101010101010101L, buffer.getLong(0));
    }
  }

  @Test
  void testImplicitCapacity() {
    try (var buffer = offHeapAllocator.allocate(0)) {
      Assertions.assertEquals(InternalBufferUtils.MAX_BUFFER_SIZE, buffer.implicitCapacityLimit());
      buffer.implicitCapacityLimit(13); // max growth to 13 bytes
      Assertions.assertEquals(13, buffer.implicitCapacityLimit());

      buffer.writeLong(7234587428982348L);
      buffer.writeInt(2879823);
      buffer.writeByte((byte) -123);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(13, buffer.writerOffset());

      Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writeByte((byte) 123));
    }
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testCopyIntoByteArray(Buffer buffer) {
    buffer.implicitCapacityLimit(18); // only needing 18 bytes at max

    buffer.writeShort((short) 8943);
    buffer.writeLong(987654321123456789L);
    buffer.writeLong(-2784823473287483223L);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());

    var longTarget = new byte[16];
    buffer.copyInto(2, longTarget, 0, 16);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());
    Assertions.assertEquals(987654321123456789L, BYTE_ARRAY_AS_LONG.get(longTarget, 0));
    Assertions.assertEquals(-2784823473287483223L, BYTE_ARRAY_AS_LONG.get(longTarget, 8));

    var longShortTarget = new byte[10];
    buffer.copyInto(0, longShortTarget, 8, 2); // copy short (first 2 bytes) to the end of array
    buffer.copyInto(2, longShortTarget, 0, 8); // copy long (byte 2-10) to the start of array
    Assertions.assertEquals((short) 8943, BYTE_ARRAY_AS_SHORT.get(longShortTarget, 8));
    Assertions.assertEquals(987654321123456789L, BYTE_ARRAY_AS_LONG.get(longShortTarget, 0));

    var oneByteTarget = new byte[1];
    buffer.copyInto(11, oneByteTarget, 0, 1);
    Assertions.assertEquals(90, oneByteTarget[0]); // ((-2784823473287483223L >> 48) & 0xFF)

    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(18, longTarget, 0, 1)); // buffer OOB
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(-1, longTarget, 0, 1)); // buffer underflow
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(0, longTarget, 16, 1)); // target OOB
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(-1, longTarget, 16, 1)); // target underflow
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(0, oneByteTarget, 0, 2)); // target OOB
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testCopyIntoByteBuffer(Buffer buffer) {
    buffer.implicitCapacityLimit(18); // only needing 18 bytes at max

    buffer.writeShort((short) 8943);
    buffer.writeLong(987654321123456789L);
    buffer.writeLong(-2784823473287483223L);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());

    var longTarget = ByteBuffer.allocateDirect(16); // off-heap
    buffer.copyInto(2, longTarget, 0, 16);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());
    Assertions.assertEquals(987654321123456789L, longTarget.getLong(0));
    Assertions.assertEquals(-2784823473287483223L, longTarget.getLong(8));

    var longShortTarget = ByteBuffer.allocate(10); // on-heap
    buffer.copyInto(0, longShortTarget, 8, 2); // copy short (first 2 bytes) to the end of array
    buffer.copyInto(2, longShortTarget, 0, 8); // copy long (byte 2-10) to the start of array
    Assertions.assertEquals((short) 8943, longShortTarget.getShort(8));
    Assertions.assertEquals(987654321123456789L, longShortTarget.getLong(0));

    var oneByteTarget = ByteBuffer.allocate(1); // on-heap
    buffer.copyInto(11, oneByteTarget, 0, 1);
    Assertions.assertEquals(90, oneByteTarget.get(0)); // ((-2784823473287483223L >> 48) & 0xFF)

    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(18, longTarget, 0, 1)); // buffer OOB
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(-1, longTarget, 0, 1)); // buffer underflow
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(0, longTarget, 16, 1)); // target OOB
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(-1, longTarget, 16, 1)); // target underflow
    Assertions.assertThrows(
      IndexOutOfBoundsException.class,
      () -> buffer.copyInto(0, oneByteTarget, 0, 2)); // target OOB
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testCopyIntoBuffer(Buffer buffer) {
    buffer.implicitCapacityLimit(18); // only needing 18 bytes at max

    buffer.writeShort((short) 8943);
    buffer.writeLong(987654321123456789L);
    buffer.writeLong(-2784823473287483223L);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());

    try (var longTarget = offHeapAllocator.allocate(16)) {
      buffer.copyInto(2, longTarget, 0, 16);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(18, buffer.writerOffset());
      Assertions.assertEquals(987654321123456789L, longTarget.getLong(0));
      Assertions.assertEquals(-2784823473287483223L, longTarget.getLong(8));

      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> buffer.copyInto(18, longTarget, 0, 1)); // buffer OOB
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> buffer.copyInto(-1, longTarget, 0, 1)); // buffer underflow
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> buffer.copyInto(0, longTarget, 16, 1)); // target OOB
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> buffer.copyInto(-1, longTarget, 16, 1)); // target underflow
    }

    try (var longShortTarget = onHeapAllocator.allocate(10)) {
      buffer.copyInto(0, longShortTarget, 8, 2); // copy short (first 2 bytes) to the end of array
      buffer.copyInto(2, longShortTarget, 0, 8); // copy long (byte 2-10) to the start of array
      Assertions.assertEquals((short) 8943, longShortTarget.getShort(8));
      Assertions.assertEquals(987654321123456789L, longShortTarget.getLong(0));
    }

    try (var oneByteTarget = onHeapAllocator.allocate(1)) {
      buffer.copyInto(11, oneByteTarget, 0, 1);
      Assertions.assertEquals(90, oneByteTarget.getByte(0)); // ((-2784823473287483223L >> 48) & 0xFF)
      Assertions.assertThrows(
        IndexOutOfBoundsException.class,
        () -> buffer.copyInto(0, oneByteTarget, 0, 2)); // target OOB
    }

    // get buffer allocator that allocates buffer backed by byte buffers
    var byteBufferMemoryManager = new ByteBufferMemoryManager();
    var byteBufferOnHeadAllocator = MemoryManager.using(byteBufferMemoryManager, BufferAllocator::onHeapUnpooled);
    try (var longShortTarget = byteBufferOnHeadAllocator.allocate(10)) {
      buffer.copyInto(0, longShortTarget, 8, 2); // copy short (first 2 bytes) to the end of array
      buffer.copyInto(2, longShortTarget, 0, 8); // copy long (byte 2-10) to the start of array
      Assertions.assertEquals((short) 8943, longShortTarget.getShort(8));
      Assertions.assertEquals(987654321123456789L, longShortTarget.getLong(0));
    }

    var byteBufferOffHeadAllocator = MemoryManager.using(byteBufferMemoryManager, BufferAllocator::offHeapUnpooled);
    try (var longShortTarget = byteBufferOffHeadAllocator.allocate(10)) {
      buffer.copyInto(0, longShortTarget, 8, 2); // copy short (first 2 bytes) to the end of array
      buffer.copyInto(2, longShortTarget, 0, 8); // copy long (byte 2-10) to the start of array
      Assertions.assertEquals((short) 8943, longShortTarget.getShort(8));
      Assertions.assertEquals(987654321123456789L, longShortTarget.getLong(0));
    }
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testCopyWithInternalOffset(Buffer buffer) {
    buffer.implicitCapacityLimit(18); // only needing 18 bytes at max

    buffer.writeShort((short) 8943);
    buffer.writeLong(987654321123456789L);
    buffer.writeLong(-2784823473287483223L);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());

    buffer.split(Short.BYTES); // move MemSeg offset to 2
    Assertions.assertEquals(0, buffer.readerOffset());

    var longTarget = new byte[8];
    buffer.copyInto(8, longTarget, 0, 8);
    Assertions.assertEquals(-2784823473287483223L, BYTE_ARRAY_AS_LONG.get(longTarget, 0));

    var longTargetBB = ByteBuffer.allocate(8);
    buffer.copyInto(8, longTargetBB, 0, 8);
    Assertions.assertEquals(-2784823473287483223L, longTargetBB.getLong());
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testTransferToWritableByteChannel(Buffer buffer) throws IOException {
    buffer.implicitCapacityLimit(18); // only needing 18 bytes at max

    buffer.writeShort((short) 8943);
    buffer.writeLong(987654321123456789L);
    buffer.writeLong(-2784823473287483223L);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());

    var tempFile = Files.createTempFile("cn_mem_seg_test_", null);
    try (var channel = Files.newByteChannel(tempFile, RW_TRUNCATE_OPTIONS)) {
      // write bytes 6-10 into the channel
      buffer.readerOffset(6);
      var written = buffer.transferTo(channel, 4);
      Assertions.assertEquals(4, written);
      Assertions.assertEquals(4, channel.size());
      Assertions.assertEquals(4, channel.position());
      Assertions.assertEquals(10, buffer.readerOffset());

      // check that the least significant bits of the long were successfully transferred
      var dst = ByteBuffer.allocate(4);
      var readBytes = channel.position(0).read(dst);
      Assertions.assertEquals(4, readBytes);
      Assertions.assertEquals(1265727253, dst.getInt(0));

      // read second written long into the channel
      buffer.readerOffset(10);
      channel.position(6);
      written = buffer.transferTo(channel, 8);
      Assertions.assertEquals(8, written);
      Assertions.assertEquals(14, channel.size());
      Assertions.assertEquals(14, channel.position());
      Assertions.assertEquals(18, buffer.readerOffset());

      // check that the long was successfully written
      dst = ByteBuffer.allocate(8);
      readBytes = channel.position(6).read(dst);
      Assertions.assertEquals(8, readBytes);
      Assertions.assertEquals(-2784823473287483223L, dst.getLong(0));

      // buffer overflow
      buffer.readerOffset(0);
      channel.position(0);
      written = buffer.transferTo(channel, 150);
      Assertions.assertEquals(18, written);
      Assertions.assertEquals(18, channel.size());
      Assertions.assertEquals(18, channel.position());

      // buffer OOB
      channel.position(0);
      written = buffer.transferTo(channel, 1);
      Assertions.assertEquals(0, written);
    }
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testTransferToFileChannel(Buffer buffer) throws IOException {
    buffer.implicitCapacityLimit(18); // only needing 18 bytes at max

    buffer.writeShort((short) 8943);
    buffer.writeLong(987654321123456789L);
    buffer.writeLong(-2784823473287483223L);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.writerOffset());

    var tempFile = Files.createTempFile("cn_mem_seg_test_", null);
    try (var channel = FileChannel.open(tempFile, RW_TRUNCATE_OPTIONS)) {
      // write bytes 6-10 into the channel
      buffer.readerOffset(6);
      var written = buffer.transferTo(channel, 0, 4);
      Assertions.assertEquals(4, written);
      Assertions.assertEquals(4, channel.size());
      Assertions.assertEquals(10, buffer.readerOffset());

      // check that the least significant bits of the long were successfully transferred
      var dst = ByteBuffer.allocate(4);
      var readBytes = channel.position(0).read(dst);
      Assertions.assertEquals(4, readBytes);
      Assertions.assertEquals(1265727253, dst.getInt(0));

      // read second written long into the channel
      buffer.readerOffset(10);
      written = buffer.transferTo(channel, 6, 8);
      Assertions.assertEquals(8, written);
      Assertions.assertEquals(14, channel.size());
      Assertions.assertEquals(18, buffer.readerOffset());

      // check that the long was successfully written
      dst = ByteBuffer.allocate(8);
      readBytes = channel.position(6).read(dst);
      Assertions.assertEquals(8, readBytes);
      Assertions.assertEquals(-2784823473287483223L, dst.getLong(0));

      // buffer overflow
      buffer.readerOffset(0);
      written = buffer.transferTo(channel, 0, 150);
      Assertions.assertEquals(18, written);
      Assertions.assertEquals(18, channel.size());

      // buffer OOB
      written = buffer.transferTo(channel, 0, 1);
      Assertions.assertEquals(0, written);
    }
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testTransferFromFileChannel(Buffer buffer) throws IOException {
    buffer.implicitCapacityLimit(18).ensureWritable(18); // only needing 18 bytes at max

    var tempFile = Files.createTempFile("cn_mem_seg_test_", null);
    try (var channel = FileChannel.open(tempFile, RW_TRUNCATE_OPTIONS)) {
      // write seed data into the file channel
      var src = ByteBuffer.allocate(18);
      src.putShort((short) 8943);
      src.putLong(987654321123456789L);
      src.putLong(-2784823473287483223L);
      var written = channel.write(src.flip());
      Assertions.assertEquals(18, written);

      // read bytes 6-10 into the buffer
      var read = buffer.transferFrom(channel, 6, 4);
      Assertions.assertEquals(4, read);
      Assertions.assertEquals(4, buffer.readableBytes());
      Assertions.assertEquals(4, buffer.writerOffset());

      // check that the least significant bits of the long were successfully transferred
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(1265727253, buffer.readInt());

      // read second written long into the buffer
      read = buffer.transferFrom(channel, 10, 8);
      Assertions.assertEquals(8, read);
      Assertions.assertEquals(8, buffer.readableBytes());
      Assertions.assertEquals(12, buffer.writerOffset());

      // check that the long was successfully written
      Assertions.assertEquals(4, buffer.readerOffset());
      Assertions.assertEquals(-2784823473287483223L, buffer.readLong());

      // channel overflow
      buffer.resetOffsets();
      read = buffer.transferFrom(channel, 0, 150);
      Assertions.assertEquals(18, read);
      Assertions.assertEquals(18, buffer.readableBytes());
      Assertions.assertEquals(18, buffer.writerOffset());

      // channel OOB
      read = buffer.transferFrom(channel, 18, 1);
      Assertions.assertEquals(0, read);
    }
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testTransferFromReadableByteChannel(Buffer buffer) throws IOException {
    buffer.implicitCapacityLimit(18).ensureWritable(18); // only needing 18 bytes at max

    var tempFile = Files.createTempFile("cn_mem_seg_test_", null);
    try (var channel = Files.newByteChannel(tempFile, RW_TRUNCATE_OPTIONS)) {
      // write seed data into the file channel
      var src = ByteBuffer.allocate(18);
      src.putShort((short) 8943);
      src.putLong(987654321123456789L);
      src.putLong(-2784823473287483223L);
      var written = channel.write(src.flip());
      Assertions.assertEquals(18, written);

      // read bytes 6-10 into the buffer
      channel.position(6);
      var read = buffer.transferFrom(channel, 4);
      Assertions.assertEquals(4, read);
      Assertions.assertEquals(4, buffer.readableBytes());
      Assertions.assertEquals(4, buffer.writerOffset());

      // check that the least significant bits of the long were successfully transferred
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(1265727253, buffer.readInt());

      // read second written long into the buffer
      channel.position(10);
      read = buffer.transferFrom(channel, 8);
      Assertions.assertEquals(8, read);
      Assertions.assertEquals(8, buffer.readableBytes());
      Assertions.assertEquals(12, buffer.writerOffset());

      // check that the long was successfully written
      Assertions.assertEquals(4, buffer.readerOffset());
      Assertions.assertEquals(-2784823473287483223L, buffer.readLong());

      // channel overflow
      buffer.resetOffsets();
      channel.position(0);
      read = buffer.transferFrom(channel, 150);
      Assertions.assertEquals(18, read);
      Assertions.assertEquals(18, buffer.readableBytes());
      Assertions.assertEquals(18, buffer.writerOffset());

      // channel OOB
      channel.position(18);
      read = buffer.transferFrom(channel, 1);
      Assertions.assertEquals(0, read);
    }
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testWriteByteArray(Buffer buffer) {
    var bytes = new byte[18];
    BYTE_ARRAY_AS_SHORT.set(bytes, 0, (short) 8943);
    BYTE_ARRAY_AS_LONG.set(bytes, 2, 987654321123456789L);
    BYTE_ARRAY_AS_LONG.set(bytes, 10, -2784823473287483223L);

    buffer.implicitCapacityLimit(18); // only needing 18 bytes at max
    buffer.writeBytes(bytes);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(18, buffer.readableBytes());

    // validate transfer
    Assertions.assertEquals(8943, buffer.readShort());
    Assertions.assertEquals(987654321123456789L, buffer.readLong());
    Assertions.assertEquals(-2784823473287483223L, buffer.readLong());

    // transfer only bytes 6-10 and then 0-2 into the buffer
    buffer.resetOffsets();
    buffer.writeBytes(bytes, 6, 4);
    buffer.writeBytes(bytes, 0, 2);
    Assertions.assertEquals(0, buffer.readerOffset());
    Assertions.assertEquals(6, buffer.readableBytes());

    // validate transfer
    Assertions.assertEquals(1265727253, buffer.readInt());
    Assertions.assertEquals(8943, buffer.readShort());

    // transfer bytes 0-2 to the end of the buffer
    buffer.writerOffset(16).readerOffset(16);
    buffer.writeBytes(bytes, 0, 2);
    Assertions.assertEquals(2, buffer.readableBytes());
    Assertions.assertEquals(8943, buffer.readShort());

    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writeBytes(bytes, 0, 1)); // buffer OOB
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writeBytes(bytes, -1, 2)); // src underflow
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writeBytes(bytes, 0, 100)); // src underflow
    Assertions.assertThrows(IllegalArgumentException.class, () -> buffer.writeBytes(bytes, 0, -5)); // length invalid
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testWriteByteBuffer(Buffer buffer) {
    for (var type = 0; type < 2; type++) {
      var byteBuffer = type == 0 ? ByteBuffer.allocate(18) : ByteBuffer.allocateDirect(18);
      byteBuffer.putShort((short) 8943);
      byteBuffer.putLong(987654321123456789L);
      byteBuffer.putLong(-2784823473287483223L);
      byteBuffer = byteBuffer.flip();

      buffer.implicitCapacityLimit(18); // only needing 18 bytes at max
      buffer.writeBytes(byteBuffer);
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(18, buffer.readableBytes());

      // validate transfer
      Assertions.assertEquals(8943, buffer.readShort());
      Assertions.assertEquals(987654321123456789L, buffer.readLong());
      Assertions.assertEquals(-2784823473287483223L, buffer.readLong());

      // transfer only bytes 6-10 and then 0-2 into the buffer
      buffer.resetOffsets();
      buffer.writeBytes(byteBuffer.position(6).limit(10));
      buffer.writeBytes(byteBuffer.position(0).limit(2));
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(6, buffer.readableBytes());

      // validate transfer
      Assertions.assertEquals(1265727253, buffer.readInt());
      Assertions.assertEquals(8943, buffer.readShort());

      // transfer bytes 0-2 to the end of the buffer
      buffer.writerOffset(16).readerOffset(16);
      buffer.writeBytes(byteBuffer.position(0).limit(2));
      Assertions.assertEquals(2, buffer.readableBytes());
      Assertions.assertEquals(8943, buffer.readShort());

      // clear for possible next run
      buffer.resetOffsets().fill((byte) 0);
    }

    var hugeBuffer = ByteBuffer.allocate(100);
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writeBytes(hugeBuffer)); // buffer OOB
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testBytesBefore(Buffer buffer) {
    buffer.implicitCapacityLimit(12).ensureWritable(12); // fix buffer size to 12
    buffer.writeLong(0x12124466778899AAL).writeInt(0x66778899); // pre-fill without using 0x03 oder 0x05

    // find byte at start
    var needle = (byte) 0x03;
    buffer.setByte(3, needle);
    buffer.readerOffset(2);
    Assertions.assertEquals(1, buffer.bytesBefore(needle));

    // find buffer at start
    try (var needleBuf = onHeapAllocator.allocate(3)) {
      needleBuf.writeMedium(0x667788);
      buffer.readerOffset(7);
      Assertions.assertEquals(1, buffer.bytesBefore(needleBuf));
    }

    // find byte in first long
    buffer.readerOffset(0);
    Assertions.assertEquals(7, buffer.bytesBefore((byte) 0xAA));

    // find buffer in first long
    try (var needleBuf = onHeapAllocator.allocate(1)) {
      needleBuf.writeByte((byte) 0xAA);
      buffer.readerOffset(0);
      Assertions.assertEquals(7, buffer.bytesBefore(needleBuf));
    }

    // find byte at the end
    buffer.readerOffset(8);
    Assertions.assertEquals(3, buffer.bytesBefore((byte) 0x99));

    // find non-existent byte in buffer
    Assertions.assertEquals(-1, buffer.bytesBefore((byte) 0x05));
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testByteCursor(Buffer buffer) {
    var randBytes = randomBytes(12);
    buffer.writeBytes(randBytes);

    var cursor = buffer.openCursor();
    Assertions.assertEquals(12, cursor.bytesLeft());
    while (cursor.readByte()) {
      var off = cursor.currentOffset();
      Assertions.assertEquals(12 - off, cursor.bytesLeft());
      Assertions.assertEquals(randBytes[off - 1], cursor.getByte());
    }

    Assertions.assertEquals(0, cursor.bytesLeft());
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testByteCursorRegion(Buffer buffer) {
    var randBytes = randomBytes(12);
    buffer.writeBytes(randBytes);

    var cursor = buffer.openCursor(4, 6);
    Assertions.assertEquals(6, cursor.bytesLeft());
    while (cursor.readByte()) {
      var off = cursor.currentOffset();
      Assertions.assertEquals(10 - off, cursor.bytesLeft());
      Assertions.assertEquals(randBytes[off - 1], cursor.getByte());
    }

    Assertions.assertEquals(0, cursor.bytesLeft());

    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.openCursor(12, 1)); // buffer OOB
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.openCursor(-1, 1)); // buffer underflow
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.openCursor(1, -1)); // length invalid
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.openCursor(10, 5)); // buffer OOB
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testReverseByteCursor(Buffer buffer) {
    var randBytes = randomBytes(12);
    buffer.writeBytes(randBytes);

    var cursor = buffer.openReverseCursor();
    Assertions.assertEquals(12, cursor.bytesLeft());
    while (cursor.readByte()) {
      var off = cursor.currentOffset();
      Assertions.assertEquals(off + 1, cursor.bytesLeft());
      Assertions.assertEquals(randBytes[off + 1], cursor.getByte());
    }

    Assertions.assertEquals(0, cursor.bytesLeft());
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testReverseByteCursorRegion(Buffer buffer) {
    var randBytes = randomBytes(12);
    buffer.writeBytes(randBytes);

    var cursor = buffer.openReverseCursor(10, 6);
    Assertions.assertEquals(6, cursor.bytesLeft());
    while (cursor.readByte()) {
      var off = cursor.currentOffset();
      Assertions.assertEquals(off - 4, cursor.bytesLeft());
      Assertions.assertEquals(randBytes[off + 1], cursor.getByte());
    }

    Assertions.assertEquals(0, cursor.bytesLeft());
  }

  @Test
  void testEnsureWriteable() {
    try (var buffer = onHeapAllocator.allocate(64)) {
      var randomBytes = randomBytes(64);
      buffer.writeBytes(randomBytes);
      Assertions.assertEquals(64, buffer.capacity());

      buffer.readerOffset(8);
      Assertions.assertEquals(8, buffer.readerOffset());
      Assertions.assertEquals(64, buffer.writerOffset());

      // test that the buffer doesn't expand when the first 8 bytes can be thrown away
      buffer.ensureWritable(8, 8, true);
      buffer.writeLong(123456789L);
      Assertions.assertEquals(64, buffer.capacity());
      Assertions.assertEquals(randomBytes[8], buffer.getByte(0));
      Assertions.assertEquals(123456789L, buffer.getLong(56));

      buffer.readerOffset(8);
      Assertions.assertEquals(8, buffer.readerOffset());
      Assertions.assertEquals(64, buffer.writerOffset());

      // test that the buffer expands if compaction doesn't solve the sizing issues
      buffer.ensureWritable(16, 16, true);
      Assertions.assertEquals(80, buffer.capacity());
      Assertions.assertEquals(randomBytes[8], buffer.getByte(0));
      Assertions.assertEquals(123456789L, buffer.getLong(56));

      // test that the buffer expands if compaction isn't allowed
      buffer.writerOffset(77);
      buffer.ensureWritable(4, 4, false);
      Assertions.assertEquals(84, buffer.capacity());
      Assertions.assertEquals(randomBytes[8], buffer.getByte(0));
      Assertions.assertEquals(123456789L, buffer.getLong(56));

      // test minimum growth is respected over requested length
      buffer.writerOffset(80);
      buffer.ensureWritable(15, 20, true);
      Assertions.assertEquals(104, buffer.capacity());
      Assertions.assertEquals(randomBytes[8], buffer.getByte(0));
      Assertions.assertEquals(123456789L, buffer.getLong(56));
    }
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testBufferCopy(Buffer buffer) {
    buffer.writeByte((byte) 0x05);
    buffer.writeInt(123456789);
    buffer.writeLong(12345678987654321L);

    // copy the full buffer
    try (var fullCopy = buffer.copy()) {
      Assertions.assertFalse(fullCopy.readOnly());
      Assertions.assertEquals(0, fullCopy.readerOffset());
      Assertions.assertEquals(buffer.writerOffset(), fullCopy.writerOffset());
      Assertions.assertEquals(0x05, fullCopy.readByte());
      Assertions.assertEquals(123456789, fullCopy.readInt());
      Assertions.assertEquals(12345678987654321L, fullCopy.readLong());
    }

    // copy the full buffer, read-only
    try (var fullCopyRo = buffer.copy(true)) {
      Assertions.assertTrue(fullCopyRo.readOnly());
      Assertions.assertEquals(0, fullCopyRo.readerOffset());
      Assertions.assertEquals(buffer.writerOffset(), fullCopyRo.writerOffset());
      Assertions.assertEquals(0x05, fullCopyRo.readByte());
      Assertions.assertEquals(123456789, fullCopyRo.readInt());
      Assertions.assertEquals(12345678987654321L, fullCopyRo.readLong());
      Assertions.assertThrows(BufferReadOnlyException.class, () -> fullCopyRo.writeByte((byte) 0x01));
    }

    // copy range of the buffer
    try (var intOnlyCopy = buffer.copy(1, 4)) {
      Assertions.assertFalse(intOnlyCopy.readOnly());
      Assertions.assertEquals(0, intOnlyCopy.readerOffset());
      Assertions.assertEquals(4, intOnlyCopy.writerOffset());
      Assertions.assertEquals(123456789, intOnlyCopy.readInt());
    }

    // copy range of the buffer, read-only
    try (var longOnlyCopyRo = buffer.copy(5, 8, true)) {
      Assertions.assertTrue(longOnlyCopyRo.readOnly());
      Assertions.assertEquals(0, longOnlyCopyRo.readerOffset());
      Assertions.assertEquals(8, longOnlyCopyRo.writerOffset());
      Assertions.assertEquals(12345678987654321L, longOnlyCopyRo.readLong());
      Assertions.assertThrows(BufferReadOnlyException.class, () -> longOnlyCopyRo.writeByte((byte) 0x01));
    }

    // make the original buffer read-only
    buffer.makeReadOnly();
    Assertions.assertTrue(buffer.readOnly());
    Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeByte((byte) 0x01));

    // copy full buffer from read-only variant of the original buffer
    try (var fullCopyRoRo = buffer.copy(true)) {
      Assertions.assertTrue(fullCopyRoRo.readOnly());
      Assertions.assertEquals(0, fullCopyRoRo.readerOffset());
      Assertions.assertEquals(buffer.writerOffset(), fullCopyRoRo.writerOffset());
      Assertions.assertEquals(0x05, fullCopyRoRo.readByte());
      Assertions.assertEquals(123456789, fullCopyRoRo.readInt());
      Assertions.assertEquals(12345678987654321L, fullCopyRoRo.readLong());
      Assertions.assertThrows(BufferReadOnlyException.class, () -> fullCopyRoRo.writeByte((byte) 0x01));
    }

    // copy range of the read-only buffer, read-only
    try (var longOnlyCopyRoRo = buffer.copy(5, 8, true)) {
      Assertions.assertTrue(longOnlyCopyRoRo.readOnly());
      Assertions.assertEquals(0, longOnlyCopyRoRo.readerOffset());
      Assertions.assertEquals(8, longOnlyCopyRoRo.writerOffset());
      Assertions.assertEquals(12345678987654321L, longOnlyCopyRoRo.readLong());
      Assertions.assertThrows(BufferReadOnlyException.class, () -> longOnlyCopyRoRo.writeByte((byte) 0x01));
    }

    // copy range of the read-only buffer, into a non-read-only buffer
    try (var longOnlyCopyRw = buffer.copy(5, 8)) {
      Assertions.assertFalse(longOnlyCopyRw.readOnly());
      Assertions.assertEquals(0, longOnlyCopyRw.readerOffset());
      Assertions.assertEquals(8, longOnlyCopyRw.writerOffset());
      Assertions.assertEquals(12345678987654321L, longOnlyCopyRw.readLong());
      Assertions.assertDoesNotThrow(() -> longOnlyCopyRw.writeByte((byte) 0x01));
    }

    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.copy(0, -1));
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.copy(2, -1));
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.copy(0, -1, true));
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.copy(2, -1, true));
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.copy(0, 20));
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.copy(0, 20, true));
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testReadBufferSplit(Buffer buffer) {
    buffer.implicitCapacityLimit(24); // don't expand over 24 bytes

    buffer.writeLong(45879823482439L);
    buffer.writeLong(43898534859483758L);
    buffer.writeLong(6589586958659L);

    buffer.readerOffset(4);
    try (var readSplit = buffer.readSplit(4)) {
      Assertions.assertEquals(4, readSplit.readerOffset());
      Assertions.assertEquals(8, readSplit.writerOffset());
      Assertions.assertEquals(8, readSplit.capacity());
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(16, buffer.writerOffset());
      Assertions.assertEquals(16, buffer.capacity());

      // test that the splitting was successful
      Assertions.assertEquals(982826567, readSplit.readInt());
      Assertions.assertEquals(43898534859483758L, buffer.readLong());
      Assertions.assertEquals(6589586958659L, buffer.readLong());
    }

    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.readSplit(50));
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testWriteBufferSplit(Buffer buffer) {
    buffer.implicitCapacityLimit(24); // don't expand over 24 bytes

    buffer.writeLong(45879823482439L);
    buffer.writeLong(43898534859483758L);
    buffer.writeLong(6589586958659L);

    buffer.writerOffset(4);
    try (var writeSplit = buffer.writeSplit(4)) {
      Assertions.assertEquals(0, writeSplit.readerOffset());
      Assertions.assertEquals(4, writeSplit.writerOffset());
      Assertions.assertEquals(8, writeSplit.capacity());
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(0, buffer.writerOffset());
      Assertions.assertEquals(16, buffer.capacity());

      // test that the splitting was successful
      writeSplit.skipWritableBytes(4);
      Assertions.assertEquals(10682, writeSplit.readInt()); // MSB
      Assertions.assertEquals(982826567, writeSplit.readInt()); // LSB

      buffer.skipWritableBytes(16);
      Assertions.assertEquals(43898534859483758L, buffer.readLong());
      Assertions.assertEquals(6589586958659L, buffer.readLong());
    }

    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writeSplit(50));
  }

  @ParameterizedTest
  @MethodSource("bufferTypes")
  void testBufferSplit(Buffer buffer) {
    buffer.implicitCapacityLimit(24); // don't expand over 24 bytes

    buffer.writeLong(45879823482439L);
    buffer.writeLong(43898534859483758L);
    buffer.writeLong(6589586958659L);

    try (var split = buffer.split(12)) {
      Assertions.assertEquals(0, split.readerOffset());
      Assertions.assertEquals(12, split.writerOffset());
      Assertions.assertEquals(12, split.capacity());
      Assertions.assertEquals(0, buffer.readerOffset());
      Assertions.assertEquals(12, buffer.writerOffset());
      Assertions.assertEquals(12, buffer.capacity());

      // test that the splitting was successful
      Assertions.assertEquals(45879823482439L, split.readLong());
      Assertions.assertEquals(10220924, split.readInt()); // MSB
      Assertions.assertEquals(544582254, buffer.readInt()); // LSB
      Assertions.assertEquals(6589586958659L, buffer.readLong());
    }

    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.writeSplit(50));
  }

  @Test
  void testReadOnlyBuffersThrowExceptionOnWriteAccess() {
    try (var buffer = onHeapAllocator.allocate(8)) {
      buffer.makeReadOnly();

      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writerOffset(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writerOffset(4));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.fill((byte) 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.ensureWritable(4));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.ensureWritable(4, 4, true));
      Assertions.assertThrows(BufferReadOnlyException.class, buffer::compact);

      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeByte((byte) 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeShort((short) 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeChar((char) 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeMedium(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeInt(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeFloat(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeLong(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeDouble(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeUnsignedByte(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeUnsignedShort(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeUnsignedMedium(0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.writeUnsignedInt(0));
      Assertions.assertEquals(0, buffer.writerOffset());

      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setByte(0, (byte) 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setShort(0, (short) 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setChar(0, (char) 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setMedium(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setInt(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setFloat(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setLong(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setDouble(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setUnsignedByte(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setUnsignedShort(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setUnsignedMedium(0, 0));
      Assertions.assertThrows(BufferReadOnlyException.class, () -> buffer.setUnsignedInt(0, 0));
    }
  }

  @Test
  void testClosedBuffersThrowExceptionOnAccess() {
    var buffer = onHeapAllocator.allocate(8);
    buffer.close();

    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writerOffset(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writerOffset(4));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.fill((byte) 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.ensureWritable(4));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.ensureWritable(4, 4, true));
    Assertions.assertThrows(BufferClosedException.class, buffer::compact);

    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeByte((byte) 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeShort((short) 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeChar((char) 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeMedium(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeInt(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeFloat(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeLong(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeDouble(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeUnsignedByte(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeUnsignedShort(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeUnsignedMedium(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.writeUnsignedInt(0));
    Assertions.assertEquals(0, buffer.writerOffset());

    Assertions.assertThrows(BufferClosedException.class, buffer::readByte);
    Assertions.assertThrows(BufferClosedException.class, buffer::readShort);
    Assertions.assertThrows(BufferClosedException.class, buffer::readChar);
    Assertions.assertThrows(BufferClosedException.class, buffer::readMedium);
    Assertions.assertThrows(BufferClosedException.class, buffer::readInt);
    Assertions.assertThrows(BufferClosedException.class, buffer::readFloat);
    Assertions.assertThrows(BufferClosedException.class, buffer::readLong);
    Assertions.assertThrows(BufferClosedException.class, buffer::readDouble);
    Assertions.assertThrows(BufferClosedException.class, buffer::readUnsignedByte);
    Assertions.assertThrows(BufferClosedException.class, buffer::readUnsignedShort);
    Assertions.assertThrows(BufferClosedException.class, buffer::readUnsignedMedium);
    Assertions.assertThrows(BufferClosedException.class, buffer::readUnsignedInt);
    Assertions.assertEquals(0, buffer.readerOffset());

    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setByte(0, (byte) 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setShort(0, (short) 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setChar(0, (char) 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setMedium(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setInt(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setFloat(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setLong(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setDouble(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setUnsignedByte(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setUnsignedShort(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setUnsignedMedium(0, 0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.setUnsignedInt(0, 0));

    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getByte(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getShort(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getChar(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getMedium(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getInt(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getFloat(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getLong(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getDouble(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getUnsignedByte(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getUnsignedShort(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getUnsignedMedium(0));
    Assertions.assertThrows(BufferClosedException.class, () -> buffer.getUnsignedInt(0));
  }

  @Test
  void testBackingMemoryCannotBeChangedWhileBufferIsAcquired() {
    try (var buffer = onHeapAllocator.allocate(8)) {
      var bufferAsResourceSupport = Assertions.assertInstanceOf(ResourceSupport.class, buffer);
      InternalBufferUtils.acquire(bufferAsResourceSupport); // should usually not be called like this
      Assertions.assertTrue(buffer.isAccessible());
      Assertions.assertFalse(InternalBufferUtils.isOwned(bufferAsResourceSupport));

      Assertions.assertThrows(IllegalStateException.class, () -> buffer.ensureWritable(5));
      Assertions.assertThrows(IllegalStateException.class, () -> buffer.ensureWritable(5, 5, false));
      Assertions.assertThrows(IllegalStateException.class, () -> buffer.ensureWritable(5, 5, true));
      Assertions.assertThrows(IllegalStateException.class, () -> buffer.readSplit(5));
      Assertions.assertThrows(IllegalStateException.class, () -> buffer.writeSplit(5));
      Assertions.assertThrows(IllegalStateException.class, () -> buffer.split(5));
      Assertions.assertThrows(IllegalStateException.class, buffer::split);
      Assertions.assertThrows(IllegalStateException.class, buffer::compact);
      Assertions.assertThrows(IllegalStateException.class, buffer::send);

      Assertions.assertDoesNotThrow(buffer::close); // need to close once manually, as we acquired once manually
      Assertions.assertTrue(buffer.isAccessible());
      Assertions.assertTrue(InternalBufferUtils.isOwned(bufferAsResourceSupport));
    }
  }
}
