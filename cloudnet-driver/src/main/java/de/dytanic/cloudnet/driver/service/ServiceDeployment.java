package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceDeployment extends SerializableJsonDocPropertyable implements SerializableObject {

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
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeObject(this.template);
        buffer.writeStringCollection(this.excludes);

        super.write(buffer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.template = buffer.readObject(ServiceTemplate.class);
        this.excludes = buffer.readStringCollection();

        super.read(buffer);
    }
}