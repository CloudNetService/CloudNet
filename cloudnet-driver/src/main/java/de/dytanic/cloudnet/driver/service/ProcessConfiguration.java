package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

@ToString
@EqualsAndHashCode
public final class ProcessConfiguration implements SerializableObject {

    protected ServiceEnvironmentType environment;

    protected int maxHeapMemorySize = -1;

    protected Collection<String> jvmOptions;
    protected Collection<String> processParameters = new ArrayList<>();


    public ProcessConfiguration(ServiceEnvironmentType environment, int maxHeapMemorySize, Collection<String> jvmOptions) {
        this(environment, maxHeapMemorySize, jvmOptions, new ArrayList<>());
    }

    public ProcessConfiguration(ServiceEnvironmentType environment, int maxHeapMemorySize, Collection<String> jvmOptions, Collection<String> processParameters) {
        this.environment = environment;
        this.maxHeapMemorySize = maxHeapMemorySize;
        this.jvmOptions = jvmOptions;

        if (processParameters != null) {
            this.processParameters = processParameters;
        }
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

    public Collection<String> getProcessParameters() {
        return this.processParameters;
    }

    public void setJvmOptions(Collection<String> jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public void setProcessParameters(Collection<String> processParameters) {
        this.processParameters = processParameters;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeEnumConstant(this.environment);
        buffer.writeInt(this.maxHeapMemorySize);
        buffer.writeStringCollection(this.jvmOptions);
        buffer.writeStringCollection(this.processParameters);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.environment = buffer.readEnumConstant(ServiceEnvironmentType.class);
        this.maxHeapMemorySize = buffer.readInt();
        this.jvmOptions = buffer.readStringCollection();
        this.processParameters = buffer.readStringCollection();
    }
}