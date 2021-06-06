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

package de.dytanic.cloudnet.ext.bridge.bungee;

import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeCloudNetReconnectHandler implements ReconnectHandler {

  @Override
  public ServerInfo getServer(ProxiedPlayer player) {
    return BungeeCloudNetHelper.getNextFallback(player, null).orElse(null);
  }

  @Override
  public void setServer(ProxiedPlayer player) {
  }

  @Override
  public void save() {
  }

  @Override
  public void close() {
  }

}
