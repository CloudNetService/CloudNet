package de.dytanic.cloudnet.driver.service;

import com.google.common.collect.ComparisonChain;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import de.dytanic.cloudnet.driver.service.property.ServiceProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Optional;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceInfoSnapshot extends SerializableJsonDocPropertyable implements INameable, Comparable<ServiceInfoSnapshot>, SerializableObject {

    public static final Type TYPE = new TypeToken<ServiceInfoSnapshot>() {
    }.getType();

    protected long creationTime;

    protected HostAndPort address;

    protected long connectedTime;

    protected ServiceLifeCycle lifeCycle;

    protected ProcessSnapshot processSnapshot;

    protected ServiceConfiguration configuration;

    public ServiceInfoSnapshot(long creationTime, HostAndPort address, long connectedTime, ServiceLifeCycle lifeCycle, ProcessSnapshot processSnapshot, ServiceConfiguration configuration) {
        this(creationTime, address, connectedTime, lifeCycle, processSnapshot, JsonDocument.newDocument(), configuration);
    }

    public ServiceInfoSnapshot(long creationTime, HostAndPort address, long connectedTime, ServiceLifeCycle lifeCycle, ProcessSnapshot processSnapshot, JsonDocument properties, ServiceConfiguration configuration) {
        this.creationTime = creationTime;
        this.address = address;
        this.connectedTime = connectedTime;
        this.lifeCycle = lifeCycle;
        this.processSnapshot = processSnapshot;
        this.properties = properties;
        this.configuration = configuration;
    }

    public ServiceInfoSnapshot() {
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public ServiceId getServiceId() {
        return this.configuration.getServiceId();
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public boolean isConnected() {
        return this.connectedTime != -1;
    }

    public long getConnectedTime() {
        return this.connectedTime;
    }

    public void setConnectedTime(long connectedTime) {
        this.connectedTime = connectedTime;
    }

    public ServiceLifeCycle getLifeCycle() {
        return this.lifeCycle;
    }

    public void setLifeCycle(ServiceLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public ProcessSnapshot getProcessSnapshot() {
        return this.processSnapshot;
    }

    public void setProcessSnapshot(ProcessSnapshot processSnapshot) {
        this.processSnapshot = processSnapshot;
    }

    public ServiceConfiguration getConfiguration() {
        return this.configuration;
    }

    @NotNull
    public SpecificCloudServiceProvider provider() {
        return CloudNetDriver.getInstance().getCloudServiceProvider(this);
    }

    @NotNull
    public <T> Optional<T> getProperty(@NotNull ServiceProperty<T> property) {
        return property.get(this);
    }

    public <T> void setProperty(@NotNull ServiceProperty<T> property, @Nullable T value) {
        property.set(this, value);
    }

    @Override
    public String getName() {
        return this.getServiceId().getName();
    }

    @Override
    public int compareTo(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return ComparisonChain.start()
                .compare(this.getServiceId().getTaskName(), serviceInfoSnapshot.getServiceId().getTaskName())
                .compare(this.getServiceId().getTaskServiceId(), serviceInfoSnapshot.getServiceId().getTaskServiceId())
                .result();
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeLong(this.creationTime);
        buffer.writeObject(this.address);
        buffer.writeLong(this.connectedTime);
        buffer.writeEnumConstant(this.lifeCycle);
        buffer.writeObject(this.processSnapshot);
        buffer.writeObject(this.configuration);

        super.write(buffer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.creationTime = buffer.readLong();
        this.address = buffer.readObject(HostAndPort.class);
        this.connectedTime = buffer.readLong();
        this.lifeCycle = buffer.readEnumConstant(ServiceLifeCycle.class);
        this.processSnapshot = buffer.readObject(ProcessSnapshot.class);
        this.configuration = buffer.readObject(ServiceConfiguration.class);

        super.read(buffer);
    }
}