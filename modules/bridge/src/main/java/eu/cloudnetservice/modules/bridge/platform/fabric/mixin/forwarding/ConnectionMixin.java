/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

@Environment(EnvType.SERVER)
@Mixin(Connection.class)
public final class ConnectionMixin implements BridgedClientConnection {

  @Shadow
  private SocketAddress address;

  private UUID forwardedUniqueId;
  private Property[] forwardedProperties;

  @Override
  public void addr(@NonNull SocketAddress address) {
    this.address = address;
  }

  @Override
  public @NonNull UUID forwardedUniqueId() {
    return this.forwardedUniqueId;
  }

  @Override
  public void forwardedUniqueId(@NonNull UUID uuid) {
    this.forwardedUniqueId = uuid;
  }

  @Override
  public @NonNull Property[] forwardedProfile() {
    return this.forwardedProperties;
  }

  @Override
  public void forwardedProfile(@NonNull Property[] profile) {
    this.forwardedProperties = profile;
  }
}
