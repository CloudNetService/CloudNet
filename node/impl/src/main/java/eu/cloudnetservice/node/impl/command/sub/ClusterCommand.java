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

import com.google.common.collect.Lists;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.command.source.ConsoleCommandSource;
import eu.cloudnetservice.node.impl.tick.DefaultShutdownHandler;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.utils.base.column.ColumnFormatter;
import eu.cloudnetservice.utils.base.column.RowedFormatter;
import eu.cloudnetservice.utils.base.io.ZipUtil;
import eu.cloudnetservice.utils.base.resource.ResourceFormatter;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@CommandAlias("clu")
@Permission("cloudnet.command.cluster")
@Description("command-cluster-description")
public final class ClusterCommand {

  public static final RowedFormatter<NodeServer> FORMATTER = RowedFormatter.<NodeServer>builder()
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

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCommand.class);
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final Configuration configuration;
  private final CloudServiceManager serviceProvider;
  private final NodeServerProvider nodeServerProvider;
  private final ClusterNodeProvider clusterNodeProvider;
  private final TemplateStorageProvider templateStorageProvider;
  private final Provider<DefaultShutdownHandler> shutdownHandlerProvider;

  @Inject
  public ClusterCommand(
    @NonNull Configuration configuration,
    @NonNull CloudServiceManager serviceProvider,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull ClusterNodeProvider clusterNodeProvider,
    @NonNull TemplateStorageProvider templateStorageProvider,
    @NonNull Provider<DefaultShutdownHandler> shutdownHandlerProvider
  ) {
    this.configuration = configuration;
    this.nodeServerProvider = nodeServerProvider;
    this.clusterNodeProvider = clusterNodeProvider;
    this.serviceProvider = serviceProvider;
    this.templateStorageProvider = templateStorageProvider;
    this.shutdownHandlerProvider = shutdownHandlerProvider;
  }

  @Parser(suggestions = "clusterNodeServer")
  public @NonNull NodeServer defaultClusterNodeServerParser(@NonNull CommandInput input, @NonNull @Service I18n i18n) {
    var nodeServer = this.nodeServerProvider.node(input.readString());
    if (nodeServer == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-cluster-node-not-found"));
    }

    return nodeServer;
  }

  @Suggestions("clusterNodeServer")
  public @NonNull Stream<String> suggestClusterNodeServer() {
    return this.nodeServerProvider.nodeServers().stream().map(clusterNodeServer -> clusterNodeServer.info().uniqueId());
  }

  @Parser(suggestions = "networkClusterNode")
  public @NonNull NetworkClusterNode defaultNetworkClusterNodeParser(@NonNull CommandInput input, @NonNull I18n i18n) {
    var nodeId = input.readString();
    var clusterNode = this.clusterNodeProvider.node(nodeId);
    if (clusterNode == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-cluster-node-not-found"));
    }

    return clusterNode;
  }

  @Suggestions("networkClusterNode")
  public @NonNull Stream<String> suggestNetworkClusterNode() {
    return this.configuration.clusterConfig().nodes().stream().map(NetworkClusterNode::uniqueId);
  }

  @Parser(name = "anyHostAndPort")
  public @NonNull HostAndPort defaultHostAndPortParser(@NonNull CommandInput input, @NonNull @Service I18n i18n) {
    var address = input.readString();
    var hostAndPort = NetworkUtil.parseHostAndPort(address, true);
    if (hostAndPort == null || NetworkUtil.checkWildcard(hostAndPort)) {
      throw new ArgumentNotAvailableException(i18n.translate("command-any-host-and-port-invalid", address));
    }

    return hostAndPort;
  }

  @Parser(name = "assignableHostAndPort", suggestions = "assignableHostAndPort")
  public @NonNull HostAndPort assignableHostAndPortParser(@NonNull CommandInput input, @NonNull @Service I18n i18n) {
    var address = input.readInput();
    var hostAndPort = NetworkUtil.parseHostAndPort(address, false);
    // check if we can parse a host and port form the given input
    if (hostAndPort == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-any-host-invalid", address));
    }
    // check if we can assign the parsed host and port
    if (NetworkUtil.checkAssignable(hostAndPort) && !NetworkUtil.checkWildcard(hostAndPort)) {
      return hostAndPort;
    }

    throw new ArgumentNotAvailableException(i18n.translate("command-assignable-host-invalid", address));
  }

  @Suggestions("assignableHostAndPort")
  public @NonNull List<String> suggestAssignableHostAndPort() {
    return List.copyOf(NetworkUtil.availableIPAddresses());
  }

  @Parser(name = "anyHost")
  public @NonNull String anyHostParser(@NonNull CommandInput input, @NonNull @Service I18n i18n) {
    var address = input.readString();
    var hostAndPort = NetworkUtil.parseHostAndPort(address, false);
    if (hostAndPort == null || NetworkUtil.checkWildcard(hostAndPort)) {
      throw new ArgumentNotAvailableException(i18n.translate("command-any-host-invalid", address));
    }

    return hostAndPort.host();
  }

  @Parser(name = "noNodeId", suggestions = "clusterNode")
  public @NonNull String noClusterNodeParser(@NonNull CommandInput input, @NonNull @Service I18n i18n) {
    var nodeId = input.readString();
    for (var node : this.configuration.clusterConfig().nodes()) {
      if (node.uniqueId().equals(nodeId)) {
        throw new ArgumentNotAvailableException(i18n.translate("command-tasks-node-not-found"));
      }
    }

    return nodeId;
  }

  @Parser(name = "staticService", suggestions = "staticService")
  public @NonNull String staticServiceParser(@NonNull CommandInput input, @NonNull @Service I18n i18n) {
    var name = input.readString();
    if (this.serviceProvider.serviceByName(name) != null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-cluster-push-static-service-running"));
    }
    if (!Files.exists(this.serviceProvider.persistentServicesDirectory().resolve(name))) {
      throw new ArgumentNotAvailableException(i18n.translate("command-cluster-push-static-service-not-found"));
    }
    return name;
  }

  @Suggestions("staticService")
  public @NonNull List<String> suggestNotStartedStaticServices() {
    return this.resolveAllStaticServices();
  }

  @Command(value = "cluster|clu shutdown", requiredSender = ConsoleCommandSource.class)
  public void shutdownCluster() {
    for (var nodeServer : this.nodeServerProvider.nodeServers()) {
      if (nodeServer.channel() != null) {
        nodeServer.shutdown();
      }
    }

    this.shutdownHandlerProvider.get().shutdown();
  }

  @Command("cluster|clu add <nodeId> <host>")
  public void addNodeToCluster(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument(value = "nodeId", parserName = "noNodeId") String nodeId,
    @NonNull @Argument(value = "host", parserName = "anyHostAndPort") HostAndPort hostAndPort
  ) {
    this.clusterNodeProvider.addNode(new NetworkClusterNode(nodeId, Lists.newArrayList(hostAndPort)));
    source.sendMessage(i18n.translate("command-cluster-add-node-success", nodeId));
  }

  @Command("cluster|clu remove <nodeId>")
  public void removeNodeFromCluster(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("nodeId") NetworkClusterNode node
  ) {
    this.clusterNodeProvider.removeNode(node.uniqueId());
    source.sendMessage(i18n.translate("command-cluster-remove-node-success", node.uniqueId()));
  }

  @Command("cluster|clu nodes")
  public void listNodes(@NonNull CommandSource source) {
    source.sendMessage(FORMATTER.format(this.nodeServerProvider.nodeServers()));
  }

  @Command("cluster|clu node <nodeId>")
  public void listNode(@NonNull CommandSource source, @NonNull @Argument("nodeId") NodeServer nodeServer) {
    this.displayNode(source, nodeServer);
  }

  @Command("cluster|clu node <nodeId> set drain <enabled>")
  public void drainNode(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument(value = "nodeId") NodeServer nodeServer,
    @Argument("enabled") boolean enabled
  ) {
    nodeServer.drain(enabled);
    source.sendMessage(i18n.translate("command-cluster-node-set-drain", enabled ? 1 : 0, nodeServer.info().uniqueId()));
  }

  @Command("cluster|clu sync")
  public void sync(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    source.sendMessage(i18n.translate("command-cluster-start-sync"));
    // perform a cluster sync that takes care of tasks, groups and more
    this.nodeServerProvider.syncDataIntoCluster();
  }

  @Command("cluster|clu push templates [template]")
  public void pushTemplates(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Nullable @Argument("template") ServiceTemplate template
  ) {
    // check if we need to push all templates or just a specific one
    if (template == null) {
      var localStorage = this.templateStorageProvider.localTemplateStorage();
      // resolve and push all local templates
      for (var localTemplate : localStorage.templates()) {
        this.pushTemplate(i18n, source, localTemplate);
      }
    } else {
      // only push the specific template that was given
      this.pushTemplate(i18n, source, template);
    }
  }

  @Command("cluster|clu push staticServices [service]")
  public void pushStaticServices(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Nullable @Argument(value = "service", parserName = "staticService") String service,
    @Flag("overwrite") boolean overwrite
  ) {
    var staticServicePath = this.serviceProvider.persistentServicesDirectory();
    // check if we need to push all static services or just a specific one
    if (service == null) {
      // resolve all existing static services, that are not running and push them
      for (var serviceName : this.resolveAllStaticServices()) {
        this.pushStaticService(i18n, source, staticServicePath.resolve(serviceName), serviceName, overwrite);
      }
    } else {
      // only push the specific static service that was given
      this.pushStaticService(i18n, source, staticServicePath.resolve(service), service, overwrite);
    }
  }

  private void pushStaticService(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull Path servicePath,
    @NonNull String serviceName,
    boolean overwrite
  ) {
    // zip the whole directory into a stream
    var stream = ZipUtil.zipToStream(servicePath);
    // notify the source about the deployment
    source.sendMessage(i18n.translate("command-cluster-push-static-service-starting"));
    // deploy the static service into the cluster
    this.nodeServerProvider.deployStaticServiceToCluster(serviceName, stream, overwrite)
      .thenAccept(transferStatus -> {
        if (transferStatus == TransferStatus.FAILURE) {
          // the transfer failed
          source.sendMessage(i18n.translate("command-cluster-push-static-service-failed"));
        } else {
          // the transfer was successful
          source.sendMessage(i18n.translate("command-cluster-push-static-service-success"));
        }
      });
  }

  private void pushTemplate(@NonNull @Service I18n i18n, @NonNull CommandSource source, @NonNull ServiceTemplate template) {
    var templateName = template.toString();
    try {
      source.sendMessage(
        i18n.translate("command-cluster-push-template-compress", templateName));
      // compress the template and create an InputStream
      var inputStream = template.storage().zipTemplate(template);
      // check if the template really exists in the given storage
      if (inputStream != null) {
        // deploy the template into the cluster
        this.nodeServerProvider.deployTemplateToCluster(template, inputStream, true).whenComplete((status, ex) -> {
          if (ex != null || status == TransferStatus.FAILURE) {
            // the transfer failed
            source.sendMessage(i18n.translate("command-cluster-push-template-failed", templateName));

            // print the detailed exception, if available
            if (ex != null) {
              LOGGER.error("Unable to push template {} to cluster", template, ex);
            }
          } else {
            // the transfer was successful
            source.sendMessage(i18n.translate("command-cluster-push-template-success", templateName));
          }
        });
      } else {
        source.sendMessage(i18n.translate("command-template-not-found", templateName));
      }
    } catch (IOException exception) {
      LOGGER.error("An exception occurred while compressing template {}", templateName, exception);
    }
  }

  private @NonNull List<String> resolveAllStaticServices() {
    try {
      // walk through the static service directory
      return Files.walk(this.serviceProvider.persistentServicesDirectory(), 1)
        // remove to root path we started at
        .filter(path -> !path.equals(this.serviceProvider.persistentServicesDirectory()))
        // remove all services that are started, as we can't push them
        .filter(path -> this.serviceProvider.localCloudService(path.getFileName().toString()) == null)
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

    var nodeSnapshot = node.nodeInfoSnapshot();
    if (nodeSnapshot != null) {
      list.add(" ");

      // format & add the creation timestamp
      var creationTime = Instant.ofEpochMilli(nodeSnapshot.creationTime()).atZone(ZoneId.systemDefault());
      list.add("* ClusterNodeInfoSnapshot from " + TIME_FORMATTER.format(creationTime));

      list.addAll(Arrays.asList(
        "CloudServices (" + node.nodeInfoSnapshot().currentServicesCount() + ") memory usage (U/R/M): "
          + node.nodeInfoSnapshot().usedMemory() + "/" + node.nodeInfoSnapshot().reservedMemory()
          + "/" + node.nodeInfoSnapshot().maxMemory() + " MB",
        " ",
        "CPU usage process: " + ResourceFormatter.formatTwoDigitPrecision(
          node.nodeInfoSnapshot().processSnapshot().cpuUsage()) + "%",
        "CPU usage system: " + ResourceFormatter.formatTwoDigitPrecision(
          node.nodeInfoSnapshot().processSnapshot().systemCpuUsage()) + "%",
        "Threads: " + node.nodeInfoSnapshot().processSnapshot().threads().size(),
        "Heap usage: " + (node.nodeInfoSnapshot().processSnapshot().heapUsageMemory() / (1024 * 1024)) + "/" +
          (node.nodeInfoSnapshot().processSnapshot().maxHeapMemory() / (1024 * 1024)) + "MB",
        " "
      ));
    }
    source.sendMessage(list);
  }
}
