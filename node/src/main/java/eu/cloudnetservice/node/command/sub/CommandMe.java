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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import eu.cloudnetservice.common.unsafe.CPUUsageResolver;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.NonNull;

@CommandAlias("info")
@CommandPermission("cloudnet.command.me")
@Description("Displays all important information about this process and the JVM")
public final class CommandMe {

  private static final Pattern UUID_REPLACE = Pattern.compile("-\\w{4}-");
  private static final String VM_NAME = System.getProperty("java.vm.name");
  private static final String VM_VERSION = System.getProperty("java.vm.version");

  private static final String UPDATE_BRANCH = System.getProperty("cloudnet.updateBranch", "release");
  private static final String UPDATE_REPO = System.getProperty("cloudnet.updateRepo", "CloudNetService/launchermeta");

  @CommandMethod("me|info")
  public void me(@NonNull CommandSource source, @Flag("showClusterId") boolean showFullClusterId) {
    var cloudNet = Node.instance();
    var memoryMXBean = ManagementFactory.getMemoryMXBean();
    var nodeInfoSnapshot = cloudNet.nodeServerProvider().localNode().nodeInfoSnapshot();

    String clusterId = cloudNet.config().clusterConfig().clusterId().toString();
    // hide the middle parts of the uuid if specified
    if (!showFullClusterId) {
      clusterId = this.removeUUIDParts(cloudNet.config().clusterConfig().clusterId());
    }

    source.sendMessage(List.of(
      " ",
      Node.instance().version() + " created by Dytanic, maintained by the CloudNet Community",
      "Discord: <https://discord.cloudnetservice.eu/>",
      " ",
      "ClusterId: " + clusterId,
      "NodeId: " + cloudNet.config().identity().uniqueId(),
      "Head-NodeId: " + cloudNet.nodeServerProvider().headNode().info().uniqueId(),
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
      "Threads: " + ProcessSnapshot.THREAD_MX_BEAN.getThreadCount(),
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

  private @NonNull String removeUUIDParts(@NonNull UUID uuid) {
    var matcher = UUID_REPLACE.matcher(uuid.toString());
    return matcher.replaceAll("-****-");
  }
}
