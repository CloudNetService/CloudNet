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
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.HashMap;
import java.util.Map;

public class CategorizedDriverAPIListener {

  private final DriverAPICategory category;
  private final Map<DriverAPIRequestType, DriverAPIHandler> handlers;

  public CategorizedDriverAPIListener(DriverAPICategory category) {
    this.category = category;
    this.handlers = new HashMap<>();
  }

  public DriverAPICategory getCategory() {
    return this.category;
  }

  protected void registerHandler(DriverAPIRequestType requestType, DriverAPIHandler handler) {
    Preconditions.checkArgument(requestType.getCategory() == this.category,
      "RequestType " + requestType + " is not from category " + this.category);
    this.handlers.put(requestType, handler);
  }

  public void handleDriverRequest(DriverAPIRequestType requestType, INetworkChannel channel, IPacket packet) {
    DriverAPIHandler handler = this.handlers.get(requestType);
    Preconditions.checkNotNull(handler, "RequestType " + requestType + " not registered for category" + this.category);
    ProtocolBuffer response = handler.handle(channel, packet, packet.getBuffer());
    if (response != null) {
      channel.sendPacket(Packet.createResponseFor(packet, response));
    }
  }

}
