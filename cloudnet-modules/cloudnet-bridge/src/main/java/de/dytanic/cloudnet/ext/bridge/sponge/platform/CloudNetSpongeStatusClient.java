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

package de.dytanic.cloudnet.ext.bridge.sponge.platform;

import java.net.InetSocketAddress;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.status.StatusClient;

public class CloudNetSpongeStatusClient implements StatusClient {

  public static final StatusClient INSTANCE = new CloudNetSpongeStatusClient();

  private final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 53345);
  private final MinecraftVersion minecraftVersion = Sponge.getPlatform().getMinecraftVersion();

  @Override
  public @NotNull InetSocketAddress getAddress() {
    return this.address;
  }

  @Override
  public @NotNull MinecraftVersion getVersion() {
    return this.minecraftVersion;
  }

  @Override
  public @NotNull Optional<InetSocketAddress> getVirtualHost() {
    return Optional.empty();
  }
}
