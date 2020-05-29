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

    public abstract void writeString(@NotNull String stringToWrite);

    @NotNull
    public abstract byte[] readArray();

    public abstract void writeArray(@NotNull byte[] bytes);

    @NotNull
    public abstract byte[] toArray();

    @NotNull
    public abstract Collection<String> readStringCollection();

    public abstract void writeStringCollection(@NotNull Collection<String> list);

    public abstract int readVarInt();

    public abstract void writeVarInt(int value);

    public abstract long readVarLong();

    public abstract void writeVarLong(long value);

    @NotNull
    public abstract UUID readUUID();

    public abstract void writeUUID(@NotNull UUID uniqueId);

    @NotNull
    public abstract <T extends SerializableObject> T readObject(@NotNull Class<T> objectClass);

    public abstract <T extends SerializableObject> T readObject(@NotNull T targetObject);

    public abstract void writeObject(@NotNull SerializableObject object);

    @NotNull
    public abstract <T extends SerializableObject> Collection<T> readObjectCollection(@NotNull Class<T> objectClass);

    public abstract void writeObjectCollection(@NotNull Collection<? extends SerializableObject> objects);

    @NotNull
    public abstract <T extends SerializableObject> T[] readObjectArray(@NotNull Class<T> objectClass);

    public abstract <T extends SerializableObject> void writeObjectArray(@NotNull T[] objects);

    public abstract <E extends Enum<E>> E readEnumConstant(Class<E> enumClass);

    public abstract <E extends Enum<E>> void writeEnumConstant(E enumConstant);

}
