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

package eu.cloudnetservice.modules.bridge.impl.platform.sponge;

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import lombok.NonNull;
import org.spongepowered.api.Platform;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.plugin.PluginManager;

@Singleton
public final class SpongeAddressAccessor {

  private static final MethodAccessor<Method> GET_CONNECT_ACCESSOR = Reflexion.on(ServerPlayer.class)
    .findMethod("connection")
    .orElse(null);

  private final int spongeApiVersion;

  @Inject
  public SpongeAddressAccessor(@NonNull PluginManager pluginManager) {
    this.spongeApiVersion = pluginManager.plugin(Platform.API_ID)
      .map(plugin -> plugin.metadata().version().getMajorVersion())
      .orElse(0);
  }

  public @NonNull InetSocketAddress playerHostAddress(@NonNull ServerPlayer serverPlayer) {
    if (this.spongeApiVersion < 11) {
      return serverPlayer.connection().address();
    }

    if (GET_CONNECT_ACCESSOR == null) {
      throw new IllegalStateException("Unable to find connection accessor for api version >= 11");
    }

    return GET_CONNECT_ACCESSOR.<RemoteConnection>invoke(serverPlayer).getOrThrow().address();
  }
}
