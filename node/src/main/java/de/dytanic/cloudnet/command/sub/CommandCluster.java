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
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.net.InetAddresses;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.config.IConfiguration;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
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
import org.jetbrains.annotations.NotNull;

@CommandAlias("clu")
@CommandPermission("cloudnet.command.cluster")
@Description("Manages the cluster and provides information about it")
public final class CommandCluster {

  private static final DateFormat DEFAULT_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  private static final Logger LOGGER = LogManager.getLogger(CommandCluster.class);

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
      // create an uri with the tpc protocol
      URI uri = URI.create("tcp://" + address);

      String host = uri.getHost();
      // check if the host and port are valid
      if (host == null || uri.getPort() == -1) {
        throw new ArgumentNotAvailableException(I18n.trans("command-cluster-invalid-host"));
      }

      InetAddress inetAddress = InetAddresses.forUriString(host);
      return new HostAndPort(inetAddress.getHostAddress(), uri.getPort());
    } catch (IllegalArgumentException exception) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-invalid-host"));
    }
  }

  @Parser(name = "noNodeId", suggestions = "clusterNode")
  public String noClusterNodeParser(CommandContext<CommandSource> $, Queue<String> input) {
    String nodeId = input.remove();
    for (NetworkClusterNode node : CloudNet.getInstance().getConfig().getClusterConfig().getNodes()) {
      if (node.getUniqueId().equals(nodeId)) {
        throw new ArgumentNotAvailableException(I18n.trans("command-tasks-node-not-found"));
      }
    }

    return nodeId;
  }

  @Parser(name = "staticService", suggestions = "staticService")
  public String staticServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();
    ICloudServiceManager manager = CloudNet.getInstance().getCloudServiceProvider();
    if (manager.getCloudServiceByName(name) != null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-cluster-push-static-service-running"));
    }
    if (!Files.exists(manager.getPersistentServicesDirectoryPath().resolve(name))) {
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
    for (IClusterNodeServer nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
      nodeServer.shutdown();
    }
  }

  @CommandMethod("cluster|clu add <nodeId> <host>")
  public void addNodeToCluster(
    CommandSource source,
    @Argument(value = "nodeId", parserName = "noNodeId") String nodeId,
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
  public void sync(CommandSource source) {
    source.sendMessage(I18n.trans("command-cluster-start-sync"));
    // perform a cluster sync that takes care of tasks, groups and more
    CloudNet.getInstance().getClusterNodeServerProvider().syncClusterData();
  }

  @CommandMethod("cluster|clu push templates [template]")
  public void pushTemplates(CommandSource source, @Argument("template") ServiceTemplate template) {
    // check if we need to push all templates or just a specific one
    if (template == null) {
      TemplateStorage localStorage = CloudNet.getInstance().getLocalTemplateStorage();
      // resolve and push all local templates
      for (ServiceTemplate localTemplate : localStorage.getTemplates()) {
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
    Path staticServicePath = CloudNet.getInstance().getCloudServiceProvider().getPersistentServicesDirectoryPath();
    // check if we need to push all static services or just a specific one
    if (service == null) {
      // resolve all existing static services, that are not running and push them
      for (String serviceName : this.resolveAllStaticServices()) {
        this.pushStaticService(source, staticServicePath.resolve(serviceName), serviceName);
      }
    } else {
      // only push the specific static service that was given
      this.pushStaticService(source, staticServicePath.resolve(service), service);
    }
  }

  private void pushStaticService(
    @NotNull CommandSource source,
    @NotNull Path servicePath,
    @NotNull String serviceName
  ) {
    // zip the whole directory into a stream
    InputStream stream = FileUtils.zipToStream(servicePath);
    // notify the source about the deployment
    source.sendMessage(I18n.trans("command-cluster-push-static-service-starting"));
    // deploy the static service into the cluster
    CloudNet.getInstance().getClusterNodeServerProvider().deployStaticServiceToCluster(serviceName, stream, true)
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

  private void pushTemplate(@NotNull CommandSource source, @NotNull ServiceTemplate template) {
    String templateName = template.toString();
    try {
      source.sendMessage(
        I18n.trans("command-cluster-push-template-compress").replace("%template%", templateName));
      // compress the template and create an InputStream
      InputStream inputStream = template.storage().zipTemplate();
      // check if the template really exists in the given storage
      if (inputStream != null) {
        // deploy the template into the cluster
        CloudNet.getInstance().getClusterNodeServerProvider().deployTemplateToCluster(template, inputStream, true)
          .onComplete(transferStatus -> {
            if (transferStatus == TransferStatus.FAILURE) {
              // the transfer failed
              source.sendMessage(
                I18n.trans("command-cluster-push-template-failed").replace("%template%", templateName));
            } else {
              // the transfer was successful
              source.sendMessage(
                I18n.trans("command-cluster-push-template-success").replace("%template%", templateName));
            }
          });
      } else {
        source.sendMessage(I18n.trans("command-template-not-found").replace("%template%", templateName));
      }
    } catch (IOException exception) {
      LOGGER.severe("An exception occurred while compressing template %s", exception, templateName);
    }
  }

  private @NotNull List<String> resolveAllStaticServices() {
    ICloudServiceManager manager = CloudNet.getInstance().getCloudServiceProvider();
    try {
      // walk through the static service directory
      return Files.walk(manager.getPersistentServicesDirectoryPath(), 1)
        // remove to root path we started at
        .filter(path -> !path.equals(manager.getPersistentServicesDirectoryPath()))
        // remove all services that are started, as we can't push them
        .filter(path -> manager.getLocalCloudService(path.getFileName().toString()) == null)
        // map to all names of the different services
        .map(path -> path.getFileName().toString())
        .collect(Collectors.toList());
    } catch (IOException e) {
      // we can't find any static service
      return Collections.emptyList();
    }
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
