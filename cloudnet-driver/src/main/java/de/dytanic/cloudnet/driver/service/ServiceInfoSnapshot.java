package de.dytanic.cloudnet.driver.service;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;

@ToString
@EqualsAndHashCode
public class ServiceInfoSnapshot extends BasicJsonDocPropertyable {

    public static final Type TYPE = new TypeToken<ServiceInfoSnapshot>() {
    }.getType();

    protected long creationTime;

    protected ServiceId serviceId;

    protected HostAndPort address;

    protected boolean connected;

    protected ServiceLifeCycle lifeCycle;

    protected ProcessSnapshot processSnapshot;

    protected ServiceConfiguration configuration;

    public ServiceInfoSnapshot(long creationTime, ServiceId serviceId, HostAndPort address, boolean connected, ServiceLifeCycle lifeCycle, ProcessSnapshot processSnapshot, JsonDocument properties, ServiceConfiguration configuration) {
        this.creationTime = creationTime;
        this.serviceId = serviceId;
        this.address = address;
        this.connected = connected;
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
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
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
}