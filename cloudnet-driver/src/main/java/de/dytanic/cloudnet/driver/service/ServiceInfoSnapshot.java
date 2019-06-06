package de.dytanic.cloudnet.driver.service;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.network.HostAndPort;

import java.lang.reflect.Type;

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

    public ServiceInfoSnapshot(long creationTime, ServiceId serviceId, HostAndPort address, boolean connected, ServiceLifeCycle lifeCycle, ProcessSnapshot processSnapshot, ServiceConfiguration configuration) {
        this.creationTime = creationTime;
        this.serviceId = serviceId;
        this.address = address;
        this.connected = connected;
        this.lifeCycle = lifeCycle;
        this.processSnapshot = processSnapshot;
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

    public ServiceLifeCycle getLifeCycle() {
        return this.lifeCycle;
    }

    public ProcessSnapshot getProcessSnapshot() {
        return this.processSnapshot;
    }

    public ServiceConfiguration getConfiguration() {
        return this.configuration;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ServiceInfoSnapshot)) return false;
        final ServiceInfoSnapshot other = (ServiceInfoSnapshot) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getCreationTime() != other.getCreationTime()) return false;
        final Object this$serviceId = this.getServiceId();
        final Object other$serviceId = other.getServiceId();
        if (this$serviceId == null ? other$serviceId != null : !this$serviceId.equals(other$serviceId)) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        if (this.isConnected() != other.isConnected()) return false;
        final Object this$lifeCycle = this.getLifeCycle();
        final Object other$lifeCycle = other.getLifeCycle();
        if (this$lifeCycle == null ? other$lifeCycle != null : !this$lifeCycle.equals(other$lifeCycle)) return false;
        final Object this$processSnapshot = this.getProcessSnapshot();
        final Object other$processSnapshot = other.getProcessSnapshot();
        if (this$processSnapshot == null ? other$processSnapshot != null : !this$processSnapshot.equals(other$processSnapshot))
            return false;
        final Object this$configuration = this.getConfiguration();
        final Object other$configuration = other.getConfiguration();
        if (this$configuration == null ? other$configuration != null : !this$configuration.equals(other$configuration))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ServiceInfoSnapshot;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $creationTime = this.getCreationTime();
        result = result * PRIME + (int) ($creationTime >>> 32 ^ $creationTime);
        final Object $serviceId = this.getServiceId();
        result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        result = result * PRIME + (this.isConnected() ? 79 : 97);
        final Object $lifeCycle = this.getLifeCycle();
        result = result * PRIME + ($lifeCycle == null ? 43 : $lifeCycle.hashCode());
        final Object $processSnapshot = this.getProcessSnapshot();
        result = result * PRIME + ($processSnapshot == null ? 43 : $processSnapshot.hashCode());
        final Object $configuration = this.getConfiguration();
        result = result * PRIME + ($configuration == null ? 43 : $configuration.hashCode());
        return result;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setLifeCycle(ServiceLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public void setProcessSnapshot(ProcessSnapshot processSnapshot) {
        this.processSnapshot = processSnapshot;
    }
}