package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@ToString
@EqualsAndHashCode
public final class ProcessConfiguration implements SerializableObject {

    protected ServiceEnvironmentType environment;

    protected int maxHeapMemorySize;

    protected Collection<String> jvmOptions;

    public ProcessConfiguration(ServiceEnvironmentType environment, int maxHeapMemorySize, Collection<String> jvmOptions) {
        this.environment = environment;
        this.maxHeapMemorySize = maxHeapMemorySize;
        this.jvmOptions = jvmOptions;
    }

    public ProcessConfiguration() {
    }

    public ServiceEnvironmentType getEnvironment() {
        return this.environment;
    }

    public void setEnvironment(ServiceEnvironmentType environment) {
        this.environment = environment;
    }

    public int getMaxHeapMemorySize() {
        return this.maxHeapMemorySize;
    }

    public void setMaxHeapMemorySize(int maxHeapMemorySize) {
        this.maxHeapMemorySize = maxHeapMemorySize;
    }

    public Collection<String> getJvmOptions() {
        return this.jvmOptions;
    }

    public void setJvmOptions(Collection<String> jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeEnumConstant(this.environment);
        buffer.writeInt(this.maxHeapMemorySize);
        buffer.writeStringCollection(this.jvmOptions);
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.environment = buffer.readEnumConstant(ServiceEnvironmentType.class);
        this.maxHeapMemorySize = buffer.readInt();
        this.jvmOptions = buffer.readStringCollection();
    }
}