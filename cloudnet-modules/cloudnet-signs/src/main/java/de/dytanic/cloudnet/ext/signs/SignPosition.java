package de.dytanic.cloudnet.ext.signs;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignPosition {

    protected double x, y, z, yaw, pitch;

    protected String group, world;

    public SignPosition(double x, double y, double z, double yaw, double pitch, String group, String world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.group = group;
        this.world = world;
    }

    public SignPosition() {
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getYaw() {
        return this.yaw;
    }

    public double getPitch() {
        return this.pitch;
    }

    public String getGroup() {
        return this.group;
    }

    public String getWorld() {
        return this.world;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setWorld(String world) {
        this.world = world;
    }

}