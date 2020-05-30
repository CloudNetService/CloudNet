package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;

public class NetworkClusterNode extends BasicJsonDocPropertyable implements SerializableObject {

    private String uniqueId;

    private HostAndPort[] listeners;

    public NetworkClusterNode(String uniqueId, HostAndPort[] listeners) {
        this.uniqueId = uniqueId;
        this.listeners = listeners;
    }

    public String getUniqueId() {
        return this.uniqueId;
    }

    public HostAndPort[] getListeners() {
        return this.listeners;
    }

    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeString(this.uniqueId);
        buffer.writeObjectArray(this.listeners);

        buffer.writeString(super.properties.toJson());
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.uniqueId = buffer.readString();
        this.listeners = buffer.readObjectArray(HostAndPort.class);

        super.properties = JsonDocument.newDocument(buffer.readString());
    }
}