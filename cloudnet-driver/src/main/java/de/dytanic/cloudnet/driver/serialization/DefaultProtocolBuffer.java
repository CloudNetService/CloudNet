/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocument;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.ByteProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultProtocolBuffer extends ProtocolBuffer {

  private final ByteBuf wrapped;

  public DefaultProtocolBuffer(ByteBuf wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public ProtocolBuffer writeString(@NotNull String stringToWrite) {
    this.writeArray(stringToWrite.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  @Override
  public @Nullable String readOptionalString() {
    return this.readBoolean() ? this.readString() : null;
  }

  @Override
  public ProtocolBuffer writeOptionalString(@Nullable String stringToWrite) {
    this.writeBoolean(stringToWrite != null);
    if (stringToWrite != null) {
      this.writeString(stringToWrite);
    }
    return this;
  }

  @Override
  public @NotNull String readString() {
    return new String(this.readArray(), StandardCharsets.UTF_8);
  }

  @Override
  public ProtocolBuffer writeArray(byte[] bytes) {
    this.writeVarInt(bytes.length);
    this.writeBytes(bytes);
    return this;
  }

  @Override
  public byte[] readOptionalArray() {
    return this.readBoolean() ? this.readArray() : null;
  }

  @Override
  public ProtocolBuffer writeOptionalArray(byte[] bytes) {
    this.writeBoolean(bytes != null);
    if (bytes != null) {
      this.writeArray(bytes);
    }
    return this;
  }

  @Override
  public byte[] readArray() {
    int length = this.readVarInt();

    byte[] bytes = new byte[length];
    this.readBytes(bytes);

    return bytes;
  }

  @Override
  public byte[] toArray() {
    byte[] bytes = new byte[this.readableBytes()];
    this.getBytes(this.readerIndex(), bytes);
    return bytes;
  }

  @Override
  public ProtocolBuffer writeStringCollection(@NotNull Collection<String> list) {
    this.writeVarInt(list.size());
    for (String s : list) {
      this.writeString(s);
    }
    return this;
  }

  @Override
  public @NotNull String[] readStringArray() {
    String[] array = new String[this.readVarInt()];
    for (int i = 0; i < array.length; i++) {
      array[i] = this.readString();
    }
    return array;
  }

  @Override
  public ProtocolBuffer writeStringArray(@NotNull String[] array) {
    this.writeVarInt(array.length);
    for (String s : array) {
      this.writeString(s);
    }
    return this;
  }

  @Override
  public @NotNull Collection<String> readStringCollection() {
    int length = this.readVarInt();
    List<String> out = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      out.add(this.readString());
    }

    return out;
  }

  @Override
  public int readVarInt() {
    return NettyUtils.readVarInt(this);
  }

  @Override
  public ProtocolBuffer writeVarInt(int value) {
    NettyUtils.writeVarInt(this, value);
    return this;
  }

  @Override
  public long readVarLong() {
    return NettyUtils.readVarLong(this);
  }

  @Override
  public ProtocolBuffer writeVarLong(long value) {
    NettyUtils.writeVarLong(this, value);
    return this;
  }

  @Override
  public @NotNull UUID readUUID() {
    return new UUID(this.readLong(), this.readLong());
  }

  @Override
  public ProtocolBuffer writeUUID(@NotNull UUID uuid) {
    this.writeLong(uuid.getMostSignificantBits());
    this.writeLong(uuid.getLeastSignificantBits());
    return this;
  }

  @Override
  public @Nullable UUID readOptionalUUID() {
    return this.readBoolean() ? this.readUUID() : null;
  }

  @Override
  public ProtocolBuffer writeOptionalUUID(@Nullable UUID uuid) {
    this.writeBoolean(uuid != null);
    if (uuid != null) {
      this.writeUUID(uuid);
    }
    return this;
  }

  @Override
  public @NotNull Collection<UUID> readUUIDCollection() {
    int size = this.readVarInt();
    Collection<UUID> uuids = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      uuids.add(this.readUUID());
    }
    return uuids;
  }

  @Override
  public ProtocolBuffer writeUUIDCollection(@NotNull Collection<UUID> uuids) {
    this.writeVarInt(uuids.size());
    for (UUID uuid : uuids) {
      this.writeUUID(uuid);
    }
    return this;
  }

  @Override
  public @NotNull JsonDocument readJsonDocument() {
    return this.readObject(SerializableJsonDocument.class);
  }

  @Override
  public ProtocolBuffer writeJsonDocument(@NotNull JsonDocument document) {
    return this.writeObject(SerializableJsonDocument.asSerializable(document));
  }

  @Override
  public @Nullable JsonDocument readOptionalJsonDocument() {
    return this.readBoolean() ? this.readJsonDocument() : null;
  }

  @Override
  public ProtocolBuffer writeOptionalJsonDocument(@Nullable JsonDocument document) {
    this.writeBoolean(document != null);
    if (document != null) {
      this.writeJsonDocument(document);
    }
    return this;
  }

  @Override
  public <T extends SerializableObject> @NotNull T readObject(@NotNull Class<T> objectClass) {
    try {
      T t = objectClass.getDeclaredConstructor().newInstance();
      return this.readObject(t);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new Error(exception);
    }
  }

  @Override
  public <T extends SerializableObject> @NotNull T readObject(@NotNull T targetObject) {
    targetObject.read(this);
    return targetObject;
  }

  @Override
  public ProtocolBuffer writeObject(@NotNull SerializableObject object) {
    object.write(this);
    return this;
  }

  @Override
  public <T extends SerializableObject> @Nullable T readOptionalObject(@NotNull Class<T> objectClass) {
    return this.readBoolean() ? this.readObject(objectClass) : null;
  }

  @Override
  public <T extends SerializableObject> T readOptionalObject(@NotNull T targetObject) {
    return this.readBoolean() ? this.readObject(targetObject) : null;
  }

  @Override
  public ProtocolBuffer writeOptionalObject(@Nullable SerializableObject object) {
    this.writeBoolean(object != null);
    if (object != null) {
      this.writeObject(object);
    }
    return this;
  }

  @Override
  public @NotNull <T extends SerializableObject> Collection<T> readObjectCollection(@NotNull Class<T> objectClass) {
    int size = this.readVarInt();
    Collection<T> result = new ArrayList<>(size);

    try {
      Constructor<T> constructor = objectClass.getDeclaredConstructor();
      for (int i = 0; i < size; i++) {
        result.add(this.readObject(constructor.newInstance()));
      }
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
      throw new Error(exception);
    }

    return result;
  }

  @Override
  public ProtocolBuffer writeObjectCollection(@NotNull Collection<? extends SerializableObject> objects) {
    this.writeVarInt(objects.size());
    for (SerializableObject object : objects) {
      this.writeObject(object);
    }
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull <T extends SerializableObject> T[] readObjectArray(@NotNull Class<T> objectClass) {
    int size = this.readVarInt();
    Object result = Array.newInstance(objectClass, size);

    try {
      Constructor<T> constructor = objectClass.getDeclaredConstructor();
      for (int i = 0; i < size; i++) {
        Array.set(result, i, this.readObject(constructor.newInstance()));
      }
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
      throw new Error(exception);
    }

    return (T[]) result;
  }

  @Override
  public <T extends SerializableObject> ProtocolBuffer writeObjectArray(@NotNull T[] objects) {
    this.writeVarInt(objects.length);
    for (T object : objects) {
      this.writeObject(object);
    }
    return this;
  }

  @Override
  public <E extends Enum<E>> E readEnumConstant(@NotNull Class<E> enumClass) {
    return enumClass.getEnumConstants()[this.readVarInt()];
  }

  @Override
  public <E extends Enum<E>> ProtocolBuffer writeEnumConstant(@NotNull E enumConstant) {
    this.writeVarInt(enumConstant.ordinal());
    return this;
  }

  @Override
  public <E extends Enum<E>> E readOptionalEnumConstant(@NotNull Class<E> enumClass) {
    int value = this.readVarInt();
    return value != -1 ? enumClass.getEnumConstants()[value] : null;
  }

  @Override
  public <E extends Enum<E>> ProtocolBuffer writeOptionalEnumConstant(@Nullable E enumConstant) {
    this.writeVarInt(enumConstant != null ? enumConstant.ordinal() : -1);
    return this;
  }

  @Override
  public ProtocolBuffer writeThrowable(Throwable throwable) {
    try (ByteBufOutputStream outputStream = new ByteBufOutputStream(this);
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
      objectOutputStream.writeObject(throwable);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return this;
  }

  @Override
  public Throwable readThrowable() {
    try (ByteBufInputStream inputStream = new ByteBufInputStream(this);
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
      return (Throwable) objectInputStream.readObject();
    } catch (IOException | ClassNotFoundException exception) {
      exception.printStackTrace();
    }
    return null;
  }

  @Override
  public int capacity() {
    return this.wrapped.capacity();
  }

  @Override
  public ByteBuf capacity(int newCapacity) {
    return this.wrapped.capacity(newCapacity);
  }

  @Override
  public int maxCapacity() {
    return this.wrapped.maxCapacity();
  }

  @Override
  public ByteBufAllocator alloc() {
    return this.wrapped.alloc();
  }

  @Override
  @Deprecated
  public ByteOrder order() {
    return this.wrapped.order();
  }

  @Override
  @Deprecated
  public ByteBuf order(ByteOrder endianness) {
    return this.wrapped.order(endianness);
  }

  @Override
  public ByteBuf unwrap() {
    return this.wrapped.unwrap();
  }

  @Override
  public boolean isDirect() {
    return this.wrapped.isDirect();
  }

  @Override
  public boolean isReadOnly() {
    return this.wrapped.isReadOnly();
  }

  @Override
  public ByteBuf asReadOnly() {
    return this.wrapped.asReadOnly();
  }

  @Override
  public int readerIndex() {
    return this.wrapped.readerIndex();
  }

  @Override
  public ByteBuf readerIndex(int readerIndex) {
    return this.wrapped.readerIndex(readerIndex);
  }

  @Override
  public int writerIndex() {
    return this.wrapped.writerIndex();
  }

  @Override
  public ByteBuf writerIndex(int writerIndex) {
    return this.wrapped.writerIndex(writerIndex);
  }

  @Override
  public ByteBuf setIndex(int readerIndex, int writerIndex) {
    return this.wrapped.setIndex(readerIndex, writerIndex);
  }

  @Override
  public int readableBytes() {
    return this.wrapped.readableBytes();
  }

  @Override
  public int writableBytes() {
    return this.wrapped.writableBytes();
  }

  @Override
  public int maxWritableBytes() {
    return this.wrapped.maxWritableBytes();
  }

  @Override
  public boolean isReadable() {
    return this.wrapped.isReadable();
  }

  @Override
  public boolean isReadable(int size) {
    return this.wrapped.isReadable(size);
  }

  @Override
  public boolean isWritable() {
    return this.wrapped.isWritable();
  }

  @Override
  public boolean isWritable(int size) {
    return this.wrapped.isWritable(size);
  }

  @Override
  public ByteBuf clear() {
    return this.wrapped.clear();
  }

  @Override
  public ByteBuf markReaderIndex() {
    return this.wrapped.markReaderIndex();
  }

  @Override
  public ByteBuf resetReaderIndex() {
    return this.wrapped.resetReaderIndex();
  }

  @Override
  public ByteBuf markWriterIndex() {
    return this.wrapped.markWriterIndex();
  }

  @Override
  public ByteBuf resetWriterIndex() {
    return this.wrapped.resetWriterIndex();
  }

  @Override
  public ByteBuf discardReadBytes() {
    return this.wrapped.discardReadBytes();
  }

  @Override
  public ByteBuf discardSomeReadBytes() {
    return this.wrapped.discardSomeReadBytes();
  }

  @Override
  public ByteBuf ensureWritable(int minWritableBytes) {
    return this.wrapped.ensureWritable(minWritableBytes);
  }

  @Override
  public int ensureWritable(int minWritableBytes, boolean force) {
    return this.wrapped.ensureWritable(minWritableBytes, force);
  }

  @Override
  public boolean getBoolean(int index) {
    return this.wrapped.getBoolean(index);
  }

  @Override
  public byte getByte(int index) {
    return this.wrapped.getByte(index);
  }

  @Override
  public short getUnsignedByte(int index) {
    return this.wrapped.getUnsignedByte(index);
  }

  @Override
  public short getShort(int index) {
    return this.wrapped.getShort(index);
  }

  @Override
  public short getShortLE(int index) {
    return this.wrapped.getShortLE(index);
  }

  @Override
  public int getUnsignedShort(int index) {
    return this.wrapped.getUnsignedShort(index);
  }

  @Override
  public int getUnsignedShortLE(int index) {
    return this.wrapped.getUnsignedShortLE(index);
  }

  @Override
  public int getMedium(int index) {
    return this.wrapped.getMedium(index);
  }

  @Override
  public int getMediumLE(int index) {
    return this.wrapped.getMediumLE(index);
  }

  @Override
  public int getUnsignedMedium(int index) {
    return this.wrapped.getUnsignedMedium(index);
  }

  @Override
  public int getUnsignedMediumLE(int index) {
    return this.wrapped.getUnsignedMediumLE(index);
  }

  @Override
  public int getInt(int index) {
    return this.wrapped.getInt(index);
  }

  @Override
  public int getIntLE(int index) {
    return this.wrapped.getIntLE(index);
  }

  @Override
  public long getUnsignedInt(int index) {
    return this.wrapped.getUnsignedInt(index);
  }

  @Override
  public long getUnsignedIntLE(int index) {
    return this.wrapped.getUnsignedIntLE(index);
  }

  @Override
  public long getLong(int index) {
    return this.wrapped.getLong(index);
  }

  @Override
  public long getLongLE(int index) {
    return this.wrapped.getLongLE(index);
  }

  @Override
  public char getChar(int index) {
    return this.wrapped.getChar(index);
  }

  @Override
  public float getFloat(int index) {
    return this.wrapped.getFloat(index);
  }

  @Override
  public float getFloatLE(int index) {
    return this.wrapped.getFloatLE(index);
  }

  @Override
  public double getDouble(int index) {
    return this.wrapped.getDouble(index);
  }

  @Override
  public double getDoubleLE(int index) {
    return this.wrapped.getDoubleLE(index);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst) {
    return this.wrapped.getBytes(index, dst);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int length) {
    return this.wrapped.getBytes(index, dst, length);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
    return this.wrapped.getBytes(index, dst, dstIndex, length);
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst) {
    return this.wrapped.getBytes(index, dst);
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
    return this.wrapped.getBytes(index, dst, dstIndex, length);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuffer dst) {
    return this.wrapped.getBytes(index, dst);
  }

  @Override
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
    return this.wrapped.getBytes(index, out, length);
  }

  @Override
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
    return this.wrapped.getBytes(index, out, length);
  }

  @Override
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
    return this.wrapped.getBytes(index, out, position, length);
  }

  @Override
  public CharSequence getCharSequence(int index, int length, Charset charset) {
    return this.wrapped.getCharSequence(index, length, charset);
  }

  @Override
  public ByteBuf setBoolean(int index, boolean value) {
    return this.wrapped.setBoolean(index, value);
  }

  @Override
  public ByteBuf setByte(int index, int value) {
    return this.wrapped.setByte(index, value);
  }

  @Override
  public ByteBuf setShort(int index, int value) {
    return this.wrapped.setShort(index, value);
  }

  @Override
  public ByteBuf setShortLE(int index, int value) {
    return this.wrapped.setShortLE(index, value);
  }

  @Override
  public ByteBuf setMedium(int index, int value) {
    return this.wrapped.setMedium(index, value);
  }

  @Override
  public ByteBuf setMediumLE(int index, int value) {
    return this.wrapped.setMediumLE(index, value);
  }

  @Override
  public ByteBuf setInt(int index, int value) {
    return this.wrapped.setInt(index, value);
  }

  @Override
  public ByteBuf setIntLE(int index, int value) {
    return this.wrapped.setIntLE(index, value);
  }

  @Override
  public ByteBuf setLong(int index, long value) {
    return this.wrapped.setLong(index, value);
  }

  @Override
  public ByteBuf setLongLE(int index, long value) {
    return this.wrapped.setLongLE(index, value);
  }

  @Override
  public ByteBuf setChar(int index, int value) {
    return this.wrapped.setChar(index, value);
  }

  @Override
  public ByteBuf setFloat(int index, float value) {
    return this.wrapped.setFloat(index, value);
  }

  @Override
  public ByteBuf setFloatLE(int index, float value) {
    return this.wrapped.setFloatLE(index, value);
  }

  @Override
  public ByteBuf setDouble(int index, double value) {
    return this.wrapped.setDouble(index, value);
  }

  @Override
  public ByteBuf setDoubleLE(int index, double value) {
    return this.wrapped.setDoubleLE(index, value);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src) {
    return this.wrapped.setBytes(index, src);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int length) {
    return this.wrapped.setBytes(index, src, length);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
    return this.wrapped.setBytes(index, src, srcIndex, length);
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src) {
    return this.wrapped.setBytes(index, src);
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
    return this.wrapped.setBytes(index, src, srcIndex, length);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuffer src) {
    return this.wrapped.setBytes(index, src);
  }

  @Override
  public int setBytes(int index, InputStream in, int length) throws IOException {
    return this.wrapped.setBytes(index, in, length);
  }

  @Override
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
    return this.wrapped.setBytes(index, in, length);
  }

  @Override
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
    return this.wrapped.setBytes(index, in, position, length);
  }

  @Override
  public ByteBuf setZero(int index, int length) {
    return this.wrapped.setZero(index, length);
  }

  @Override
  public int setCharSequence(int index, CharSequence sequence, Charset charset) {
    return this.wrapped.setCharSequence(index, sequence, charset);
  }

  @Override
  public boolean readBoolean() {
    return this.wrapped.readBoolean();
  }

  @Override
  public byte readByte() {
    return this.wrapped.readByte();
  }

  @Override
  public short readUnsignedByte() {
    return this.wrapped.readUnsignedByte();
  }

  @Override
  public short readShort() {
    return this.wrapped.readShort();
  }

  @Override
  public short readShortLE() {
    return this.wrapped.readShortLE();
  }

  @Override
  public int readUnsignedShort() {
    return this.wrapped.readUnsignedShort();
  }

  @Override
  public int readUnsignedShortLE() {
    return this.wrapped.readUnsignedShortLE();
  }

  @Override
  public int readMedium() {
    return this.wrapped.readMedium();
  }

  @Override
  public int readMediumLE() {
    return this.wrapped.readMediumLE();
  }

  @Override
  public int readUnsignedMedium() {
    return this.wrapped.readUnsignedMedium();
  }

  @Override
  public int readUnsignedMediumLE() {
    return this.wrapped.readUnsignedMediumLE();
  }

  @Override
  public int readInt() {
    return this.wrapped.readInt();
  }

  @Override
  public int readIntLE() {
    return this.wrapped.readIntLE();
  }

  @Override
  public long readUnsignedInt() {
    return this.wrapped.readUnsignedInt();
  }

  @Override
  public long readUnsignedIntLE() {
    return this.wrapped.readUnsignedIntLE();
  }

  @Override
  public long readLong() {
    return this.wrapped.readLong();
  }

  @Override
  public long readLongLE() {
    return this.wrapped.readLongLE();
  }

  @Override
  public char readChar() {
    return this.wrapped.readChar();
  }

  @Override
  public float readFloat() {
    return this.wrapped.readFloat();
  }

  @Override
  public float readFloatLE() {
    return this.wrapped.readFloatLE();
  }

  @Override
  public double readDouble() {
    return this.wrapped.readDouble();
  }

  @Override
  public double readDoubleLE() {
    return this.wrapped.readDoubleLE();
  }

  @Override
  public ByteBuf readBytes(int length) {
    return this.wrapped.readBytes(length);
  }

  @Override
  public ByteBuf readSlice(int length) {
    return this.wrapped.readSlice(length);
  }

  @Override
  public ByteBuf readRetainedSlice(int length) {
    return this.wrapped.readRetainedSlice(length);
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst) {
    return this.wrapped.readBytes(dst);
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst, int length) {
    return this.wrapped.readBytes(dst, length);
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
    return this.wrapped.readBytes(dst, dstIndex, length);
  }

  @Override
  public ByteBuf readBytes(byte[] dst) {
    return this.wrapped.readBytes(dst);
  }

  @Override
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
    return this.wrapped.readBytes(dst, dstIndex, length);
  }

  @Override
  public ByteBuf readBytes(ByteBuffer dst) {
    return this.wrapped.readBytes(dst);
  }

  @Override
  public ByteBuf readBytes(OutputStream out, int length) throws IOException {
    return this.wrapped.readBytes(out, length);
  }

  @Override
  public int readBytes(GatheringByteChannel out, int length) throws IOException {
    return this.wrapped.readBytes(out, length);
  }

  @Override
  public CharSequence readCharSequence(int length, Charset charset) {
    return this.wrapped.readCharSequence(length, charset);
  }

  @Override
  public int readBytes(FileChannel out, long position, int length) throws IOException {
    return this.wrapped.readBytes(out, position, length);
  }

  @Override
  public ByteBuf skipBytes(int length) {
    return this.wrapped.skipBytes(length);
  }

  @Override
  public ProtocolBuffer writeBoolean(boolean value) {
    this.wrapped.writeBoolean(value);
    return this;
  }

  @Override
  public ProtocolBuffer writeByte(int value) {
    this.wrapped.writeByte(value);
    return this;
  }

  @Override
  public ProtocolBuffer writeShort(int value) {
    this.wrapped.writeShort(value);
    return this;
  }

  @Override
  public ByteBuf writeShortLE(int value) {
    return this.wrapped.writeShortLE(value);
  }

  @Override
  public ByteBuf writeMedium(int value) {
    return this.wrapped.writeMedium(value);
  }

  @Override
  public ByteBuf writeMediumLE(int value) {
    return this.wrapped.writeMediumLE(value);
  }

  @Override
  public ProtocolBuffer writeInt(int value) {
    this.wrapped.writeInt(value);
    return this;
  }

  @Override
  public ByteBuf writeIntLE(int value) {
    return this.wrapped.writeIntLE(value);
  }

  @Override
  public ProtocolBuffer writeLong(long value) {
    this.wrapped.writeLong(value);
    return this;
  }

  @Override
  public ByteBuf writeLongLE(long value) {
    return this.wrapped.writeLongLE(value);
  }

  @Override
  public ByteBuf writeChar(int value) {
    return this.wrapped.writeChar(value);
  }

  @Override
  public ProtocolBuffer writeFloat(float value) {
    this.wrapped.writeFloat(value);
    return this;
  }

  @Override
  public ByteBuf writeFloatLE(float value) {
    return this.wrapped.writeFloatLE(value);
  }

  @Override
  public ProtocolBuffer writeDouble(double value) {
    this.wrapped.writeDouble(value);
    return this;
  }

  @Override
  public ByteBuf writeDoubleLE(double value) {
    return this.wrapped.writeDoubleLE(value);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src) {
    return this.wrapped.writeBytes(src);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src, int length) {
    return this.wrapped.writeBytes(src, length);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
    return this.wrapped.writeBytes(src, srcIndex, length);
  }

  @Override
  public ByteBuf writeBytes(byte[] src) {
    return this.wrapped.writeBytes(src);
  }

  @Override
  public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
    return this.wrapped.writeBytes(src, srcIndex, length);
  }

  @Override
  public ByteBuf writeBytes(ByteBuffer src) {
    return this.wrapped.writeBytes(src);
  }

  @Override
  public int writeBytes(InputStream in, int length) throws IOException {
    return this.wrapped.writeBytes(in, length);
  }

  @Override
  public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
    return this.wrapped.writeBytes(in, length);
  }

  @Override
  public int writeBytes(FileChannel in, long position, int length) throws IOException {
    return this.wrapped.writeBytes(in, position, length);
  }

  @Override
  public ByteBuf writeZero(int length) {
    return this.wrapped.writeZero(length);
  }

  @Override
  public int writeCharSequence(CharSequence sequence, Charset charset) {
    return this.wrapped.writeCharSequence(sequence, charset);
  }

  @Override
  public int indexOf(int fromIndex, int toIndex, byte value) {
    return this.wrapped.indexOf(fromIndex, toIndex, value);
  }

  @Override
  public int bytesBefore(byte value) {
    return this.wrapped.bytesBefore(value);
  }

  @Override
  public int bytesBefore(int length, byte value) {
    return this.wrapped.bytesBefore(length, value);
  }

  @Override
  public int bytesBefore(int index, int length, byte value) {
    return this.wrapped.bytesBefore(index, length, value);
  }

  @Override
  public int forEachByte(ByteProcessor processor) {
    return this.wrapped.forEachByte(processor);
  }

  @Override
  public int forEachByte(int index, int length, ByteProcessor processor) {
    return this.wrapped.forEachByte(index, length, processor);
  }

  @Override
  public int forEachByteDesc(ByteProcessor processor) {
    return this.wrapped.forEachByteDesc(processor);
  }

  @Override
  public int forEachByteDesc(int index, int length, ByteProcessor processor) {
    return this.wrapped.forEachByteDesc(index, length, processor);
  }

  @Override
  public ByteBuf copy() {
    return this.wrapped.copy();
  }

  @Override
  public ByteBuf copy(int index, int length) {
    return this.wrapped.copy(index, length);
  }

  @Override
  public ByteBuf slice() {
    return this.wrapped.slice();
  }

  @Override
  public ByteBuf retainedSlice() {
    return this.wrapped.retainedSlice();
  }

  @Override
  public ByteBuf slice(int index, int length) {
    return this.wrapped.slice(index, length);
  }

  @Override
  public ByteBuf retainedSlice(int index, int length) {
    return this.wrapped.retainedSlice(index, length);
  }

  @Override
  public ByteBuf duplicate() {
    return this.wrapped.duplicate();
  }

  @Override
  public ByteBuf retainedDuplicate() {
    return this.wrapped.retainedDuplicate();
  }

  @Override
  public int nioBufferCount() {
    return this.wrapped.nioBufferCount();
  }

  @Override
  public ByteBuffer nioBuffer() {
    return this.wrapped.nioBuffer();
  }

  @Override
  public ByteBuffer nioBuffer(int index, int length) {
    return this.wrapped.nioBuffer(index, length);
  }

  @Override
  public ByteBuffer internalNioBuffer(int index, int length) {
    return this.wrapped.internalNioBuffer(index, length);
  }

  @Override
  public ByteBuffer[] nioBuffers() {
    return this.wrapped.nioBuffers();
  }

  @Override
  public ByteBuffer[] nioBuffers(int index, int length) {
    return this.wrapped.nioBuffers(index, length);
  }

  @Override
  public boolean hasArray() {
    return this.wrapped.hasArray();
  }

  @Override
  public byte[] array() {
    return this.wrapped.array();
  }

  @Override
  public int arrayOffset() {
    return this.wrapped.arrayOffset();
  }

  @Override
  public boolean hasMemoryAddress() {
    return this.wrapped.hasMemoryAddress();
  }

  @Override
  public long memoryAddress() {
    return this.wrapped.memoryAddress();
  }

  @Override
  public String toString(Charset charset) {
    return this.wrapped.toString(charset);
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    return this.wrapped.toString(index, length, charset);
  }

  @Override
  public int hashCode() {
    return this.wrapped.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this.wrapped.equals(obj);
  }

  @Override
  public int compareTo(ByteBuf buffer) {
    return this.wrapped.compareTo(buffer);
  }

  @Override
  public String toString() {
    return this.wrapped.toString();
  }

  @Override
  public ByteBuf retain(int increment) {
    return this.wrapped.retain(increment);
  }

  @Override
  public ByteBuf retain() {
    return this.wrapped.retain();
  }

  @Override
  public ByteBuf touch() {
    return this.wrapped.touch();
  }

  @Override
  public ByteBuf touch(Object hint) {
    return this.wrapped.touch(hint);
  }

  @Override
  public int refCnt() {
    return this.wrapped.refCnt();
  }

  @Override
  public boolean release() {
    return this.wrapped.release();
  }

  @Override
  public boolean release(int decrement) {
    return this.wrapped.release(decrement);
  }
}
