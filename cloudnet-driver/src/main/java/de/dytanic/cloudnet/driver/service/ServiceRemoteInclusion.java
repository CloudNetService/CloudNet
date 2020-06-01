package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceRemoteInclusion extends SerializableJsonDocPropertyable implements SerializableObject {

    private String url;

    private String destination;

    public ServiceRemoteInclusion(String url, String destination) {
        this.url = url;
        this.destination = destination;
    }

    public ServiceRemoteInclusion() {
    }

    public String getUrl() {
        return this.url;
    }

    public String getDestination() {
        return this.destination;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(this.url);
        buffer.writeString(this.destination);

        super.write(buffer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.url = buffer.readString();
        this.destination = buffer.readString();

        super.read(buffer);
    }
}