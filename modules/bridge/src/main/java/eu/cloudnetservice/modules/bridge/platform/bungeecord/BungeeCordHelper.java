/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform.bungeecord;

import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.component.ComponentFormats;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;

public final class BungeeCordHelper {

  static final Consumer<ServiceInfoSnapshot> SERVER_REGISTER_HANDLER;
  static final Consumer<ServiceInfoSnapshot> SERVER_UNREGISTER_HANDLER;

  static {
    var proxyInstance = ProxyServer.getInstance();
    var proxyConfig = proxyInstance.getConfig();

    // waterfall adds a dynamic api to register a server from the proxy.
    // we need to use that api as the backing map is not concurrent and other plugins might cause issues
    SERVER_REGISTER_HANDLER = Reflexion.onBound(proxyConfig)
      .findMethod("addServer", ServerInfo.class)
      .map(acc -> (Consumer<ServiceInfoSnapshot>) service -> acc.invokeWithArgs(constructServerInfo(service)))
      .orElse(service -> {
        var serverInfo = constructServerInfo(service);
        proxyInstance.getServers().put(service.name(), serverInfo);
      });

    // waterfall adds a dynamic api to unregister a server from the proxy.
    // we need to use that api as the backing map is not concurrent and other plugins might cause issues
    SERVER_UNREGISTER_HANDLER = Reflexion.onBound(proxyConfig)
      .findMethod("removeServerNamed", String.class)
      .map(acc -> (Consumer<ServiceInfoSnapshot>) service -> acc.invokeWithArgs(service.name()))
      .orElse(service -> proxyInstance.getServers().remove(service.name()));
  }

  private BungeeCordHelper() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull BaseComponent[] translateToComponent(@NonNull String input) {
    return ComponentFormats.ADVENTURE_TO_BUNGEE.convert(input);
  }

  private static @NonNull ServerInfo constructServerInfo(@NonNull ServiceInfoSnapshot snapshot) {
    return ProxyServer.getInstance().constructServerInfo(
      snapshot.name(),
      new InetSocketAddress(snapshot.address().host(), snapshot.address().port()),
      "Just another CloudNet provided service info",
      false);
  }
}
