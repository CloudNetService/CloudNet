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

package de.dytanic.cloudnet.command.commands;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.template;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.validHostAndPort;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandCluster extends SubCommandHandler {

  public CommandCluster() {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            for (IClusterNodeServer node : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
              node.sendCommandLine("stop");
            }

            CloudNet.getInstance().getCommandMap().dispatchCommand(sender, "stop");
          },
          anyStringIgnoreCase("shutdown", "stop")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            NetworkCluster networkCluster = CloudNet.getInstance().getConfig().getClusterConfig();
            HostAndPort hostAndPort = (HostAndPort) args.argument(2);
            networkCluster.getNodes().add(new NetworkClusterNode(
              (String) args.argument(1),
              new HostAndPort[]{hostAndPort}
            ));
            CloudNet.getInstance().getConfig().getIpWhitelist().add(hostAndPort
              .getHost()); //setClusterConfig already saves, so we don't have to call the save method again to save the ipWhitelist
            CloudNet.getInstance().getConfig().setClusterConfig(networkCluster);
            CloudNet.getInstance().getClusterNodeServerProvider().setClusterServers(networkCluster);

            sender.sendMessage(LanguageManager.getMessage("command-cluster-create-node-success"));
          },
          exactStringIgnoreCase("add"),
          dynamicString(
            "nodeId",
            LanguageManager.getMessage("command-cluster-create-node-already-existing"),
            nodeId -> CloudNet.getInstance().getConfig().getClusterConfig().getNodes().stream()
              .noneMatch(node -> node.getUniqueId().equals(nodeId))
          ),
          validHostAndPort("host")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            NetworkCluster networkCluster = CloudNet.getInstance().getConfig().getClusterConfig();

            networkCluster.getNodes().removeIf(node -> node.getUniqueId().equals(args.argument(
              1))); //always true because the predicate given in the arguments of this SubCommand returns false if no node with that id is found

            CloudNet.getInstance().getConfig().setClusterConfig(networkCluster);
            CloudNet.getInstance().getClusterNodeServerProvider().setClusterServers(networkCluster);

            sender.sendMessage(LanguageManager.getMessage("command-cluster-remove-node-success"));
          },
          anyStringIgnoreCase("remove", "rm"),
          dynamicString(
            "nodeId",
            LanguageManager.getMessage("command-cluster-node-not-found"),
            nodeId -> CloudNet.getInstance().getConfig().getClusterConfig().getNodes().stream()
              .anyMatch(node -> node.getUniqueId().equals(nodeId)),
            () -> CloudNet.getInstance().getConfig().getClusterConfig().getNodes().stream()
              .map(NetworkClusterNode::getUniqueId).collect(Collectors.toList())
          )
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> displayNodes(sender),
          exactStringIgnoreCase("nodes")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()
              .stream()
              .filter(node -> node.getNodeInfo().getUniqueId().equals(args.argument(1)))
              .findFirst()
              .ifPresent(node -> displayNode(sender, node));
          },
          anyStringIgnoreCase("node"),
          dynamicString(
            "nodeId",
            LanguageManager.getMessage("command-cluster-node-not-found"),
            nodeId -> CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers().stream()
              .anyMatch(node -> node.getNodeInfo().getUniqueId().equals(nodeId)),
            () -> CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers().stream()
              .map(server -> server.getNodeInfo().getUniqueId()).collect(Collectors.toList())
          )
        )

        .prefix(exactStringIgnoreCase("push"))

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            pushTasks(sender);
            pushGroups(sender);
            pushPermissions(sender);
            pushLocalTemplates(sender);
          },
          anyStringIgnoreCase("all", "*")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> pushLocalTemplate(sender,
            (ServiceTemplate) args.argument(2)),
          anyStringIgnoreCase("local-template", "lt"),
          template("prefix/name")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> pushLocalTemplates(
            sender),
          anyStringIgnoreCase("local-templates", "lts")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> pushTasks(sender),
          anyStringIgnoreCase("tasks", "t")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> pushGroups(sender),
          anyStringIgnoreCase("groups", "g")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> pushPermissions(sender),
          anyStringIgnoreCase("local-perms", "lp")
        )

        .getSubCommands(),
      "cluster", "clu"
    );

    super.prefix = "cloudnet";
    super.permission = "cloudnet.command.cluster";
    super.description = LanguageManager.getMessage("command-description-cluster");
  }

  private static void pushLocalTemplate(ICommandSender sender, ServiceTemplate serviceTemplate) {
    TemplateStorage storage = CloudNetDriver.getInstance().getLocalTemplateStorage();
    pushLocalTemplate(sender, storage, serviceTemplate);
  }

  private static void pushLocalTemplate(ICommandSender sender, TemplateStorage storage,
    ServiceTemplate serviceTemplate) {
    String template = serviceTemplate.getStorage() + ":" + serviceTemplate.getTemplatePath();

    try {
      sender.sendMessage(
        LanguageManager.getMessage("command-cluster-push-template-compress").replace("%template%", template));
      try (InputStream inputStream = storage.zipTemplate(serviceTemplate)) {
        if (inputStream != null) {
          sender.sendMessage(LanguageManager.getMessage("command-cluster-push-template-compress-success")
            .replace("%template%", template));
          CloudNet.getInstance().deployTemplateInCluster(serviceTemplate, inputStream);

          sender.sendMessage(LanguageManager.getMessage("command-cluster-push-template-from-local-success")
            .replace("%template%", template));
        } else {
          sender.sendMessage(LanguageManager.getMessage("command-cluster-push-template-compress-failed")
            .replace("%template%", template));
        }
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  private static void pushLocalTemplates(ICommandSender sender) {
    TemplateStorage storage = CloudNetDriver.getInstance().getLocalTemplateStorage();

    for (ServiceTemplate serviceTemplate : storage.getTemplates()) {
      pushLocalTemplate(sender, storage, serviceTemplate);
    }
  }

  private static void pushPermissions(ICommandSender sender) {
    CloudNet.getInstance().publishPermissionGroupUpdates(CloudNet.getInstance().getPermissionManagement().getGroups(),
      NetworkUpdateType.SET);

    sender.sendMessage(LanguageManager.getMessage("command-cluster-push-permissions-success"));
  }

  private static void pushTasks(ICommandSender sender) {
    CloudNet.getInstance()
      .updateServiceTasksInCluster(CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks(),
        NetworkUpdateType.SET);
    sender.sendMessage(LanguageManager.getMessage("command-cluster-push-tasks-success"));
  }

  private static void pushGroups(ICommandSender sender) {
    CloudNet.getInstance().updateGroupConfigurationsInCluster(
      CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations(), NetworkUpdateType.SET);
    sender.sendMessage(LanguageManager.getMessage("command-cluster-push-groups-success"));
  }

  private static void displayNodes(ICommandSender sender) {
    StringBuilder stringBuilder = new StringBuilder();
    for (IClusterNodeServer node : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
      stringBuilder
        // line 1
        .append("Id: ")
        .append(node.getNodeInfo().getUniqueId())
        .append(node.isHeadNode() ? " (Head) " : " ")
        .append(node.isConnected() ? "&aConnected&r" : "&cNot connected&r");
      if (node.getNodeInfoSnapshot() != null) {
        stringBuilder.append("\n");

        NetworkClusterNodeInfoSnapshot snapshot = node.getNodeInfoSnapshot();
        ProcessSnapshot processSnapshot = snapshot.getProcessSnapshot();

        stringBuilder
          // cpu usage
          .append("CPU (P/S): ")
          .append(CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(processSnapshot.getCpuUsage()))
          .append("%/")
          .append(CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(snapshot.getSystemCpuUsage()))
          .append("%; ")
          // memory usage
          .append("Memory (U/R/M): ")
          .append(snapshot.getUsedMemory())
          .append("/")
          .append(snapshot.getReservedMemory())
          .append("/")
          .append(snapshot.getMaxMemory())
          .append("MB");
      }

      stringBuilder.append("\n\n");
    }

    if (stringBuilder.length() > 0) {
      sender.sendMessage(stringBuilder.substring(0, stringBuilder.length() - 1).split("\n"));
    }
  }

  private static void displayNode(ICommandSender sender, IClusterNodeServer node) {
    Preconditions.checkNotNull(node);

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
      list.add("* ClusterNodeInfoSnapshot from " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
        .format(node.getNodeInfoSnapshot().getCreationTime()));

      list.addAll(Arrays.asList(
        "CloudServices (" + node.getNodeInfoSnapshot().getCurrentServicesCount() + ") memory usage (U/R/M): "
          + node.getNodeInfoSnapshot().getUsedMemory() + "/" + node.getNodeInfoSnapshot().getReservedMemory()
          + "/" + node.getNodeInfoSnapshot().getMaxMemory() + " MB",
        " ",
        "CPU usage process: " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT
          .format(node.getNodeInfoSnapshot().getProcessSnapshot().getCpuUsage()) + "%",
        "CPU usage system: " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT
          .format(node.getNodeInfoSnapshot().getSystemCpuUsage()) + "%",
        "Threads: " + node.getNodeInfoSnapshot().getProcessSnapshot().getThreads().size(),
        "Heap usage: " + (node.getNodeInfoSnapshot().getProcessSnapshot().getHeapUsageMemory() / 1048576) + "/" +
          (node.getNodeInfoSnapshot().getProcessSnapshot().getMaxHeapMemory() / 1048576) + "MB",
        "Loaded classes: " + node.getNodeInfoSnapshot().getProcessSnapshot().getCurrentLoadedClassCount(),
        "Unloaded classes: " + node.getNodeInfoSnapshot().getProcessSnapshot().getUnloadedClassCount(),
        "Total loaded classes: " + node.getNodeInfoSnapshot().getProcessSnapshot().getTotalLoadedClassCount(),
        " ",
        "Modules: ",
        node.getNodeInfoSnapshot().getModules().stream()
          .map(module -> module.getGroup() + ":" + module.getName() + ":" + module.getVersion())
          .collect(Collectors.toList()).toString(),
        " ",
        "Properties:"
      ));

      list.addAll(Arrays.asList(node.getNodeInfoSnapshot().getProperties().toPrettyJson().split("\n")));
      list.add(" ");
    }

    sender.sendMessage(list.toArray(new String[0]));
  }

}
