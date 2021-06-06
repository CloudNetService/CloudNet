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

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.Collection;

public class DriverNodeInfoListener extends CategorizedDriverAPIListener {

  public DriverNodeInfoListener() {
    super(DriverAPICategory.NODE_INFO);

    super.registerHandler(DriverAPIRequestType.GET_CONSOLE_COMMANDS, (channel, packet, input) -> {
      Collection<CommandInfo> infos = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommands();
      return ProtocolBuffer.create().writeObjectCollection(infos);
    });

    super.registerHandler(DriverAPIRequestType.GET_CONSOLE_COMMAND_BY_LINE, (channel, packet, input) -> {
      CommandInfo info = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(input.readString());
      return ProtocolBuffer.create().writeOptionalObject(info);
    });

    super.registerHandler(DriverAPIRequestType.TAB_COMPLETE_CONSOLE_COMMAND, (channel, packet, input) -> {
      Collection<String> results = CloudNetDriver.getInstance().getNodeInfoProvider()
        .getConsoleTabCompleteResults(input.readString());
      return ProtocolBuffer.create().writeStringCollection(results);
    });

    super.registerHandler(DriverAPIRequestType.SEND_COMMAND_LINE, (channel, packet, input) -> {
      String[] results = CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLine(input.readString());
      return ProtocolBuffer.create().writeStringArray(results);
    });

    super.registerHandler(DriverAPIRequestType.SEND_COMMAND_LINE_TO_NODE, (channel, packet, input) -> {
      String[] results = CloudNetDriver.getInstance().getNodeInfoProvider()
        .sendCommandLine(input.readString(), input.readString());
      return ProtocolBuffer.create().writeStringArray(results);
    });

    super.registerHandler(DriverAPIRequestType.SEND_COMMAND_LINE_AS_PERMISSION_USER, (channel, packet, buffer) -> {
      Pair<Boolean, String[]> response = CloudNetDriver.getInstance()
        .sendCommandLineAsPermissionUser(buffer.readUUID(), buffer.readString());
      return ProtocolBuffer.create().writeBoolean(response.getFirst()).writeStringArray(response.getSecond());
    });

    super.registerHandler(DriverAPIRequestType.GET_NODES, (channel, packet, input) -> {
      NetworkClusterNode[] nodes = CloudNetDriver.getInstance().getNodeInfoProvider().getNodes();
      return ProtocolBuffer.create().writeObjectArray(nodes);
    });

    super.registerHandler(DriverAPIRequestType.GET_NODE_BY_UNIQUE_ID, (channel, packet, input) -> {
      NetworkClusterNode node = CloudNetDriver.getInstance().getNodeInfoProvider().getNode(input.readString());
      return ProtocolBuffer.create().writeOptionalObject(node);
    });

    super.registerHandler(DriverAPIRequestType.GET_NODE_INFO_SNAPSHOTS, (channel, packet, input) -> {
      NetworkClusterNodeInfoSnapshot[] snapshots = CloudNetDriver.getInstance().getNodeInfoProvider()
        .getNodeInfoSnapshots();
      return ProtocolBuffer.create().writeObjectArray(snapshots);
    });

    super.registerHandler(DriverAPIRequestType.GET_NODE_INFO_SNAPSHOT_BY_UNIQUE_ID, (channel, packet, input) -> {
      NetworkClusterNodeInfoSnapshot snapshot = CloudNetDriver.getInstance().getNodeInfoProvider()
        .getNodeInfoSnapshot(input.readString());
      return ProtocolBuffer.create().writeOptionalObject(snapshot);
    });

  }
}
