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

package de.dytanic.cloudnet.network.listener.driver;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.HashMap;
import java.util.Map;

public class PacketServerDriverAPIListener implements IPacketListener {

  private final Map<DriverAPICategory, CategorizedDriverAPIListener> listeners = new HashMap<>();

  public PacketServerDriverAPIListener() {
    this.registerListener(new DriverPermissionManagementListener());

    this.registerListener(new DriverSpecificServiceListener());
    this.registerListener(new DriverGeneralServiceListener());
    this.registerListener(new DriverServiceFactoryListener());

    this.registerListener(new DriverServiceTaskListener());
    this.registerListener(new DriverGroupListener());

    this.registerListener(new DriverNodeInfoListener());
    this.registerListener(new DriverTemplateStorageListener());
  }

  private void registerListener(CategorizedDriverAPIListener listener) {
    this.listeners.put(listener.getCategory(), listener);
  }

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    ProtocolBuffer input = packet.getBuffer();
    DriverAPIRequestType requestType = input.readEnumConstant(DriverAPIRequestType.class);

    CategorizedDriverAPIListener listener = this.listeners.get(requestType.getCategory());
    Preconditions.checkNotNull(listener, "No listener for category " + requestType.getCategory() + " found");

    listener.handleDriverRequest(requestType, channel, packet);
  }

}
