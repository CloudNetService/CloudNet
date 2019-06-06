package de.dytanic.cloudnet.ext.bridge.gomint;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;

import java.util.Locale;
import java.util.UUID;

final class GoMintCloudNetPlayerInfo {

    protected double health, maxHealth, saturation;
    protected int level, ping;
    protected Locale locale;
    protected WorldPosition location;
    protected HostAndPort address;
    private UUID uniqueId;
    private boolean online;
    private String name, deviceName, xBoxId, gamemode;

    public GoMintCloudNetPlayerInfo(double health, double maxHealth, double saturation, int level, int ping, Locale locale, WorldPosition location, HostAndPort address, UUID uniqueId, boolean online, String name, String deviceName, String xBoxId, String gamemode) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.saturation = saturation;
        this.level = level;
        this.ping = ping;
        this.locale = locale;
        this.location = location;
        this.address = address;
        this.uniqueId = uniqueId;
        this.online = online;
        this.name = name;
        this.deviceName = deviceName;
        this.xBoxId = xBoxId;
        this.gamemode = gamemode;
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

    public int getPing() {
        return this.ping;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public WorldPosition getLocation() {
        return this.location;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public boolean isOnline() {
        return this.online;
    }

    public String getName() {
        return this.name;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public String getXBoxId() {
        return this.xBoxId;
    }

    public String getGamemode() {
        return this.gamemode;
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

    public void setPing(int ping) {
        this.ping = ping;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setLocation(WorldPosition location) {
        this.location = location;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setXBoxId(String xBoxId) {
        this.xBoxId = xBoxId;
    }

    public void setGamemode(String gamemode) {
        this.gamemode = gamemode;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GoMintCloudNetPlayerInfo)) return false;
        final GoMintCloudNetPlayerInfo other = (GoMintCloudNetPlayerInfo) o;
        if (Double.compare(this.getHealth(), other.getHealth()) != 0) return false;
        if (Double.compare(this.getMaxHealth(), other.getMaxHealth()) != 0) return false;
        if (Double.compare(this.getSaturation(), other.getSaturation()) != 0) return false;
        if (this.getLevel() != other.getLevel()) return false;
        if (this.getPing() != other.getPing()) return false;
        final Object this$locale = this.getLocale();
        final Object other$locale = other.getLocale();
        if (this$locale == null ? other$locale != null : !this$locale.equals(other$locale)) return false;
        final Object this$location = this.getLocation();
        final Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        if (this.isOnline() != other.isOnline()) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$deviceName = this.getDeviceName();
        final Object other$deviceName = other.getDeviceName();
        if (this$deviceName == null ? other$deviceName != null : !this$deviceName.equals(other$deviceName))
            return false;
        final Object this$xBoxId = this.getXBoxId();
        final Object other$xBoxId = other.getXBoxId();
        if (this$xBoxId == null ? other$xBoxId != null : !this$xBoxId.equals(other$xBoxId)) return false;
        final Object this$gamemode = this.getGamemode();
        final Object other$gamemode = other.getGamemode();
        if (this$gamemode == null ? other$gamemode != null : !this$gamemode.equals(other$gamemode)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $health = Double.doubleToLongBits(this.getHealth());
        result = result * PRIME + (int) ($health >>> 32 ^ $health);
        final long $maxHealth = Double.doubleToLongBits(this.getMaxHealth());
        result = result * PRIME + (int) ($maxHealth >>> 32 ^ $maxHealth);
        final long $saturation = Double.doubleToLongBits(this.getSaturation());
        result = result * PRIME + (int) ($saturation >>> 32 ^ $saturation);
        result = result * PRIME + this.getLevel();
        result = result * PRIME + this.getPing();
        final Object $locale = this.getLocale();
        result = result * PRIME + ($locale == null ? 43 : $locale.hashCode());
        final Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        result = result * PRIME + (this.isOnline() ? 79 : 97);
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $deviceName = this.getDeviceName();
        result = result * PRIME + ($deviceName == null ? 43 : $deviceName.hashCode());
        final Object $xBoxId = this.getXBoxId();
        result = result * PRIME + ($xBoxId == null ? 43 : $xBoxId.hashCode());
        final Object $gamemode = this.getGamemode();
        result = result * PRIME + ($gamemode == null ? 43 : $gamemode.hashCode());
        return result;
    }

    public String toString() {
        return "GoMintCloudNetPlayerInfo(health=" + this.getHealth() + ", maxHealth=" + this.getMaxHealth() + ", saturation=" + this.getSaturation() + ", level=" + this.getLevel() + ", ping=" + this.getPing() + ", locale=" + this.getLocale() + ", location=" + this.getLocation() + ", address=" + this.getAddress() + ", uniqueId=" + this.getUniqueId() + ", online=" + this.isOnline() + ", name=" + this.getName() + ", deviceName=" + this.getDeviceName() + ", xBoxId=" + this.getXBoxId() + ", gamemode=" + this.getGamemode() + ")";
    }
}