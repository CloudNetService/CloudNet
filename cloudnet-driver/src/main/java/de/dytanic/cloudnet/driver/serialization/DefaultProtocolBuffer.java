package de.dytanic.cloudnet.driver.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ByteProcessor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
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

public class DefaultProtocolBuffer extends ProtocolBuffer {

    private final ByteBuf wrapped;

    public DefaultProtocolBuffer(ByteBuf wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void writeString(@NotNull String stringToWrite) {
        this.writeArray(stringToWrite.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public @NotNull String readString() {
        return new String(this.readArray(), StandardCharsets.UTF_8);
    }

    @Override
    public void writeArray(@NotNull byte[] bytes) {
        this.writeVarInt(bytes.length);
        this.writeBytes(bytes);
    }

    @Override
    public @NotNull byte[] readArray() {
        int length = this.readVarInt();

        byte[] bytes = new byte[length];
        this.readBytes(bytes);

        return bytes;
    }

    @Override
    public @NotNull byte[] toArray() {
        byte[] bytes = new byte[this.readableBytes()];
        this.readBytes(bytes);
        return bytes;
    }

    @Override
    public void writeStringCollection(@NotNull Collection<String> list) {
        this.writeVarInt(list.size());
        for (String s : list) {
            this.writeString(s);
        }
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
        int numRead = 0;
        int result = 0;
        byte read;

        do {
            read = this.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            if (numRead++ > 5) {
                throw new IllegalArgumentException("VarInt is too big!");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    @Override
    public void writeVarInt(int value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }

            this.writeByte(temp);
        } while (value != 0);
    }

    @Override
    public long readVarLong() {
        int numRead = 0;
        long result = 0;
        byte read;
        do {
            read = this.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            if (numRead++ > 10) {
                throw new IllegalArgumentException("VarInt is too big!");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    @Override
    public void writeVarLong(long value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }

            this.writeByte(temp);
        } while (value != 0);
    }

    @Override
    public @NotNull UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    @Override
    public void writeUUID(@NotNull UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
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
    public <T extends SerializableObject> T readObject(@NotNull T targetObject) {
        targetObject.read(this);
        return targetObject;
    }

    @Override
    public void writeObject(@NotNull SerializableObject object) {
        object.write(this);
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
    public void writeObjectCollection(@NotNull Collection<? extends SerializableObject> objects) {
        this.writeVarInt(objects.size());
        for (SerializableObject object : objects) {
            this.writeObject(object);
        }
    }

    @Override
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
    public <T extends SerializableObject> void writeObjectArray(@NotNull T[] objects) {
        this.writeVarInt(objects.length);
        for (T object : objects) {
            this.writeObject(object);
        }
    }

    @Override
    public <E extends Enum<E>> E readEnumConstant(Class<E> enumClass) {
        return enumClass.getEnumConstants()[this.readVarInt()];
    }

    @Override
    public <E extends Enum<E>> void writeEnumConstant(E enumConstant) {
        this.writeVarInt(enumConstant.ordinal());
    }

    @Override
    public int capacity() {
        return wrapped.capacity();
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        return wrapped.capacity(newCapacity);
    }

    @Override
    public int maxCapacity() {
        return wrapped.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return wrapped.alloc();
    }

    @Override
    @Deprecated
    public ByteOrder order() {
        return wrapped.order();
    }

    @Override
    @Deprecated
    public ByteBuf order(ByteOrder endianness) {
        return wrapped.order(endianness);
    }

    @Override
    public ByteBuf unwrap() {
        return wrapped.unwrap();
    }

    @Override
    public boolean isDirect() {
        return wrapped.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return wrapped.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return wrapped.asReadOnly();
    }

    @Override
    public int readerIndex() {
        return wrapped.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int readerIndex) {
        return wrapped.readerIndex(readerIndex);
    }

    @Override
    public int writerIndex() {
        return wrapped.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int writerIndex) {
        return wrapped.writerIndex(writerIndex);
    }

    @Override
    public ByteBuf setIndex(int readerIndex, int writerIndex) {
        return wrapped.setIndex(readerIndex, writerIndex);
    }

    @Override
    public int readableBytes() {
        return wrapped.readableBytes();
    }

    @Override
    public int writableBytes() {
        return wrapped.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return wrapped.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return wrapped.isReadable();
    }

    @Override
    public boolean isReadable(int size) {
        return wrapped.isReadable(size);
    }

    @Override
    public boolean isWritable() {
        return wrapped.isWritable();
    }

    @Override
    public boolean isWritable(int size) {
        return wrapped.isWritable(size);
    }

    @Override
    public ByteBuf clear() {
        return wrapped.clear();
    }

    @Override
    public ByteBuf markReaderIndex() {
        return wrapped.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return wrapped.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex() {
        return wrapped.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return wrapped.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes() {
        return wrapped.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return wrapped.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable(int minWritableBytes) {
        return wrapped.ensureWritable(minWritableBytes);
    }

    @Override
    public int ensureWritable(int minWritableBytes, boolean force) {
        return wrapped.ensureWritable(minWritableBytes, force);
    }

    @Override
    public boolean getBoolean(int index) {
        return wrapped.getBoolean(index);
    }

    @Override
    public byte getByte(int index) {
        return wrapped.getByte(index);
    }

    @Override
    public short getUnsignedByte(int index) {
        return wrapped.getUnsignedByte(index);
    }

    @Override
    public short getShort(int index) {
        return wrapped.getShort(index);
    }

    @Override
    public short getShortLE(int index) {
        return wrapped.getShortLE(index);
    }

    @Override
    public int getUnsignedShort(int index) {
        return wrapped.getUnsignedShort(index);
    }

    @Override
    public int getUnsignedShortLE(int index) {
        return wrapped.getUnsignedShortLE(index);
    }

    @Override
    public int getMedium(int index) {
        return wrapped.getMedium(index);
    }

    @Override
    public int getMediumLE(int index) {
        return wrapped.getMediumLE(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        return wrapped.getUnsignedMedium(index);
    }

    @Override
    public int getUnsignedMediumLE(int index) {
        return wrapped.getUnsignedMediumLE(index);
    }

    @Override
    public int getInt(int index) {
        return wrapped.getInt(index);
    }

    @Override
    public int getIntLE(int index) {
        return wrapped.getIntLE(index);
    }

    @Override
    public long getUnsignedInt(int index) {
        return wrapped.getUnsignedInt(index);
    }

    @Override
    public long getUnsignedIntLE(int index) {
        return wrapped.getUnsignedIntLE(index);
    }

    @Override
    public long getLong(int index) {
        return wrapped.getLong(index);
    }

    @Override
    public long getLongLE(int index) {
        return wrapped.getLongLE(index);
    }

    @Override
    public char getChar(int index) {
        return wrapped.getChar(index);
    }

    @Override
    public float getFloat(int index) {
        return wrapped.getFloat(index);
    }

    @Override
    public float getFloatLE(int index) {
        return wrapped.getFloatLE(index);
    }

    @Override
    public double getDouble(int index) {
        return wrapped.getDouble(index);
    }

    @Override
    public double getDoubleLE(int index) {
        return wrapped.getDoubleLE(index);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst) {
        return wrapped.getBytes(index, dst);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
        return wrapped.getBytes(index, dst, length);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        return wrapped.getBytes(index, dst, dstIndex, length);
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst) {
        return wrapped.getBytes(index, dst);
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        return wrapped.getBytes(index, dst, dstIndex, length);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        return wrapped.getBytes(index, dst);
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        return wrapped.getBytes(index, out, length);
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return wrapped.getBytes(index, out, length);
    }

    @Override
    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        return wrapped.getBytes(index, out, position, length);
    }

    @Override
    public CharSequence getCharSequence(int index, int length, Charset charset) {
        return wrapped.getCharSequence(index, length, charset);
    }

    @Override
    public ByteBuf setBoolean(int index, boolean value) {
        return wrapped.setBoolean(index, value);
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        return wrapped.setByte(index, value);
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        return wrapped.setShort(index, value);
    }

    @Override
    public ByteBuf setShortLE(int index, int value) {
        return wrapped.setShortLE(index, value);
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        return wrapped.setMedium(index, value);
    }

    @Override
    public ByteBuf setMediumLE(int index, int value) {
        return wrapped.setMediumLE(index, value);
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        return wrapped.setInt(index, value);
    }

    @Override
    public ByteBuf setIntLE(int index, int value) {
        return wrapped.setIntLE(index, value);
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        return wrapped.setLong(index, value);
    }

    @Override
    public ByteBuf setLongLE(int index, long value) {
        return wrapped.setLongLE(index, value);
    }

    @Override
    public ByteBuf setChar(int index, int value) {
        return wrapped.setChar(index, value);
    }

    @Override
    public ByteBuf setFloat(int index, float value) {
        return wrapped.setFloat(index, value);
    }

    @Override
    public ByteBuf setFloatLE(int index, float value) {
        return wrapped.setFloatLE(index, value);
    }

    @Override
    public ByteBuf setDouble(int index, double value) {
        return wrapped.setDouble(index, value);
    }

    @Override
    public ByteBuf setDoubleLE(int index, double value) {
        return wrapped.setDoubleLE(index, value);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src) {
        return wrapped.setBytes(index, src);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int length) {
        return wrapped.setBytes(index, src, length);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        return wrapped.setBytes(index, src, srcIndex, length);
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src) {
        return wrapped.setBytes(index, src);
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        return wrapped.setBytes(index, src, srcIndex, length);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        return wrapped.setBytes(index, src);
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        return wrapped.setBytes(index, in, length);
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return wrapped.setBytes(index, in, length);
    }

    @Override
    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
        return wrapped.setBytes(index, in, position, length);
    }

    @Override
    public ByteBuf setZero(int index, int length) {
        return wrapped.setZero(index, length);
    }

    @Override
    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
        return wrapped.setCharSequence(index, sequence, charset);
    }

    @Override
    public boolean readBoolean() {
        return wrapped.readBoolean();
    }

    @Override
    public byte readByte() {
        return wrapped.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return wrapped.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return wrapped.readShort();
    }

    @Override
    public short readShortLE() {
        return wrapped.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return wrapped.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return wrapped.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return wrapped.readMedium();
    }

    @Override
    public int readMediumLE() {
        return wrapped.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return wrapped.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return wrapped.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return wrapped.readInt();
    }

    @Override
    public int readIntLE() {
        return wrapped.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return wrapped.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return wrapped.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return wrapped.readLong();
    }

    @Override
    public long readLongLE() {
        return wrapped.readLongLE();
    }

    @Override
    public char readChar() {
        return wrapped.readChar();
    }

    @Override
    public float readFloat() {
        return wrapped.readFloat();
    }

    @Override
    public float readFloatLE() {
        return wrapped.readFloatLE();
    }

    @Override
    public double readDouble() {
        return wrapped.readDouble();
    }

    @Override
    public double readDoubleLE() {
        return wrapped.readDoubleLE();
    }

    @Override
    public ByteBuf readBytes(int length) {
        return wrapped.readBytes(length);
    }

    @Override
    public ByteBuf readSlice(int length) {
        return wrapped.readSlice(length);
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {
        return wrapped.readRetainedSlice(length);
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst) {
        return wrapped.readBytes(dst);
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int length) {
        return wrapped.readBytes(dst, length);
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        return wrapped.readBytes(dst, dstIndex, length);
    }

    @Override
    public ByteBuf readBytes(byte[] dst) {
        return wrapped.readBytes(dst);
    }

    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        return wrapped.readBytes(dst, dstIndex, length);
    }

    @Override
    public ByteBuf readBytes(ByteBuffer dst) {
        return wrapped.readBytes(dst);
    }

    @Override
    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        return wrapped.readBytes(out, length);
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return wrapped.readBytes(out, length);
    }

    @Override
    public CharSequence readCharSequence(int length, Charset charset) {
        return wrapped.readCharSequence(length, charset);
    }

    @Override
    public int readBytes(FileChannel out, long position, int length) throws IOException {
        return wrapped.readBytes(out, position, length);
    }

    @Override
    public ByteBuf skipBytes(int length) {
        return wrapped.skipBytes(length);
    }

    @Override
    public ByteBuf writeBoolean(boolean value) {
        return wrapped.writeBoolean(value);
    }

    @Override
    public ByteBuf writeByte(int value) {
        return wrapped.writeByte(value);
    }

    @Override
    public ByteBuf writeShort(int value) {
        return wrapped.writeShort(value);
    }

    @Override
    public ByteBuf writeShortLE(int value) {
        return wrapped.writeShortLE(value);
    }

    @Override
    public ByteBuf writeMedium(int value) {
        return wrapped.writeMedium(value);
    }

    @Override
    public ByteBuf writeMediumLE(int value) {
        return wrapped.writeMediumLE(value);
    }

    @Override
    public ByteBuf writeInt(int value) {
        return wrapped.writeInt(value);
    }

    @Override
    public ByteBuf writeIntLE(int value) {
        return wrapped.writeIntLE(value);
    }

    @Override
    public ByteBuf writeLong(long value) {
        return wrapped.writeLong(value);
    }

    @Override
    public ByteBuf writeLongLE(long value) {
        return wrapped.writeLongLE(value);
    }

    @Override
    public ByteBuf writeChar(int value) {
        return wrapped.writeChar(value);
    }

    @Override
    public ByteBuf writeFloat(float value) {
        return wrapped.writeFloat(value);
    }

    @Override
    public ByteBuf writeFloatLE(float value) {
        return wrapped.writeFloatLE(value);
    }

    @Override
    public ByteBuf writeDouble(double value) {
        return wrapped.writeDouble(value);
    }

    @Override
    public ByteBuf writeDoubleLE(double value) {
        return wrapped.writeDoubleLE(value);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src) {
        return wrapped.writeBytes(src);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int length) {
        return wrapped.writeBytes(src, length);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        return wrapped.writeBytes(src, srcIndex, length);
    }

    @Override
    public ByteBuf writeBytes(byte[] src) {
        return wrapped.writeBytes(src);
    }

    @Override
    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        return wrapped.writeBytes(src, srcIndex, length);
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer src) {
        return wrapped.writeBytes(src);
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        return wrapped.writeBytes(in, length);
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return wrapped.writeBytes(in, length);
    }

    @Override
    public int writeBytes(FileChannel in, long position, int length) throws IOException {
        return wrapped.writeBytes(in, position, length);
    }

    @Override
    public ByteBuf writeZero(int length) {
        return wrapped.writeZero(length);
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return wrapped.writeCharSequence(sequence, charset);
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        return wrapped.indexOf(fromIndex, toIndex, value);
    }

    @Override
    public int bytesBefore(byte value) {
        return wrapped.bytesBefore(value);
    }

    @Override
    public int bytesBefore(int length, byte value) {
        return wrapped.bytesBefore(length, value);
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        return wrapped.bytesBefore(index, length, value);
    }

    @Override
    public int forEachByte(ByteProcessor processor) {
        return wrapped.forEachByte(processor);
    }

    @Override
    public int forEachByte(int index, int length, ByteProcessor processor) {
        return wrapped.forEachByte(index, length, processor);
    }

    @Override
    public int forEachByteDesc(ByteProcessor processor) {
        return wrapped.forEachByteDesc(processor);
    }

    @Override
    public int forEachByteDesc(int index, int length, ByteProcessor processor) {
        return wrapped.forEachByteDesc(index, length, processor);
    }

    @Override
    public ByteBuf copy() {
        return wrapped.copy();
    }

    @Override
    public ByteBuf copy(int index, int length) {
        return wrapped.copy(index, length);
    }

    @Override
    public ByteBuf slice() {
        return wrapped.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return wrapped.retainedSlice();
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return wrapped.slice(index, length);
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return wrapped.retainedSlice(index, length);
    }

    @Override
    public ByteBuf duplicate() {
        return wrapped.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return wrapped.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return wrapped.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return wrapped.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        return wrapped.nioBuffer(index, length);
    }

    @Override
    public ByteBuffer internalNioBuffer(int index, int length) {
        return wrapped.internalNioBuffer(index, length);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return wrapped.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        return wrapped.nioBuffers(index, length);
    }

    @Override
    public boolean hasArray() {
        return wrapped.hasArray();
    }

    @Override
    public byte[] array() {
        return wrapped.array();
    }

    @Override
    public int arrayOffset() {
        return wrapped.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return wrapped.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return wrapped.memoryAddress();
    }

    @Override
    public String toString(Charset charset) {
        return wrapped.toString(charset);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        return wrapped.toString(index, length, charset);
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return wrapped.equals(obj);
    }

    @Override
    public int compareTo(ByteBuf buffer) {
        return wrapped.compareTo(buffer);
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }

    @Override
    public ByteBuf retain(int increment) {
        return wrapped.retain(increment);
    }

    @Override
    public ByteBuf retain() {
        return wrapped.retain();
    }

    @Override
    public ByteBuf touch() {
        return wrapped.touch();
    }

    @Override
    public ByteBuf touch(Object hint) {
        return wrapped.touch(hint);
    }

    @Override
    public int refCnt() {
        return wrapped.refCnt();
    }

    @Override
    public boolean release() {
        return wrapped.release();
    }

    @Override
    public boolean release(int decrement) {
        return wrapped.release(decrement);
    }
}
