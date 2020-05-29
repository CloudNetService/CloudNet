package de.dytanic.cloudnet.driver.serialization;

public interface SerializableObject {

    void write(ProtocolBuffer buffer);

    void read(ProtocolBuffer buffer);

}
