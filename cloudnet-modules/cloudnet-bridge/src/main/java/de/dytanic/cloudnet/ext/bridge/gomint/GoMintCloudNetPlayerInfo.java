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

    protected double health;
    protected double maxHealth;
    protected double saturation;
    protected int level;
    protected int ping;
    protected Locale locale;
    protected WorldPosition location;
    protected HostAndPort address;
    private UUID uniqueId;
    private boolean online;
    private String name;
    private String deviceName;
    private String xBoxId;
    private String gamemode;

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

    public void setHealth(double health) {
        this.health = health;
    }

    public double getMaxHealth() {
        return this.maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public double getSaturation() {
        return this.saturation;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPing() {
        return this.ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public WorldPosition getLocation() {
        return this.location;
    }

    public void setLocation(WorldPosition location) {
        this.location = location;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public boolean isOnline() {
        return this.online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getXBoxId() {
        return this.xBoxId;
    }

    public void setXBoxId(String xBoxId) {
        this.xBoxId = xBoxId;
    }

    public String getGamemode() {
        return this.gamemode;
    }

    public void setGamemode(String gamemode) {
        this.gamemode = gamemode;
    }

}