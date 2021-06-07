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

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import java.lang.reflect.Type;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CloudOfflinePlayer extends SerializableJsonDocPropertyable implements ICloudOfflinePlayer {

  public static final Type TYPE = new TypeToken<CloudOfflinePlayer>() {
  }.getType();

  protected UUID uniqueId;

  protected String name;
  protected String xBoxId;

  protected long firstLoginTimeMillis;
  protected long lastLoginTimeMillis;

  protected NetworkConnectionInfo lastNetworkConnectionInfo;

  public CloudOfflinePlayer(UUID uniqueId, String name, String xBoxId, long firstLoginTimeMillis,
    long lastLoginTimeMillis, NetworkConnectionInfo lastNetworkConnectionInfo) {
    this.uniqueId = uniqueId;
    this.name = name;
    this.xBoxId = xBoxId;
    this.firstLoginTimeMillis = firstLoginTimeMillis;
    this.lastLoginTimeMillis = lastLoginTimeMillis;
    this.lastNetworkConnectionInfo = lastNetworkConnectionInfo;
  }

  public CloudOfflinePlayer() {
  }


  public static CloudOfflinePlayer of(ICloudPlayer cloudPlayer) {
    Preconditions.checkNotNull(cloudPlayer);

    CloudOfflinePlayer cloudOfflinePlayer = new CloudOfflinePlayer(
      cloudPlayer.getUniqueId(),
      cloudPlayer.getName(),
      cloudPlayer.getXBoxId(),
      cloudPlayer.getFirstLoginTimeMillis(),
      cloudPlayer.getLastLoginTimeMillis(),
      cloudPlayer.getLastNetworkConnectionInfo()
    );

    cloudOfflinePlayer.setProperties(cloudPlayer.getProperties());

    return cloudOfflinePlayer;
  }

  @Override
  public void setProperties(@NotNull JsonDocument properties) {
    this.properties = properties;
  }

  @NotNull
  public UUID getUniqueId() {
    return this.uniqueId;
  }

  public void setUniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getName() {
    return this.name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public String getXBoxId() {
    return this.xBoxId;
  }

  public void setXBoxId(String xBoxId) {
    this.xBoxId = xBoxId;
  }

  public long getFirstLoginTimeMillis() {
    return this.firstLoginTimeMillis;
  }

  public void setFirstLoginTimeMillis(long firstLoginTimeMillis) {
    this.firstLoginTimeMillis = firstLoginTimeMillis;
  }

  public long getLastLoginTimeMillis() {
    return this.lastLoginTimeMillis;
  }

  public void setLastLoginTimeMillis(long lastLoginTimeMillis) {
    this.lastLoginTimeMillis = lastLoginTimeMillis;
  }

  public NetworkConnectionInfo getLastNetworkConnectionInfo() {
    return this.lastNetworkConnectionInfo;
  }

  public void setLastNetworkConnectionInfo(@NotNull NetworkConnectionInfo lastNetworkConnectionInfo) {
    this.lastNetworkConnectionInfo = lastNetworkConnectionInfo;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeUUID(this.uniqueId);
    buffer.writeString(this.name);
    buffer.writeOptionalString(this.xBoxId);
    buffer.writeLong(this.firstLoginTimeMillis);
    buffer.writeLong(this.lastLoginTimeMillis);
    buffer.writeObject(this.lastNetworkConnectionInfo);

    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.uniqueId = buffer.readUUID();
    this.name = buffer.readString();
    this.xBoxId = buffer.readOptionalString();
    this.firstLoginTimeMillis = buffer.readLong();
    this.lastLoginTimeMillis = buffer.readLong();
    this.lastNetworkConnectionInfo = buffer.readObject(NetworkConnectionInfo.class);

    super.read(buffer);
  }

}
