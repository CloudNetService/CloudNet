package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;

import java.lang.reflect.Type;
import java.util.UUID;

public final class NetworkPlayerServerInfo {

    public static final Type TYPE = new TypeToken<NetworkPlayerServerInfo>() {
    }.getType();

    protected UUID uniqueId;

    protected String name, xBoxId;

    protected double health, maxHealth, saturation;

    protected int level;

    protected WorldPosition location;

    protected HostAndPort address;

    protected NetworkServiceInfo networkService;

    public NetworkPlayerServerInfo(UUID uniqueId, String name, String xBoxId, double health, double maxHealth, double saturation, int level, WorldPosition location, HostAndPort address, NetworkServiceInfo networkService) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.xBoxId = xBoxId;
        this.health = health;
        this.maxHealth = maxHealth;
        this.saturation = saturation;
        this.level = level;
        this.location = location;
        this.address = address;
        this.networkService = networkService;
    }

    public NetworkPlayerServerInfo() {
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public String getXBoxId() {
        return this.xBoxId;
    }

    public double getHealth() {
        return this.health;
    }

    public double getMaxHealth() {
        return this.maxHealth;
    }

    public double getSaturation() {
        return this.saturation;
    }

    public int getLevel() {
        return this.level;
    }

    public WorldPosition getLocation() {
        return this.location;
    }

    public HostAndPort getAddress() {
        return this.address;
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

    public void setXBoxId(String xBoxId) {
        this.xBoxId = xBoxId;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setLocation(WorldPosition location) {
        this.location = location;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public void setNetworkService(NetworkServiceInfo networkService) {
        this.networkService = networkService;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkPlayerServerInfo)) return false;
        final NetworkPlayerServerInfo other = (NetworkPlayerServerInfo) o;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$xBoxId = this.getXBoxId();
        final Object other$xBoxId = other.getXBoxId();
        if (this$xBoxId == null ? other$xBoxId != null : !this$xBoxId.equals(other$xBoxId)) return false;
        if (Double.compare(this.getHealth(), other.getHealth()) != 0) return false;
        if (Double.compare(this.getMaxHealth(), other.getMaxHealth()) != 0) return false;
        if (Double.compare(this.getSaturation(), other.getSaturation()) != 0) return false;
        if (this.getLevel() != other.getLevel()) return false;
        final Object this$location = this.getLocation();
        final Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final Object this$networkService = this.getNetworkService();
        final Object other$networkService = other.getNetworkService();
        if (this$networkService == null ? other$networkService != null : !this$networkService.equals(other$networkService))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $xBoxId = this.getXBoxId();
        result = result * PRIME + ($xBoxId == null ? 43 : $xBoxId.hashCode());
        final long $health = Double.doubleToLongBits(this.getHealth());
        result = result * PRIME + (int) ($health >>> 32 ^ $health);
        final long $maxHealth = Double.doubleToLongBits(this.getMaxHealth());
        result = result * PRIME + (int) ($maxHealth >>> 32 ^ $maxHealth);
        final long $saturation = Double.doubleToLongBits(this.getSaturation());
        result = result * PRIME + (int) ($saturation >>> 32 ^ $saturation);
        result = result * PRIME + this.getLevel();
        final Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final Object $networkService = this.getNetworkService();
        result = result * PRIME + ($networkService == null ? 43 : $networkService.hashCode());
        return result;
    }

    public String toString() {
        return "NetworkPlayerServerInfo(uniqueId=" + this.getUniqueId() + ", name=" + this.getName() + ", xBoxId=" + this.getXBoxId() + ", health=" + this.getHealth() + ", maxHealth=" + this.getMaxHealth() + ", saturation=" + this.getSaturation() + ", level=" + this.getLevel() + ", location=" + this.getLocation() + ", address=" + this.getAddress() + ", networkService=" + this.getNetworkService() + ")";
    }
}