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
import java.lang.management.ManagementFactory;
import java.util.Arrays;

@CommandAlias("info")
@CommandPermission("cloudnet.command.me")
@Description("Displays all important information about this process and the JVM")
public final class CommandMe {

  private static final String VM_VERSION = System.getProperty("java.vm.version");
  private static final String VM_NAME = System.getProperty("java.vm.name");

  @CommandMethod("me|info")
  public void me(CommandSource commandSource) {
    var cloudNet = CloudNet.getInstance();
    var memoryMXBean = ManagementFactory.getMemoryMXBean();

    var nodeInfoSnapshot = cloudNet.getClusterNodeServerProvider().getSelfNode()
      .getNodeInfoSnapshot();

    var messages = Arrays.asList(
      " ",
      CloudNet.getInstance().version() + " by Dytanic & the CloudNet Community",
      "Discord: https://discord.cloudnetservice.eu/",
      " ",
      "ClusterId: " + cloudNet.getConfig().getClusterConfig().clusterId(),
      "NodeId: " + cloudNet.getConfig().getIdentity().uniqueId(),
      "Head-NodeId: " + cloudNet.getClusterNodeServerProvider().getHeadNode().getNodeInfo().uniqueId(),
      "CPU usage: (P/S) " + CPUUsageResolver.FORMAT.format(CPUUsageResolver.getProcessCPUUsage()) + "/"
        +
        CPUUsageResolver.FORMAT.format(CPUUsageResolver.getSystemCPUUsage()) + "/100%",
      "Node services memory allocation (U/R/M): " + nodeInfoSnapshot.usedMemory() + "/" +
        nodeInfoSnapshot.reservedMemory() + "/" + nodeInfoSnapshot.maxMemory() + " MB",
      "Threads: " + Thread.getAllStackTraces().keySet().size(),
      "Heap usage: " + (memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024)) + "/" + (
        memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024)) + "MB",
      "JVM: " + VM_NAME + " " + VM_VERSION,
      " "
    );
    commandSource.sendMessage(messages);
  }
}
