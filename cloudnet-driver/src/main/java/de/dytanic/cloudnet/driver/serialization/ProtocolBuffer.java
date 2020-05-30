package de.dytanic.cloudnet.driver.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public abstract class ProtocolBuffer extends ByteBuf {

    public static ProtocolBuffer create() {
        return wrap(Unpooled.buffer());
    }

    public static ProtocolBuffer wrap(byte[] bytes) {
        return wrap(Unpooled.wrappedBuffer(bytes));
    }

    public static ProtocolBuffer wrap(ByteBuf buf) {
        return new DefaultProtocolBuffer(buf);
    }


    @NotNull
    public abstract String readString();

    public abstract ProtocolBuffer writeString(@NotNull String stringToWrite);

    @NotNull
    public abstract byte[] readArray();

    public abstract ProtocolBuffer writeArray(@NotNull byte[] bytes);

    @NotNull
    public abstract byte[] toArray();

    @NotNull
    public abstract Collection<String> readStringCollection();

    public abstract ProtocolBuffer writeStringCollection(@NotNull Collection<String> list);

    public abstract int readVarInt();

    public abstract ProtocolBuffer writeVarInt(int value);

    public abstract long readVarLong();

    public abstract ProtocolBuffer writeVarLong(long value);

    @NotNull
    public abstract UUID readUUID();

    public abstract ProtocolBuffer writeUUID(@NotNull UUID uniqueId);

    @NotNull
    public abstract <T extends SerializableObject> T readObject(@NotNull Class<T> objectClass);

    public abstract <T extends SerializableObject> T readObject(@NotNull T targetObject);

    public abstract ProtocolBuffer writeObject(@NotNull SerializableObject object);

    @NotNull
    public abstract <T extends SerializableObject> Collection<T> readObjectCollection(@NotNull Class<T> objectClass);

    public abstract ProtocolBuffer writeObjectCollection(@NotNull Collection<? extends SerializableObject> objects);

    @NotNull
    public abstract <T extends SerializableObject> T[] readObjectArray(@NotNull Class<T> objectClass);

    public abstract <T extends SerializableObject> ProtocolBuffer writeObjectArray(@NotNull T[] objects);

    public abstract <E extends Enum<E>> E readEnumConstant(Class<E> enumClass);

    public abstract <E extends Enum<E>> ProtocolBuffer writeEnumConstant(E enumConstant);


    @Override
    public abstract ProtocolBuffer writeBoolean(boolean value);

    @Override
    public abstract ProtocolBuffer writeByte(int value);

    @Override
    public abstract ProtocolBuffer writeShort(int value);

    @Override
    public abstract ProtocolBuffer writeInt(int value);

    @Override
    public abstract ProtocolBuffer writeLong(long value);

    @Override
    public abstract ProtocolBuffer writeFloat(float value);

    @Override
    public abstract ProtocolBuffer writeDouble(double value);
}
