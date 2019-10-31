package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class CommandCluster extends CommandDefault implements ITabCompleter {

    public CommandCluster() {
        super("cluster", "clu");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    " ",
                    "ClusterId: " + getCloudNet().getConfig().getClusterConfig().getClusterId(),
                    " ",
                    "cluster shutdown",
                    "cluster add <nodeId> <host> <port>",
                    "cluster nodes | id=<id>",
                    "cluster push local-templates",
                    "cluster push tasks",
                    "cluster push groups",
                    "cluster push local-perms"
            );
            return;
        }

        if (args[0].toLowerCase().contains("push")) {
            if (args.length == 2) {
                if (args[1].equalsIgnoreCase("local-templates")) {
                    ITemplateStorage storage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

                    byte[] bytes;
                    for (ServiceTemplate serviceTemplate : storage.getTemplates()) {
                        bytes = storage.toZipByteArray(serviceTemplate);
                        if (bytes != null) {
                            getCloudNet().deployTemplateInCluster(serviceTemplate, bytes);
                        }

                        sender.sendMessage(
                                LanguageManager.getMessage("command-cluster-push-templates-from-local-success")
                                        .replace("%template%", serviceTemplate.getStorage() + ":" + serviceTemplate.getTemplatePath())
                        );
                    }
                    return;
                }

                if (args[1].equalsIgnoreCase("local-perms")) {
                    getCloudNet().publishPermissionGroupUpdates(getCloudNet().getPermissionManagement().getGroups(), NetworkUpdateType.SET);

                    sender.sendMessage(LanguageManager.getMessage("command-cluster-push-permissions-success"));
                }

                if (args[1].equalsIgnoreCase("tasks")) {
                    getCloudNet().updateServiceTasksInCluster(getCloudNet().getPermanentServiceTasks(), NetworkUpdateType.SET);
                    sender.sendMessage(LanguageManager.getMessage("command-cluster-push-tasks-success"));
                    return;
                }

                if (args[1].equalsIgnoreCase("groups")) {
                    getCloudNet().updateGroupConfigurationsInCluster(getCloudNet().getGroupConfigurations(), NetworkUpdateType.SET);
                    sender.sendMessage(LanguageManager.getMessage("command-cluster-push-groups-success"));
                    return;
                }
            }
            return;
        }

        if (args[0].toLowerCase().contains("shutdown")) {
            for (IClusterNodeServer node : getCloudNet().getClusterNodeServerProvider().getNodeServers()) {
                node.sendCommandLine("stop");
            }

            getCloudNet().getCommandMap().dispatchCommand(sender, "stop");
            return;
        }

        if (args[0].toLowerCase().contains("nodes")) {
            for (IClusterNodeServer node : getCloudNet().getClusterNodeServerProvider().getNodeServers()) {
                if (properties.containsKey("id") && !node.getNodeInfo().getUniqueId().contains(properties.get("id"))) {
                    continue;
                }

                this.displayNode(sender, node);
            }
            return;
        }

        if (args[0].toLowerCase().contains("add") && args.length == 4 && Validate.testStringParseToInt(args[3])) {
            NetworkCluster networkCluster = getCloudNet().getConfig().getClusterConfig();
            networkCluster.getNodes().add(new NetworkClusterNode(
                    args[1],
                    new HostAndPort[]{
                            new HostAndPort(args[2], Integer.parseInt(args[3]))
                    }
            ));
            getCloudNet().getConfig().getIpWhitelist().add(args[2]); //setClusterConfig already saves, so we don't have to call the save method again to save the ipWhitelist
            getCloudNet().getConfig().setClusterConfig(networkCluster);
            getCloudNet().getClusterNodeServerProvider().setClusterServers(networkCluster);

            sender.sendMessage(LanguageManager.getMessage("command-cluster-create-node-success"));
        }
    }

    private void displayNode(ICommandSender sender, IClusterNodeServer node) {
        Validate.checkNotNull(node);

        List<String> list = Iterables.newArrayList();

        list.addAll(Arrays.asList(
                " ",
                "Id: " + node.getNodeInfo().getUniqueId(),
                "State: " + (node.isConnected() ? "Connected" : "Not connected"),
                " ",
                "Address: "
        ));

        for (HostAndPort hostAndPort : node.getNodeInfo().getListeners()) {
            list.add("- " + hostAndPort.getHost() + ":" + hostAndPort.getPort());
        }

        if (node.getNodeInfoSnapshot() != null) {
            list.add(" ");
            list.add("* ClusterNodeInfoSnapshot from " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(node.getNodeInfoSnapshot().getCreationTime()));

            list.addAll(Arrays.asList(
                    "CloudServices (" + node.getNodeInfoSnapshot().getCurrentServicesCount() + ") memory usage " +
                            node.getNodeInfoSnapshot().getUsedMemory() + "/" + node.getNodeInfoSnapshot().getReservedMemory() + "/" + node.getNodeInfoSnapshot().getMaxMemory() + "MB",
                    " ",
                    "CPU usage process: " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(node.getNodeInfoSnapshot().getProcessSnapshot().getCpuUsage()) + "%",
                    "CPU usage system: " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(node.getNodeInfoSnapshot().getSystemCpuUsage()) + "%",
                    "Threads: " + node.getNodeInfoSnapshot().getProcessSnapshot().getThreads().size(),
                    "Heap usage: " + (node.getNodeInfoSnapshot().getProcessSnapshot().getHeapUsageMemory() / 1048576) + "/" +
                            (node.getNodeInfoSnapshot().getProcessSnapshot().getMaxHeapMemory() / 1048576) + "MB",
                    "Loaded classes: " + node.getNodeInfoSnapshot().getProcessSnapshot().getCurrentLoadedClassCount(),
                    "Unloaded classes: " + node.getNodeInfoSnapshot().getProcessSnapshot().getUnloadedClassCount(),
                    "Total loaded classes: " + node.getNodeInfoSnapshot().getProcessSnapshot().getTotalLoadedClassCount(),
                    " ",
                    "Extensions: ",
                    Iterables.map(node.getNodeInfoSnapshot().getExtensions(), networkClusterNodeExtensionSnapshot -> networkClusterNodeExtensionSnapshot.getGroup() + ":" +
                            networkClusterNodeExtensionSnapshot.getName() + ":" +
                            networkClusterNodeExtensionSnapshot.getVersion()).toString(),
                    " ",
                    "Properties:"
            ));

            list.addAll(Arrays.asList(node.getNodeInfoSnapshot().getProperties().toPrettyJson().split("\n")));
            list.add(" ");
        }

        sender.sendMessage(list.toArray(new String[0]));
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        return args.length == 1 ? Arrays.asList(
                "shutdown",
                "add",
                "nodes",
                "push"
        ) :
                args.length == 2 ?
                        args[0].equalsIgnoreCase("push") ?
                                Arrays.asList(
                                        "local-templates",
                                        "tasks",
                                        "groups",
                                        "local-perms"
                                ) : null : null;
    }
}