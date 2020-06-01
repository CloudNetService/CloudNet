package de.dytanic.cloudnet.driver.channel;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public class ChannelMessageTarget implements SerializableObject {

    private Type type;
    private String name;
    private ServiceEnvironmentType environment;

    public ChannelMessageTarget(@NotNull Type type, @Nullable String name) {
        this.type = type;
        this.name = name;
    }

    public ChannelMessageTarget(@NotNull ServiceEnvironmentType environment) {
        this.type = Type.ENVIRONMENT;
        this.environment = environment;
    }

    public ChannelMessageTarget() {
    }

    public Type getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public ServiceEnvironmentType getEnvironment() {
        return this.environment;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeEnumConstant(this.type);
        buffer.writeOptionalString(this.name);
        buffer.writeOptionalEnumConstant(this.environment);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.type = buffer.readEnumConstant(Type.class);
        this.name = buffer.readOptionalString();
        this.environment = buffer.readOptionalEnumConstant(ServiceEnvironmentType.class);
    }

    public enum Type {
        ALL,
        NODE,
        SERVICE,
        TASK,
        GROUP,
        ENVIRONMENT
    }

}
