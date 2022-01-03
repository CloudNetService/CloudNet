/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.command.sub;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import eu.cloudnetservice.cloudnet.common.unsafe.CPUUsageResolver;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.annotation.CommandAlias;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import java.lang.management.ManagementFactory;
import java.util.List;

@CommandAlias("info")
@CommandPermission("cloudnet.command.me")
@Description("Displays all important information about this process and the JVM")
public final class CommandMe {

  private static final String VM_NAME = System.getProperty("java.vm.name");
  private static final String VM_VERSION = System.getProperty("java.vm.version");

  private static final String UPDATE_BRANCH = System.getProperty("cloudnet.updateBranch", "master");
  private static final String UPDATE_REPO = System.getProperty("cloudnet.updateRepo", "CloudNetService/launchermeta");

  @CommandMethod("me|info")
  public void me(CommandSource commandSource) {
    var cloudNet = CloudNet.instance();
    var memoryMXBean = ManagementFactory.getMemoryMXBean();
    var nodeInfoSnapshot = cloudNet.nodeServerProvider().selfNode().nodeInfoSnapshot();

    commandSource.sendMessage(List.of(
      " ",
      CloudNet.instance().version() + " created by Dytanic, maintained by the CloudNet Community",
      "Discord: https://discord.cloudnetservice.eu/",
      " ",
      "ClusterId: " + cloudNet.config().clusterConfig().clusterId(),
      "NodeId: " + cloudNet.config().identity().uniqueId(),
      "Head-NodeId: " + cloudNet.nodeServerProvider().headnode().nodeInfo().uniqueId(),
      "CPU usage: (P/S) "
        + CPUUsageResolver.FORMAT.format(CPUUsageResolver.processCPUUsage())
        + "/"
        + CPUUsageResolver.FORMAT.format(CPUUsageResolver.systemCPUUsage())
        + "/100%",
      "Node services memory allocation (U/R/M): "
        + nodeInfoSnapshot.usedMemory()
        + "/"
        + nodeInfoSnapshot.reservedMemory()
        + "/"
        + nodeInfoSnapshot.maxMemory() + " MB",
      "Threads: " + Thread.getAllStackTraces().keySet().size(),
      "Heap usage: "
        + (memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024))
        + "/"
        + (memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024))
        + "MB",
      "JVM: " + VM_NAME + " " + VM_VERSION,
      "Update Repo: "
        + UPDATE_REPO
        + ", Update Branch: "
        + UPDATE_BRANCH
        + (cloudNet.dev() ? " (development mode)" : ""),
      " "));
  }
}
