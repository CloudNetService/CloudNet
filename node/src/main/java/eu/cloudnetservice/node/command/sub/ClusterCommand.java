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

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.collect.Lists;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.io.ZipUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.unsafe.CPUUsageResolver;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import eu.cloudnetservice.node.util.NetworkUtil;
import java.io.IOException;
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
import org.jetbrains.annotations.Nullable;

@CommandAlias("clu")
@CommandPermission("cloudnet.command.cluster")
@Description("command-cluster-description")
public final class ClusterCommand {

  public static final RowBasedFormatter<NodeServer> FORMATTER = RowBasedFormatter.<NodeServer>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "State", "Listeners", "Extra").build())
    .column(server -> server.info().uniqueId())
    .column(NodeServer::state)
    .column(server -> server.info().listeners().stream()
      .map(HostAndPort::toString)
      .collect(Collectors.joining(", ")))
    .column(server -> {
      var result = "";
      if (server.head()) {
        result += "Head";
      }
      if (server.draining()) {
        result += (result.isEmpty() ? "Draining" : ", Draining");
      }
      return result;
    })
    .build();

  private static final Logger LOGGER = LogManager.logger(ClusterCommand.class);
  private static final DateFormat DEFAULT_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Parser(suggestions = "clusterNodeServer")
  public @NonNull NodeServer defaultClusterNodeServerParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var nodeServer = Node.instance().nodeServerProvider().node(input.remove());
    if (nodeServer == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-node-not-found"));
    }

    return nodeServer;
  }

  @Suggestions("clusterNodeServer")
  public @NonNull List<String> suggestClusterNodeServer(@NonNull CommandContext<?> $, @NonNull String input) {
    return Node.instance().nodeServerProvider().nodeServers()
      .stream()
      .map(clusterNodeServer -> clusterNodeServer.info().uniqueId())
      .toList();
  }

  @Parser(suggestions = "networkClusterNode")
  public @NonNull NetworkClusterNode defaultNetworkClusterNodeParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var nodeId = input.remove();
    var clusterNode = Node.instance().clusterNodeProvider().node(nodeId);
    if (clusterNode == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-node-not-found"));
    }

    return clusterNode;
  }

  @Suggestions("networkClusterNode")
  public @NonNull List<String> suggestNetworkClusterNode(@NonNull CommandContext<?> $, @NonNull String input) {
    return Node.instance().config().clusterConfig().nodes()
      .stream()
      .map(NetworkClusterNode::uniqueId)
      .toList();
  }

  @Parser(name = "anyHostAndPort")
  public @NonNull HostAndPort defaultHostAndPortParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var address = input.remove();
    var hostAndPort = NetworkUtil.parseHostAndPort(address, true);
    if (hostAndPort == null || NetworkUtil.checkWildcard(hostAndPort)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-any-host-and-port-invalid", address));
    }

    return hostAndPort;
  }

  @Parser(name = "assignableHostAndPort", suggestions = "assignableHostAndPort")
  public @NonNull HostAndPort assignableHostAndPortParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var address = input.remove();
    var hostAndPort = NetworkUtil.parseHostAndPort(address, false);
    // check if we can parse a host and port form the given input
    if (hostAndPort == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-any-host-invalid", address));
    }
    // check if we can assign the parsed host and port
    if (NetworkUtil.checkAssignable(hostAndPort) && !NetworkUtil.checkWildcard(hostAndPort)) {
      return hostAndPort;
    }

    throw new ArgumentNotAvailableException(I18n.trans("command-assignable-host-invalid", address));
  }

  @Suggestions("assignableHostAndPort")
  public @NonNull List<String> suggestAssignableHostAndPort(@NonNull CommandContext<?> $, @NonNull String input) {
    return List.copyOf(NetworkUtil.availableIPAddresses());
  }

  @Parser(name = "anyHost")
  public @NonNull String anyHostParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var address = input.remove();
    var hostAndPort = NetworkUtil.parseHostAndPort(address, false);
    if (hostAndPort == null || NetworkUtil.checkWildcard(hostAndPort)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-any-host-invalid", address));
    }

    return hostAndPort.host();
  }

  @Parser(name = "noNodeId", suggestions = "clusterNode")
  public @NonNull String noClusterNodeParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var nodeId = input.remove();
    for (var node : Node.instance().config().clusterConfig().nodes()) {
      if (node.uniqueId().equals(nodeId)) {
        throw new ArgumentNotAvailableException(I18n.trans("command-tasks-node-not-found"));
      }
    }

    return nodeId;
  }

  @Parser(name = "staticService", suggestions = "staticService")
  public @NonNull String staticServiceParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var name = input.remove();
    var manager = Node.instance().cloudServiceProvider();
    if (manager.serviceByName(name) != null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-push-static-service-running"));
    }
    if (!Files.exists(manager.persistentServicesDirectory().resolve(name))) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-push-static-service-not-found"));
    }
    return name;
  }

  @Suggestions("staticService")
  public @NonNull List<String> suggestNotStartedStaticServices(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.resolveAllStaticServices();
  }

  @CommandMethod(value = "cluster|clu shutdown", requiredSender = ConsoleCommandSource.class)
  public void shutdownCluster(@NonNull CommandSource $) {
    for (var nodeServer : Node.instance().nodeServerProvider().nodeServers()) {
      if (nodeServer.channel() != null) {
        nodeServer.shutdown();
      }
    }
    Node.instance().stop();
  }

  @CommandMethod("cluster|clu add <nodeId> <host>")
  public void addNodeToCluster(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "nodeId", parserName = "noNodeId") String nodeId,
    @NonNull @Argument(value = "host", parserName = "anyHostAndPort") HostAndPort hostAndPort
  ) {
    Node.instance().clusterNodeProvider().addNode(new NetworkClusterNode(nodeId, Lists.newArrayList(hostAndPort)));
    source.sendMessage(I18n.trans("command-cluster-add-node-success", nodeId));
  }

  @CommandMethod("cluster|clu remove <nodeId>")
  public void removeNodeFromCluster(
    @NonNull CommandSource source,
    @NonNull @Argument("nodeId") NetworkClusterNode node
  ) {
    Node.instance().clusterNodeProvider().removeNode(node.uniqueId());
    source.sendMessage(I18n.trans("command-cluster-remove-node-success", node.uniqueId()));
  }

  @CommandMethod("cluster|clu nodes")
  public void listNodes(@NonNull CommandSource source) {
    source.sendMessage(FORMATTER.format(Node.instance().nodeServerProvider().nodeServers()));
  }

  @CommandMethod("cluster|clu node <nodeId>")
  public void listNode(@NonNull CommandSource source, @NonNull @Argument("nodeId") NodeServer nodeServer) {
    this.displayNode(source, nodeServer);
  }

  @CommandMethod("cluster|clu node <nodeId> set drain <enabled>")
  public void drainNode(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "nodeId") NodeServer nodeServer,
    @Argument("enabled") boolean enabled
  ) {
    nodeServer.drain(enabled);
    source.sendMessage(I18n.trans("command-cluster-node-set-drain", enabled ? 1 : 0, nodeServer.info().uniqueId()));
  }

  @CommandMethod("cluster|clu sync")
  public void sync(@NonNull CommandSource source) {
    source.sendMessage(I18n.trans("command-cluster-start-sync"));
    // perform a cluster sync that takes care of tasks, groups and more
    Node.instance().nodeServerProvider().syncDataIntoCluster();
  }

  @CommandMethod("cluster|clu push templates [template]")
  public void pushTemplates(@NonNull CommandSource source, @Nullable @Argument("template") ServiceTemplate template) {
    // check if we need to push all templates or just a specific one
    if (template == null) {
      var localStorage = Node.instance().templateStorageProvider().localTemplateStorage();
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
    @NonNull CommandSource source,
    @Nullable @Argument(value = "service", parserName = "staticService") String service,
    @Flag("overwrite") boolean overwrite
  ) {
    var staticServicePath = Node.instance().cloudServiceProvider().persistentServicesDirectory();
    // check if we need to push all static services or just a specific one
    if (service == null) {
      // resolve all existing static services, that are not running and push them
      for (var serviceName : this.resolveAllStaticServices()) {
        this.pushStaticService(source, staticServicePath.resolve(serviceName), serviceName, overwrite);
      }
    } else {
      // only push the specific static service that was given
      this.pushStaticService(source, staticServicePath.resolve(service), service, overwrite);
    }
  }

  private void pushStaticService(
    @NonNull CommandSource source,
    @NonNull Path servicePath,
    @NonNull String serviceName,
    boolean overwrite
  ) {
    // zip the whole directory into a stream
    var stream = ZipUtil.zipToStream(servicePath);
    // notify the source about the deployment
    source.sendMessage(I18n.trans("command-cluster-push-static-service-starting"));
    // deploy the static service into the cluster
    Node.instance().nodeServerProvider().deployStaticServiceToCluster(serviceName, stream, overwrite)
      .thenAccept(transferStatus -> {
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
      var inputStream = template.storage().zipTemplate(template);
      // check if the template really exists in the given storage
      if (inputStream != null) {
        // deploy the template into the cluster
        Node.instance().nodeServerProvider().deployTemplateToCluster(template, inputStream, true)
          .thenAccept(transferStatus -> {
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
    var manager = Node.instance().cloudServiceProvider();
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

  private void displayNode(@NonNull CommandSource source, @NonNull NodeServer node) {
    List<String> list = new ArrayList<>(Arrays.asList(
      " ",
      "Id: " + node.info().uniqueId() + (node.head() ? " (Head)" : ""),
      "State: " + node.state(),
      " ",
      "Address: "
    ));

    for (var hostAndPort : node.info().listeners()) {
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
