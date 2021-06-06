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

package de.dytanic.cloudnet.ext.bridge.node.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeLocalBridgePlayerProxyLoginRequestEvent extends Event implements ICancelable {

  private final NetworkConnectionInfo connectionInfo;
  private String kickReason;
  private boolean cancelled;

  public NodeLocalBridgePlayerProxyLoginRequestEvent(@NotNull NetworkConnectionInfo connectionInfo, String kickReason) {
    this.connectionInfo = connectionInfo;
    this.kickReason = kickReason;
  }

  public NetworkConnectionInfo getConnectionInfo() {
    return this.connectionInfo;
  }

  @Nullable
  public String getKickReason() {
    return this.kickReason;
  }

  public void setKickReason(@Nullable String kickReason) {
    this.kickReason = kickReason;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
