package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.HostAndPort;

import java.lang.reflect.Type;
import java.util.UUID;

public class NetworkConnectionInfo {

    public static final Type TYPE = new TypeToken<NetworkConnectionInfo>() {
    }.getType();

    protected UUID uniqueId;

    protected String name;

    protected int version;

    protected HostAndPort address, listener;

    protected boolean onlineMode, legacy;

    protected NetworkServiceInfo networkService;

    public NetworkConnectionInfo(UUID uniqueId, String name, int version, HostAndPort address, HostAndPort listener, boolean onlineMode, boolean legacy, NetworkServiceInfo networkService) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.version = version;
        this.address = address;
        this.listener = listener;
        this.onlineMode = onlineMode;
        this.legacy = legacy;
        this.networkService = networkService;
    }

    public NetworkConnectionInfo() {
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public int getVersion() {
        return this.version;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public HostAndPort getListener() {
        return this.listener;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public boolean isLegacy() {
        return this.legacy;
    }

    public NetworkServiceInfo getNetworkService() {
        return this.networkService;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public void setListener(HostAndPort listener) {
        this.listener = listener;
    }

    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public void setNetworkService(NetworkServiceInfo networkService) {
        this.networkService = networkService;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkConnectionInfo)) return false;
        final NetworkConnectionInfo other = (NetworkConnectionInfo) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        if (this.getVersion() != other.getVersion()) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final Object this$listener = this.getListener();
        final Object other$listener = other.getListener();
        if (this$listener == null ? other$listener != null : !this$listener.equals(other$listener)) return false;
        if (this.isOnlineMode() != other.isOnlineMode()) return false;
        if (this.isLegacy() != other.isLegacy()) return false;
        final Object this$networkService = this.getNetworkService();
        final Object other$networkService = other.getNetworkService();
        if (this$networkService == null ? other$networkService != null : !this$networkService.equals(other$networkService))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkConnectionInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        result = result * PRIME + this.getVersion();
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final Object $listener = this.getListener();
        result = result * PRIME + ($listener == null ? 43 : $listener.hashCode());
        result = result * PRIME + (this.isOnlineMode() ? 79 : 97);
        result = result * PRIME + (this.isLegacy() ? 79 : 97);
        final Object $networkService = this.getNetworkService();
        result = result * PRIME + ($networkService == null ? 43 : $networkService.hashCode());
        return result;
    }

    public String toString() {
        return "NetworkConnectionInfo(uniqueId=" + this.getUniqueId() + ", name=" + this.getName() + ", version=" + this.getVersion() + ", address=" + this.getAddress() + ", listener=" + this.getListener() + ", onlineMode=" + this.isOnlineMode() + ", legacy=" + this.isLegacy() + ", networkService=" + this.getNetworkService() + ")";
    }
}