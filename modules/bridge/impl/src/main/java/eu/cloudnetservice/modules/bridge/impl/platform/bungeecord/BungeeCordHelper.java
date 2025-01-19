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

package eu.cloudnetservice.modules.bridge.impl.platform.bungeecord;

import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

@Singleton
public final class BungeeCordHelper {

  private final ProxyServer proxyServer;
  private final Consumer<ServiceInfoSnapshot> serverRegisterHandler;
  private final Consumer<ServiceInfoSnapshot> serverUnregisterHandler;

  @Inject
  public BungeeCordHelper(
    @NonNull ProxyServer proxyServer,
    @NonNull ProxyConfig proxyConfig
  ) {
    this.proxyServer = proxyServer;

    // waterfall adds a dynamic api to register a server from the proxy.
    // we need to use that api as the backing map is not concurrent and other plugins might cause issues
    this.serverRegisterHandler = Reflexion.onBound(proxyConfig)
      .findMethod("addServer", ServerInfo.class)
      .map(acc -> (Consumer<ServiceInfoSnapshot>) service -> acc.invokeWithArgs(this.constructServerInfo(service)))
      .orElse(service -> {
        var serverInfo = this.constructServerInfo(service);
        proxyServer.getServers().put(service.name(), serverInfo);
      });

    // waterfall adds a dynamic api to unregister a server from the proxy.
    // we need to use that api as the backing map is not concurrent and other plugins might cause issues
    this.serverUnregisterHandler = Reflexion.onBound(proxyConfig)
      .findMethod("removeServerNamed", String.class)
      .map(acc -> (Consumer<ServiceInfoSnapshot>) service -> acc.invokeWithArgs(service.name()))
      .orElse(service -> proxyServer.getServers().remove(service.name()));
  }

  @NonNull Consumer<ServiceInfoSnapshot> serverRegisterHandler() {
    return this.serverRegisterHandler;
  }

  @NonNull Consumer<ServiceInfoSnapshot> serverUnregisterHandler() {
    return this.serverUnregisterHandler;
  }

  private @NonNull ServerInfo constructServerInfo(@NonNull ServiceInfoSnapshot snapshot) {
    return this.proxyServer.constructServerInfo(
      snapshot.name(),
      new InetSocketAddress(snapshot.address().host(), snapshot.address().port()),
      "Just another CloudNet provided service info",
      false);
  }
}
