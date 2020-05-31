package de.dytanic.cloudnet.driver.serialization.json;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableJsonDocument;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;

public class SerializableJsonDocPropertyable extends BasicJsonDocPropertyable implements SerializableObject {
    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeObject(SerializableJsonDocument.asSerializable(super.properties));
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        super.properties = buffer.readObject(SerializableJsonDocument.class);
    }
}
