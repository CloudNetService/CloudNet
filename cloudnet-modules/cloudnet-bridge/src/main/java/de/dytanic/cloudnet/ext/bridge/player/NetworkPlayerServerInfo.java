/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import java.lang.reflect.Type;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public final class NetworkPlayerServerInfo implements SerializableObject {

  public static final Type TYPE = new TypeToken<NetworkPlayerServerInfo>() {
  }.getType();

  protected UUID uniqueId;

  protected String name;
  protected String xBoxId;

  protected double health;
  protected double maxHealth;
  protected double saturation;

  protected int level;

  protected WorldPosition location;

  protected HostAndPort address;

  protected NetworkServiceInfo networkService;

  public NetworkPlayerServerInfo(UUID uniqueId, String name, String xBoxId, double health, double maxHealth,
    double saturation, int level, WorldPosition location, HostAndPort address, NetworkServiceInfo networkService) {
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

  public void setUniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getXBoxId() {
    return this.xBoxId;
  }

  public void setXBoxId(String xBoxId) {
    this.xBoxId = xBoxId;
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

  public NetworkServiceInfo getNetworkService() {
    return this.networkService;
  }

  public void setNetworkService(NetworkServiceInfo networkService) {
    this.networkService = networkService;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeUUID(this.uniqueId);
    buffer.writeString(this.name);
    buffer.writeOptionalString(this.xBoxId);
    buffer.writeDouble(this.health);
    buffer.writeDouble(this.maxHealth);
    buffer.writeDouble(this.saturation);
    buffer.writeInt(this.level);
    buffer.writeObject(this.location);
    buffer.writeObject(this.address);
    buffer.writeObject(this.networkService);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.uniqueId = buffer.readUUID();
    this.name = buffer.readString();
    this.xBoxId = buffer.readOptionalString();
    this.health = buffer.readDouble();
    this.maxHealth = buffer.readDouble();
    this.saturation = buffer.readDouble();
    this.level = buffer.readInt();
    this.location = buffer.readObject(WorldPosition.class);
    this.address = buffer.readObject(HostAndPort.class);
    this.networkService = buffer.readObject(NetworkServiceInfo.class);
  }
}
