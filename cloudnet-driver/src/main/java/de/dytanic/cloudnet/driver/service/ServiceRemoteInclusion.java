package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;

public final class ServiceRemoteInclusion extends BasicJsonDocPropertyable implements SerializableObject {

    private String url;

    private String destination;

    public ServiceRemoteInclusion(String url, String destination) {
        this.url = url;
        this.destination = destination;
    }

    public String getUrl() {
        return this.url;
    }

    public String getDestination() {
        return this.destination;
    }

    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeString(this.url);
        buffer.writeString(this.destination);

        buffer.writeString(super.properties.toJson());
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.url = buffer.readString();
        this.destination = buffer.readString();

        super.properties = JsonDocument.newDocument(buffer.readString());
    }
}