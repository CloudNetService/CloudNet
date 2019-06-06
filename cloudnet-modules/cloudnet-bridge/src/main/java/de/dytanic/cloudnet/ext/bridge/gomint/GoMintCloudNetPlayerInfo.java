package de.dytanic.cloudnet.ext.bridge.gomint;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Locale;
import java.util.UUID;

@ToString
@EqualsAndHashCode
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

}