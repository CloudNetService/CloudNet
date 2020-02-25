package de.dytanic.cloudnet.driver.service;

import com.google.common.collect.ComparisonChain;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceInfoSnapshot extends BasicJsonDocPropertyable implements INameable, Comparable<ServiceInfoSnapshot> {

    public static final Type TYPE = new TypeToken<ServiceInfoSnapshot>() {
    }.getType();

    protected long creationTime;

    protected ServiceId serviceId;

    protected HostAndPort address;

    protected long connectedTime;

    protected ServiceLifeCycle lifeCycle;

    protected ProcessSnapshot processSnapshot;

    protected ServiceConfiguration configuration;

    public ServiceInfoSnapshot(long creationTime, ServiceId serviceId, HostAndPort address, long connectedTime, ServiceLifeCycle lifeCycle, ProcessSnapshot processSnapshot, ServiceConfiguration configuration) {
        this(creationTime, serviceId, address, connectedTime, lifeCycle, processSnapshot, JsonDocument.newDocument(), configuration);
    }

    public ServiceInfoSnapshot(long creationTime, ServiceId serviceId, HostAndPort address, long connectedTime, ServiceLifeCycle lifeCycle, ProcessSnapshot processSnapshot, JsonDocument properties, ServiceConfiguration configuration) {
        this.creationTime = creationTime;
        this.serviceId = serviceId;
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
        return this.serviceId;
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

    public SpecificCloudServiceProvider provider() {
        return CloudNetDriver.getInstance().getCloudServiceProvider(this);
    }

    @Override
    public String getName() {
        return this.serviceId.getName();
    }

    @Override
    public int compareTo(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return ComparisonChain.start()
                .compare(this.getServiceId().getTaskName(), serviceInfoSnapshot.getServiceId().getTaskName())
                .compare(this.getServiceId().getTaskServiceId(), serviceInfoSnapshot.getServiceId().getTaskServiceId())
                .result();
    }
}