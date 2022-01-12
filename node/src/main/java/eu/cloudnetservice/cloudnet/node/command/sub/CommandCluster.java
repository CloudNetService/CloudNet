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

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.net.InetAddresses;
import eu.cloudnetservice.cloudnet.common.column.ColumnFormatter;
import eu.cloudnetservice.cloudnet.common.column.RowBasedFormatter;
import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.common.unsafe.CPUUsageResolver;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.ClusterNodeServer;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServer;
import eu.cloudnetservice.cloudnet.node.command.annotation.CommandAlias;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import lombok.NonNull;

@CommandAlias("clu")
@CommandPermission("cloudnet.command.cluster")
@Description("Manages the cluster and provides information about it")
public final class CommandCluster {

  private static final Logger LOGGER = LogManager.logger(CommandCluster.class);
  private static final DateFormat DEFAULT_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  private static final RowBasedFormatter<ClusterNodeServer> FORMATTER = RowBasedFormatter.<ClusterNodeServer>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "State", "Listeners").build())
    .column(server -> server.nodeInfo().uniqueId())
    .column(server -> {
      // we can display much more information if the node is connected
      if (server.connected()) {
        if (server.headNode() && server.drain()) {
          return "Connected (Head, Draining)";
        } else if (server.headNode()) {
          return "Connected (Head)";
        } else if (server.drain()) {
          return "Connected (Draining)";
        } else {
          return "Connected";
        }
      } else {
        return "Not connected";
      }
    })
    .column(server -> Arrays.stream(server.nodeInfo().listeners())
      .map(HostAndPort::toString)
      .collect(Collectors.joining(", ")))
    .build();

  @Parser(suggestions = "clusterNodeServer")
  public ClusterNodeServer defaultClusterNodeServerParser(CommandContext<CommandSource> $, Queue<String> input) {
    var nodeId = input.remove();
    var nodeServer = CloudNet.instance().nodeServerProvider().nodeServer(nodeId);
    if (nodeServer == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-node-not-found"));
    }

    return nodeServer;
  }

  @Suggestions("clusterNodeServer")
  public List<String> suggestClusterNodeServer(CommandContext<CommandSource> $, String input) {
    return CloudNet.instance().nodeServerProvider().nodeServers()
      .stream()
      .map(clusterNodeServer -> clusterNodeServer.nodeInfo().uniqueId())
      .toList();
  }

  @Parser(suggestions = "selfNodeServer")
  public NodeServer selfNodeServerParser(CommandContext<CommandSource> $, Queue<String> input) {
    var nodeId = input.remove();
    var provider = CloudNet.instance().nodeServerProvider();
    var selfNode = provider.selfNode();
    // check if the user requested the own node
    if (selfNode.nodeInfo().uniqueId().equals(nodeId)) {
      return selfNode;
    }
    NodeServer nodeServer = provider.nodeServer(nodeId);
    // check if the nodeServer exists
    if (nodeServer == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-node-not-found"));
    }
    return nodeServer;
  }

  @Suggestions("selfNodeServer")
  public List<String> suggestNodeServer(CommandContext<CommandSource> $, String input) {
    var provider = CloudNet.instance().nodeServerProvider();
    var nodes = provider.nodeServers()
      .stream()
      .map(clusterNodeServer -> clusterNodeServer.nodeInfo().uniqueId())
      .collect(Collectors.toList());
    // add the own node to the suggestions
    nodes.add(provider.selfNode().nodeInfo().uniqueId());
    return nodes;
  }

  @Parser(suggestions = "networkClusterNode")
  public NetworkClusterNode defaultNetworkClusterNodeParser(CommandContext<CommandSource> $, Queue<String> input) {
    var nodeId = input.remove();
    var clusterNode = CloudNet.instance().nodeInfoProvider().node(nodeId);
    if (clusterNode == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-node-not-found"));
    }

    return clusterNode;
  }

  @Suggestions("networkClusterNode")
  public List<String> suggestNetworkClusterNode(CommandContext<CommandSource> $, String input) {
    return CloudNet.instance().config().clusterConfig().nodes()
      .stream()
      .map(NetworkClusterNode::uniqueId)
      .toList();
  }

  @Parser
  public HostAndPort defaultHostAndPortParser(CommandContext<CommandSource> $, Queue<String> input) {
    var address = input.remove();
    try {
      // create an uri with the tpc protocol
      var uri = URI.create("tcp://" + address);

      var host = uri.getHost();
      // check if the host and port are valid
      if (host == null || uri.getPort() == -1) {
        throw new ArgumentNotAvailableException(I18n.trans("command-cluster-invalid-host"));
      }

      var inetAddress = InetAddresses.forUriString(host);
      return new HostAndPort(inetAddress.getHostAddress(), uri.getPort());
    } catch (IllegalArgumentException exception) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-invalid-host"));
    }
  }

  @Parser(name = "noNodeId", suggestions = "clusterNode")
  public String noClusterNodeParser(CommandContext<CommandSource> $, Queue<String> input) {
    var nodeId = input.remove();
    for (var node : CloudNet.instance().config().clusterConfig().nodes()) {
      if (node.uniqueId().equals(nodeId)) {
        throw new ArgumentNotAvailableException(I18n.trans("command-tasks-node-not-found"));
      }
    }

    return nodeId;
  }

  @Parser(name = "staticService", suggestions = "staticService")
  public String staticServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();
    var manager = CloudNet.instance().cloudServiceProvider();
    if (manager.serviceByName(name) != null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-push-static-service-running"));
    }
    if (!Files.exists(manager.persistentServicesDirectory().resolve(name))) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-push-static-service-not-found"));
    }
    return name;
  }

  @Suggestions("staticService")
  public List<String> suggestNotStartedStaticServices(CommandContext<CommandSource> $, String input) {
    return this.resolveAllStaticServices();
  }

  @CommandMethod("cluster|clu shutdown")
  public void shutdownCluster(CommandSource source) {
    for (var nodeServer : CloudNet.instance().nodeServerProvider().nodeServers()) {
      nodeServer.shutdown();
    }
    CloudNet.instance().stop();
  }

  @CommandMethod("cluster|clu add <nodeId> <host>")
  public void addNodeToCluster(
    CommandSource source,
    @Argument(value = "nodeId", parserName = "noNodeId") String nodeId,
    @Argument("host") HostAndPort hostAndPort
  ) {
    var nodeConfig = CloudNet.instance().config();
    var networkCluster = nodeConfig.clusterConfig();
    // add the new node to the cluster config
    networkCluster.nodes().add(new NetworkClusterNode(nodeId, new HostAndPort[]{hostAndPort}));
    nodeConfig.clusterConfig(networkCluster);
    // write the changes to the file
    nodeConfig.save();
    source.sendMessage(I18n.trans("command-cluster-create-node-success"));
  }

  @CommandMethod("cluster|clu remove <nodeId>")
  public void removeNodeFromCluster(CommandSource source, @Argument("nodeId") NetworkClusterNode node) {
    var nodeConfig = CloudNet.instance().config();
    var cluster = nodeConfig.clusterConfig();
    // try to remove the node from the cluster config
    if (cluster.nodes().remove(node)) {
      // update the cluster config in the node config
      nodeConfig.clusterConfig(cluster);
      // write the node config
      nodeConfig.save();
    }

    source.sendMessage(I18n.trans("command-cluster-remove-node-success"));
  }

  @CommandMethod("cluster|clu nodes")
  public void listNodes(CommandSource source) {
    source.sendMessage(FORMATTER.format(CloudNet.instance().nodeServerProvider().nodeServers()));
  }

  @CommandMethod("cluster|clu node <nodeId>")
  public void listNode(CommandSource source, @Argument("nodeId") ClusterNodeServer nodeServer) {
    this.displayNode(source, nodeServer);
  }

  @CommandMethod("cluster|clu node <nodeId> set drain <enabled>")
  public void drainNode(
    CommandSource source,
    @Argument(value = "nodeId") NodeServer nodeServer,
    @Argument("enabled") boolean enabled
  ) {
    nodeServer.drain(enabled);
    source.sendMessage(I18n.trans("command-cluster-node-set-drain", enabled ? 1 : 0, nodeServer.nodeInfo().uniqueId()));
  }

  @CommandMethod("cluster|clu sync")
  public void sync(CommandSource source) {
    source.sendMessage(I18n.trans("command-cluster-start-sync"));
    // perform a cluster sync that takes care of tasks, groups and more
    CloudNet.instance().nodeServerProvider().syncClusterData();
  }

  @CommandMethod("cluster|clu push templates [template]")
  public void pushTemplates(CommandSource source, @Argument("template") ServiceTemplate template) {
    // check if we need to push all templates or just a specific one
    if (template == null) {
      var localStorage = CloudNet.instance().localTemplateStorage();
      // resolve and push all local templates
      for (var localTemplate : localStorage.templates()) {
        this.pushTemplate(source, localTemplate);
      }
    } else {
      // only push the specific template that was given
      this.pushTemplate(source, template);
    }
  }

  @CommandMethod("cluster|clu push staticServices [service]")
  public void pushStaticServices(
    CommandSource source,
    @Argument(value = "service", parserName = "staticService") String service,
    @Flag("overwrite") boolean overwrite
  ) {
    var staticServicePath = CloudNet.instance().cloudServiceProvider().persistentServicesDirectory();
    // check if we need to push all static services or just a specific one
    if (service == null) {
      // resolve all existing static services, that are not running and push them
      for (var serviceName : this.resolveAllStaticServices()) {
        this.pushStaticService(source, staticServicePath.resolve(serviceName), serviceName);
      }
    } else {
      // only push the specific static service that was given
      this.pushStaticService(source, staticServicePath.resolve(service), service);
    }
  }

  private void pushStaticService(
    @NonNull CommandSource source,
    @NonNull Path servicePath,
    @NonNull String serviceName
  ) {
    // zip the whole directory into a stream
    var stream = FileUtils.zipToStream(servicePath);
    // notify the source about the deployment
    source.sendMessage(I18n.trans("command-cluster-push-static-service-starting"));
    // deploy the static service into the cluster
    CloudNet.instance().nodeServerProvider().deployStaticServiceToCluster(serviceName, stream, true)
      .onComplete(transferStatus -> {
        if (transferStatus == TransferStatus.FAILURE) {
          // the transfer failed
          source.sendMessage(I18n.trans("command-cluster-push-static-service-failed"));
        } else {
          // the transfer was successful
          source.sendMessage(I18n.trans("command-cluster-push-static-service-success"));
        }
      });
  }

  private void pushTemplate(@NonNull CommandSource source, @NonNull ServiceTemplate template) {
    var templateName = template.toString();
    try {
      source.sendMessage(
        I18n.trans("command-cluster-push-template-compress", templateName));
      // compress the template and create an InputStream
      var inputStream = template.storage().zipTemplate();
      // check if the template really exists in the given storage
      if (inputStream != null) {
        // deploy the template into the cluster
        CloudNet.instance().nodeServerProvider().deployTemplateToCluster(template, inputStream, true)
          .onComplete(transferStatus -> {
            if (transferStatus == TransferStatus.FAILURE) {
              // the transfer failed
              source.sendMessage(I18n.trans("command-cluster-push-template-failed", templateName));
            } else {
              // the transfer was successful
              source.sendMessage(I18n.trans("command-cluster-push-template-success", templateName));
            }
          });
      } else {
        source.sendMessage(I18n.trans("command-template-not-found", templateName));
      }
    } catch (IOException exception) {
      LOGGER.severe("An exception occurred while compressing template %s", exception, templateName);
    }
  }

  private @NonNull List<String> resolveAllStaticServices() {
    var manager = CloudNet.instance().cloudServiceProvider();
    try {
      // walk through the static service directory
      return Files.walk(manager.persistentServicesDirectory(), 1)
        // remove to root path we started at
        .filter(path -> !path.equals(manager.persistentServicesDirectory()))
        // remove all services that are started, as we can't push them
        .filter(path -> manager.localCloudService(path.getFileName().toString()) == null)
        // map to all names of the different services
        .map(path -> path.getFileName().toString())
        .toList();
    } catch (IOException e) {
      // we can't find any static service
      return Collections.emptyList();
    }
  }

  private void displayNode(@NonNull CommandSource source, @NonNull ClusterNodeServer node) {
    List<String> list = new ArrayList<>(Arrays.asList(
      " ",
      "Id: " + node.nodeInfo().uniqueId() + (node.headNode() ? " (Head)" : ""),
      "State: " + (node.connected() ? "Connected" : "Not connected"),
      " ",
      "Address: "
    ));

    for (var hostAndPort : node.nodeInfo().listeners()) {
      list.add("- " + hostAndPort.host() + ":" + hostAndPort.port());
    }

    if (node.nodeInfoSnapshot() != null) {
      list.add(" ");
      list.add("* ClusterNodeInfoSnapshot from " + DEFAULT_FORMAT
        .format(node.nodeInfoSnapshot().creationTime()));

      list.addAll(Arrays.asList(
        "CloudServices (" + node.nodeInfoSnapshot().currentServicesCount() + ") memory usage (U/R/M): "
          + node.nodeInfoSnapshot().usedMemory() + "/" + node.nodeInfoSnapshot().reservedMemory()
          + "/" + node.nodeInfoSnapshot().maxMemory() + " MB",
        " ",
        "CPU usage process: " + CPUUsageResolver.FORMAT
          .format(node.nodeInfoSnapshot().processSnapshot().cpuUsage()) + "%",
        "CPU usage system: " + CPUUsageResolver.FORMAT
          .format(node.nodeInfoSnapshot().processSnapshot().systemCpuUsage()) + "%",
        "Threads: " + node.nodeInfoSnapshot().processSnapshot().threads().size(),
        "Heap usage: " + (node.nodeInfoSnapshot().processSnapshot().heapUsageMemory() / (1024 * 1024)) + "/" +
          (node.nodeInfoSnapshot().processSnapshot().maxHeapMemory() / (1024 * 1024)) + "MB",
        " "
      ));
    }
    source.sendMessage(list);
  }

}
