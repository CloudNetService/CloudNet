/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.command.sub;

import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.Node;
import eu.cloudnetservice.utils.base.resource.CpuUsageResolver;
import eu.cloudnetservice.utils.base.resource.ResourceFormatter;
import jakarta.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;

@Singleton
@CommandAlias("info")
@Permission("cloudnet.command.me")
@Description("command-me-description")
public final class MeCommand {

  private static final Pattern UUID_REPLACE_PATTERN = Pattern.compile("-\\w{4}-");

  private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();

  private static final String UPDATE_BRANCH = System.getProperty("cloudnet.updateBranch", "release");
  private static final String UPDATE_REPO = System.getProperty("cloudnet.updateRepo", "CloudNetService/launchermeta");

  @Command("me|info")
  public void me(
    @NonNull CloudNetVersion version,
    @NonNull Configuration configuration,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CommandSource source,
    @Flag("showClusterId") boolean showFullClusterId) {
    var nodeInfoSnapshot = nodeServerProvider.localNode().nodeInfoSnapshot();

    // hide the middle parts of the uuid if not explicitly requested to show them
    var clusterId = configuration.clusterConfig().clusterId().toString();
    if (!showFullClusterId) {
      var matcher = UUID_REPLACE_PATTERN.matcher(clusterId);
      clusterId = matcher.replaceAll("-****-");
    }

    source.sendMessage(List.of(
      " ",
      version.toString(),
      "Discord: <https://discord.cloudnetservice.eu/>",
      " ",
      "ClusterId: " + clusterId,
      "NodeId: " + configuration.identity().uniqueId(),
      "Head-NodeId: " + nodeServerProvider.headNode().info().uniqueId(),
      "CPU usage: (P/S) "
        + ResourceFormatter.formatTwoDigitPrecision(CpuUsageResolver.processCpuLoad())
        + "/"
        + ResourceFormatter.formatTwoDigitPrecision(CpuUsageResolver.systemCpuLoad())
        + "/100%",
      "Node services memory allocation (U/R/M): "
        + nodeInfoSnapshot.usedMemory()
        + "/"
        + nodeInfoSnapshot.reservedMemory()
        + "/"
        + nodeInfoSnapshot.maxMemory() + " MB",
      "Threads: " + THREAD_MX_BEAN.getThreadCount(),
      "Heap usage: "
        + (MEMORY_MX_BEAN.getHeapMemoryUsage().getUsed() / (1024 * 1024))
        + "/"
        + (MEMORY_MX_BEAN.getHeapMemoryUsage().getMax() / (1024 * 1024))
        + "MB",
      "JVM: "
        + RUNTIME_MX_BEAN.getVmVendor()
        + " "
        + RUNTIME_MX_BEAN.getSpecVersion()
        + " ("
        + RUNTIME_MX_BEAN.getVmName()
        + " "
        + RUNTIME_MX_BEAN.getVmVersion()
        + ")",
      "Update Repo: "
        + UPDATE_REPO
        + ", Update Branch: "
        + UPDATE_BRANCH
        + (Node.DEV_MODE ? " (development mode)" : ""),
      " "));
  }
}
