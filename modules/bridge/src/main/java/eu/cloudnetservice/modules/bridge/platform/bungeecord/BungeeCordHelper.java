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

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;

public final class BungeeCordHelper {

  static final Consumer<ServiceInfoSnapshot> SERVER_REGISTER_HANDLER;
  static final Consumer<ServiceInfoSnapshot> SERVER_UNREGISTER_HANDLER;

  static {
    Consumer<ServiceInfoSnapshot> serverRegisterHandler;
    Consumer<ServiceInfoSnapshot> serverUnregisterHandler;
    // waterfall adds a dynamic api to register/unregister a server from the proxy
    // we need to use that api as the backing map is not concurrent and other plugins might cause issues because
    // of that
    // ---
    // find the best method to add a server to the proxy
    try {
      //noinspection deprecation
      var addServer = MethodHandles.publicLookup()
        .findVirtual(ProxyConfig.class, "addServer", MethodType.methodType(ServerInfo.class, ServerInfo.class));
      // the waterfall method is available
      serverRegisterHandler = service -> {
        try {
          addServer.invoke(ProxyServer.getInstance().getConfig(), constructServerInfo(service));
        } catch (Throwable throwable) {
          throw new RuntimeException("Unable to register service using Waterfall 'addServer':", throwable);
        }
      };
    } catch (NoSuchMethodException | IllegalAccessException ex) {
      // using the default BungeeCord way
      serverRegisterHandler = service -> ProxyServer.getInstance().getServers().put(
        service.name(),
        constructServerInfo(service));
    }
    // find the best method to remove a server from the proxy
    try {
      //noinspection deprecation
      var removeServerNamed = MethodHandles.publicLookup()
        .findVirtual(ProxyConfig.class, "removeServerNamed", MethodType.methodType(ServerInfo.class, String.class));
      // the waterfall method is available
      serverUnregisterHandler = service -> {
        try {
          removeServerNamed.invoke(ProxyServer.getInstance().getConfig(), service.name());
        } catch (Throwable throwable) {
          throw new RuntimeException("Unable to unregister service using Waterfall 'removeServerNamed':", throwable);
        }
      };
    } catch (NoSuchMethodException | IllegalAccessException ex) {
      // using the default BungeeCord way
      serverUnregisterHandler = service -> ProxyServer.getInstance().getServers().remove(service.name());
    }
    // assign the static fields to the best available method
    SERVER_REGISTER_HANDLER = serverRegisterHandler;
    SERVER_UNREGISTER_HANDLER = serverUnregisterHandler;
  }

  private BungeeCordHelper() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull BaseComponent[] translateToComponent(@NonNull String input) {
    return TextComponent.fromLegacyText(AdventureSerializerUtil.serializeToString(input));
  }

  private static @NonNull ServerInfo constructServerInfo(@NonNull ServiceInfoSnapshot snapshot) {
    return ProxyServer.getInstance().constructServerInfo(
      snapshot.name(),
      new InetSocketAddress(snapshot.address().host(), snapshot.address().port()),
      "Just another CloudNet provided service info",
      false);
  }
}
