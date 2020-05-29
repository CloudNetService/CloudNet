package de.dytanic.cloudnet.driver.serialization;

public class ObjectSerialization {

    private ObjectSerialization() {
        throw new UnsupportedOperationException();
    }

    public static <T extends SerializableObject> T deserialize(Class<T> objectClass, byte[] bytes) {
        return ProtocolBuffer.wrap(bytes).readObject(objectClass);
    }

    public static <T extends SerializableObject> T[] deserializeArray(Class<T> objectClass, byte[] bytes) {
        return ProtocolBuffer.wrap(bytes).readObjectArray(objectClass);
    }

    public static byte[] serialize(SerializableObject object) {
        ProtocolBuffer buffer = ProtocolBuffer.create();
        buffer.writeObject(object);
        return buffer.toArray();
    }

    public static byte[] serialize(SerializableObject[] object) {
        ProtocolBuffer buffer = ProtocolBuffer.create();
        buffer.writeObjectArray(object);
        return buffer.toArray();
    }

}
