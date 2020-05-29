package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceDeployment extends BasicJsonDocPropertyable implements SerializableObject {

    private ServiceTemplate template;

    private Collection<String> excludes;

    public ServiceDeployment(ServiceTemplate template, Collection<String> excludes) {
        this.template = template;
        this.excludes = excludes;
    }

    public ServiceDeployment() {
    }

    public ServiceTemplate getTemplate() {
        return this.template;
    }

    public Collection<String> getExcludes() {
        return this.excludes;
    }

    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeObject(this.template);
        buffer.writeStringCollection(this.excludes);

        buffer.writeString(super.properties.toJson());
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.template = buffer.readObject(ServiceTemplate.class);
        this.excludes = buffer.readStringCollection();

        super.properties = JsonDocument.newDocument(buffer.readString());
    }
}