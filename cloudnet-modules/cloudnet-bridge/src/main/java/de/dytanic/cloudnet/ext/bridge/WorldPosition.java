package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class WorldPosition implements SerializableObject {

  protected double x, y, z, yaw, pitch;

  protected String world, group;

  public WorldPosition(double x, double y, double z, double yaw, double pitch, String world) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.world = world;
  }

  public WorldPosition(double x, double y, double z, double yaw, double pitch, String world, String group) {
    this(x, y, z, yaw, pitch, world);
    this.group = group;
  }

  public WorldPosition() {
  }

  public double getX() {
    return this.x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getY() {
    return this.y;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getZ() {
    return this.z;
  }

  public void setZ(double z) {
    this.z = z;
  }

  public double getYaw() {
    return this.yaw;
  }

  public void setYaw(double yaw) {
    this.yaw = yaw;
  }

  public double getPitch() {
    return this.pitch;
  }

  public void setPitch(double pitch) {
    this.pitch = pitch;
  }

  public String getWorld() {
    return this.world;
  }

  public void setWorld(String world) {
    this.world = world;
  }

  public String getGroup() {
    return this.group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeDouble(this.x);
    buffer.writeDouble(this.y);
    buffer.writeDouble(this.z);
    buffer.writeDouble(this.yaw);
    buffer.writeDouble(this.pitch);
    buffer.writeString(this.world);
    buffer.writeOptionalString(this.group);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.x = buffer.readDouble();
    this.y = buffer.readDouble();
    this.z = buffer.readDouble();
    this.yaw = buffer.readDouble();
    this.pitch = buffer.readDouble();
    this.world = buffer.readString();
    this.group = buffer.readOptionalString();
  }

}
