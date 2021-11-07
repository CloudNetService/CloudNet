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

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.net.InetAddresses;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.config.IConfiguration;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.net.InetAddress;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@CommandAlias("clu")
@CommandPermission("cloudnet.command.cluster")
@Description("Manages the cluster and provides information about it")
public final class CommandCluster {

  private static final DateFormat DEFAULT_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Parser(suggestions = "clusterNodeServer")
  public IClusterNodeServer defaultClusterNodeServerParser(CommandContext<CommandSource> $, Queue<String> input) {
    String nodeId = input.remove();
    IClusterNodeServer nodeServer = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServer(nodeId);
    if (nodeServer == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-node-not-found"));
    }

    return nodeServer;
  }

  @Suggestions("clusterNodeServer")
  public List<String> suggestClusterNodeServer(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()
      .stream()
      .map(clusterNodeServer -> clusterNodeServer.getNodeInfo().getUniqueId())
      .collect(Collectors.toList());
  }

  @Parser(suggestions = "networkClusterNode")
  public NetworkClusterNode defaultNetworkClusterNodeParser(CommandContext<CommandSource> $, Queue<String> input) {
    String nodeId = input.remove();
    NetworkClusterNode clusterNode = CloudNet.getInstance().getNodeInfoProvider().getNode(nodeId);
    if (clusterNode == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-node-not-found"));
    }

    return clusterNode;
  }

  @Suggestions("networkClusterNode")
  public List<String> suggestNetworkClusterNode(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getConfig().getClusterConfig().getNodes()
      .stream()
      .map(NetworkClusterNode::getUniqueId)
      .collect(Collectors.toList());
  }

  @Parser
  public HostAndPort defaultHostAndPortParser(CommandContext<CommandSource> $, Queue<String> input) {
    String address = input.remove();
    try {
      URI uri = URI.create("tcp://" + address);

      String host = uri.getHost();
      if (host == null || uri.getPort() == -1) {
        throw new ArgumentNotAvailableException(I18n.trans("command-cluster-invalid-host"));
      }

      InetAddress inetAddress = InetAddresses.forUriString(host);
      return new HostAndPort(inetAddress.getHostAddress(), uri.getPort());
    } catch (IllegalArgumentException exception) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-invalid-host"));
    }
  }

  @CommandMethod("cluster|clu shutdown")
  public void shutdownCluster(CommandSource source) {
    for (IClusterNodeServer nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
      nodeServer.shutdown();
    }
  }

  @CommandMethod("cluster|clu add <nodeId> <host>")
  public void addNodeToCluster(
    CommandSource source,
    @Argument(value = "nodeId", parserName = "nodeId") String nodeId,
    @Argument("host") HostAndPort hostAndPort
  ) {
    IConfiguration nodeConfig = CloudNet.getInstance().getConfig();
    NetworkCluster networkCluster = nodeConfig.getClusterConfig();
    // add the new node to the cluster config
    networkCluster.getNodes().add(new NetworkClusterNode(nodeId, new HostAndPort[]{hostAndPort}));
    nodeConfig.setClusterConfig(networkCluster);
    // write the changes to the file
    nodeConfig.save();
    source.sendMessage(I18n.trans("command-cluster-create-node-success"));
  }

  @CommandMethod("cluster|clu remove <nodeId>")
  public void removeNodeFromCluster(CommandSource source, @Argument("nodeId") NetworkClusterNode node) {
    IConfiguration nodeConfig = CloudNet.getInstance().getConfig();
    NetworkCluster cluster = nodeConfig.getClusterConfig();
    // try to remove the node from the cluster config
    if (cluster.getNodes().remove(node)) {
      // update the cluster config in the node config
      nodeConfig.setClusterConfig(cluster);
      // write the node config
      nodeConfig.save();

      source.sendMessage(I18n.trans("command-cluster-remove-node-success"));
    }
  }

  @CommandMethod("cluster|clu nodes")
  public void listNodes(CommandSource source) {
    for (IClusterNodeServer nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
      this.displayNode(source, nodeServer);
    }
  }

  @CommandMethod("cluster|clu node <nodeId>")
  public void listNode(CommandSource source, @Argument(value = "nodeId") IClusterNodeServer nodeServer) {
    this.displayNode(source, nodeServer);
  }

  @CommandMethod("cluster|clu sync")
  public void sync() {
    CloudNet.getInstance().getDataSyncRegistry().registerHandler(
      DataSyncHandler.<ServiceTask>builder()
        .key("service_task")
        .convertObject(ServiceTask.class)
        .nameExtractor(INameable::getName)
        .writer(task -> CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(task))
        .currentGetter(task -> CloudNet.getInstance().getServiceTaskProvider().getServiceTask(task.getName()))
        .dataCollector(() -> CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks())
        .build());

    ChannelMessage.builder()
      .message("sync_cluster_data")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(CloudNet.getInstance().getDataSyncRegistry().prepareClusterData(false))
      .targetNodes()
      .build()
      .send();
  }

  private void displayNode(CommandSource source, IClusterNodeServer node) {
    List<String> list = new ArrayList<>(Arrays.asList(
      " ",
      "Id: " + node.getNodeInfo().getUniqueId() + (node.isHeadNode() ? " (Head)" : ""),
      "State: " + (node.isConnected() ? "Connected" : "Not connected"),
      " ",
      "Address: "
    ));

    for (HostAndPort hostAndPort : node.getNodeInfo().getListeners()) {
      list.add("- " + hostAndPort.getHost() + ":" + hostAndPort.getPort());
    }

    if (node.getNodeInfoSnapshot() != null) {
      list.add(" ");
      list.add("* ClusterNodeInfoSnapshot from " + DEFAULT_FORMAT
        .format(node.getNodeInfoSnapshot().getCreationTime()));

      list.addAll(Arrays.asList(
        "CloudServices (" + node.getNodeInfoSnapshot().getCurrentServicesCount() + ") memory usage (U/R/M): "
          + node.getNodeInfoSnapshot().getUsedMemory() + "/" + node.getNodeInfoSnapshot().getReservedMemory()
          + "/" + node.getNodeInfoSnapshot().getMaxMemory() + " MB",
        " ",
        "CPU usage process: " + CPUUsageResolver.FORMAT
          .format(node.getNodeInfoSnapshot().getProcessSnapshot().getCpuUsage()) + "%",
        "CPU usage system: " + CPUUsageResolver.FORMAT
          .format(node.getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage()) + "%",
        "Threads: " + node.getNodeInfoSnapshot().getProcessSnapshot().getThreads().size(),
        "Heap usage: " + (node.getNodeInfoSnapshot().getProcessSnapshot().getHeapUsageMemory() / (1024 * 1024)) + "/" +
          (node.getNodeInfoSnapshot().getProcessSnapshot().getMaxHeapMemory() / (1024 * 1024)) + "MB",
        " "
      ));
    }
    source.sendMessage(list);
  }

}
