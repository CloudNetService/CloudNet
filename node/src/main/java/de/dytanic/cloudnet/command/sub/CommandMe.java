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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Arrays;
import java.util.List;

@CommandAlias("info")
@CommandPermission("cloudnet.command.me")
@Description("Displays all important information about this process and the JVM")
public class CommandMe {

  //TODO klaro du willst hier komische java sachen anzeigen
  @CommandMethod("me|info")
  public void me(CommandSource commandSource) {
    CloudNet cloudNet = CloudNet.getInstance();
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    NetworkClusterNodeInfoSnapshot nodeInfoSnapshot = cloudNet.getClusterNodeServerProvider().getSelfNode()
      .getNodeInfoSnapshot();

    List<String> messages = Arrays.asList(
      " ",
      CloudNet.getInstance().getVersion() + " by Dytanic & the CloudNet Community",
      "Discord: https://discord.cloudnetservice.eu/",
      " ",
      "ClusterId: " + cloudNet.getConfig().getClusterConfig().getClusterId(),
      "NodeId: " + cloudNet.getConfig().getIdentity().getUniqueId(),
      "Head-NodeId: " + cloudNet.getClusterNodeServerProvider().getHeadNode().getNodeInfo().getUniqueId(),
      "CPU usage: (P/S) " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getProcessCPUUsage()) + "/"
        +
        CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getSystemCPUUsage()) + "/100%",
      "Node services memory allocation (U/R/M): " + nodeInfoSnapshot.getUsedMemory() + "/" +
        nodeInfoSnapshot.getReservedMemory() + "/" + nodeInfoSnapshot.getMaxMemory() + " MB",
      "Threads: " + Thread.getAllStackTraces().keySet().size(),
      "Heap usage: " + (memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024)) + "/" + (
        memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024)) + "MB",
      " "
    );
    commandSource.sendMessage(messages);
  }
}
