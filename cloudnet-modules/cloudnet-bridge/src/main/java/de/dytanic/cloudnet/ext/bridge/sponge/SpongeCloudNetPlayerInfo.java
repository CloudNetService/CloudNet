package de.dytanic.cloudnet.ext.bridge.sponge;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@ToString
@EqualsAndHashCode
final class SpongeCloudNetPlayerInfo {

    private UUID uniqueId;

    private String name;

    private int ping;

    private double health, maxHealth, saturation;

    private int level;

    private WorldPosition location;

    private HostAndPort address;

    public SpongeCloudNetPlayerInfo(UUID uniqueId, String name, int ping, double health, double maxHealth, double saturation, int level, WorldPosition location, HostAndPort address) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.ping = ping;
        this.health = health;
        this.maxHealth = maxHealth;
        this.saturation = saturation;
        this.level = level;
        this.location = location;
        this.address = address;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public int getPing() {
        return this.ping;
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

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPing(int ping) {
        this.ping = ping;
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

}