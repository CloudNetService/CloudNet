package de.dytanic.cloudnet.driver.serialization.json;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import org.jetbrains.annotations.NotNull;

public class SerializableJsonDocPropertyable extends BasicJsonDocPropertyable implements SerializableObject {
    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeJsonDocument(super.properties);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        super.properties = buffer.readJsonDocument();
    }
}
