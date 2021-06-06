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

package de.dytanic.cloudnet.network;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.network.packet.PacketServerSetGlobalServiceInfoList;

public class ClusterUtils {

  private ClusterUtils() {
    throw new UnsupportedOperationException();
  }

  public static void sendSetupInformationPackets(INetworkChannel channel) {
    sendSetupInformationPackets(channel, false);
  }

  public static void sendSetupInformationPackets(INetworkChannel channel, boolean secondNodeConnection) {
    channel.sendPacket(new PacketServerSetGlobalServiceInfoList(
      CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots().values()));
    if (!secondNodeConnection) {
      CloudNet.getInstance().publishH2DatabaseDataToCluster(channel);
    }
  }

}
