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

package de.dytanic.cloudnet.ext.bridge.waterdogpe.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;

public class WaterdogPEBridgeProxyPlayerServerConnectRequestEvent extends WaterdogPEBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  private final NetworkServiceInfo networkServiceInfo;

  public WaterdogPEBridgeProxyPlayerServerConnectRequestEvent(NetworkConnectionInfo networkConnectionInfo,
    NetworkServiceInfo networkServiceInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
    this.networkServiceInfo = networkServiceInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }

  public NetworkServiceInfo getNetworkServiceInfo() {
    return this.networkServiceInfo;
  }
}
