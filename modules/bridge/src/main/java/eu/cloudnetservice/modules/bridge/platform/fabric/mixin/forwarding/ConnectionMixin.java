/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform.fabric.mixin.forwarding;

import com.mojang.authlib.properties.Property;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedClientConnection;
import java.net.SocketAddress;
import java.util.UUID;
import lombok.NonNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
@Environment(EnvType.SERVER)
public abstract class ConnectionMixin implements BridgedClientConnection {

  @Shadow
  private SocketAddress address;

  @Unique
  private UUID cloudnet_bridge$forwardedUniqueId;
  @Unique
  private Property[] cloudnet_bridge$forwardedProperties;

  @Unique
  private boolean cloudnet_bridge$intentionPacketSeen;

  @Override
  public void cloudnet_bridge$addr(@NonNull SocketAddress address) {
    this.address = address;
  }

  @Override
  public @NonNull UUID cloudnet_bridge$forwardedUniqueId() {
    return this.cloudnet_bridge$forwardedUniqueId;
  }

  @Override
  public void cloudnet_bridge$forwardedUniqueId(@NonNull UUID uuid) {
    this.cloudnet_bridge$forwardedUniqueId = uuid;
  }

  @Override
  public @NonNull Property[] cloudnet_bridge$forwardedProfile() {
    return this.cloudnet_bridge$forwardedProperties;
  }

  @Override
  public void cloudnet_bridge$forwardedProfile(@NonNull Property[] profile) {
    this.cloudnet_bridge$forwardedProperties = profile;
  }

  @Override
  public boolean cloudnet_bridge$intentionPacketSeen() {
    return this.cloudnet_bridge$intentionPacketSeen;
  }

  @Override
  public void cloudnet_bridge$markIntentionPacketSeen() {
    this.cloudnet_bridge$intentionPacketSeen = true;
  }
}
