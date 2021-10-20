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

package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

/**
 * Use the driver event without the "GoMint" - prefix instead with the {@link de.dytanic.cloudnet.driver.event.EventListener}
 * annotation and register it using {@link de.dytanic.cloudnet.driver.event.IEventManager#registerListener(Object)}
 */
@Deprecated
@ScheduledForRemoval(inVersion = "3.6")
public final class GoMintBridgeProxyPlayerLoginSuccessEvent extends GoMintBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public GoMintBridgeProxyPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
