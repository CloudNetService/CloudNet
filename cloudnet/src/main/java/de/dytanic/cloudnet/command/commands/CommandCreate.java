package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Arrays;
import java.util.Collection;

public final class CommandCreate extends CommandDefault implements ITabCompleter {

    public CommandCreate() {
        super("create");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "create by <task> <count>",
                    "create new <name> <count> <" + Arrays.toString(ServiceEnvironmentType.values()) + ">",
                    " ",
                    "parameters: ",
                    "- task=<name>",
                    "- node=[Node-1;Node-2]",
                    "- autoDeleteOnStop=<true : false>",
                    "- static=<true : false>",
                    "- port=<port>",
                    "- memory=<mb>",
                    "- groups=[Lobby, Prime, TestLobby]",
                    "- runtime=<name>",
                    "- jvmOptions=[-XX:OptimizeStringConcat;-Xms256M]",
                    "- templates=[storage:prefix/name  local:Lobby/Lobby;local:/PremiumLobby]",
                    "- deployments=[storage:prefix/name  local:Lobby/Lobby;local:/PremiumLobby]",
                    "- --start"
            );
            return;
        }

        if (args[0].equalsIgnoreCase("by") && args.length > 2 && Validate.testStringParseToInt(args[2])) {
            ServiceTask serviceTask = Iterables.first(CloudNetDriver.getInstance().getPermanentServiceTasks(), serviceTask1 -> serviceTask1.getName().equalsIgnoreCase(args[1]));

            if (serviceTask != null) {
                int count = Integer.parseInt(args[2]);

                CloudNet.getInstance().getTaskScheduler().schedule(() -> {
                    sender.sendMessage(LanguageManager.getMessage("command-create-start"));
                    Collection<ServiceInfoSnapshot> serviceInfoSnapshots = runCloudService(
                            properties,
                            count,
                            serviceTask.getName(),
                            serviceTask.getRuntime(),
                            serviceTask.isAutoDeleteOnStop(),
                            serviceTask.isStaticServices(),
                            serviceTask.getAssociatedNodes(),
                            serviceTask.getIncludes(),
                            serviceTask.getTemplates(),
                            serviceTask.getDeployments(),
                            serviceTask.getGroups(),
                            serviceTask.getDeletedFilesAfterStop(),
                            serviceTask.getProcessConfiguration(),
                            serviceTask.getStartPort()
                    );

                    if (serviceInfoSnapshots.isEmpty()) {
                        sender.sendMessage(LanguageManager.getMessage("command-create-by-task-failed"));
                        return;
                    }
                    this.listAndStartServices(sender, serviceInfoSnapshots, properties);

                    sender.sendMessage(LanguageManager.getMessage("command-create-by-task-success"));
                });
            }
            return;
        }

        if (args[0].equalsIgnoreCase("new") && args.length > 3 && Validate.testStringParseToInt(args[2])) {
            ServiceEnvironmentType environmentType;

            try {
                environmentType = ServiceEnvironmentType.valueOf(args[3].toUpperCase());
            } catch (Throwable ex) {
                return;
            }

            CloudNet.getInstance().getTaskScheduler().schedule(() -> {
                sender.sendMessage(LanguageManager.getMessage("command-create-start"));
                try {
                    Collection<ServiceInfoSnapshot> serviceInfoSnapshots = this.runCloudService(
                            properties,
                            Integer.parseInt(args[2]),
                            args[1],
                            null,
                            false,
                            false,
                            Iterables.newArrayList(),
                            Iterables.newArrayList(),
                            Iterables.newArrayList(),
                            Iterables.newArrayList(),
                            Iterables.newArrayList(),
                            Iterables.newArrayList(),
                            new ProcessConfiguration(
                                    environmentType,
                                    372,
                                    Iterables.newArrayList()
                            ),
                            46949
                    );

                    this.listAndStartServices(sender, serviceInfoSnapshots, properties);

                    sender.sendMessage(LanguageManager.getMessage("command-create-new-service-success"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    private void listAndStartServices(ICommandSender sender, Collection<ServiceInfoSnapshot> serviceInfoSnapshots, Properties properties) {
        for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
            if (serviceInfoSnapshot != null) {
                sender.sendMessage(serviceInfoSnapshot.getServiceId().getName() + " - " + serviceInfoSnapshot.getServiceId().getUniqueId().toString());
            }
        }

        if (properties.containsKey("start")) {
            for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
                CloudNetDriver.getInstance().setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.RUNNING);
            }
        }
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        return Arrays.asList(
                "name",
                "task",
                "port",
                "templates",
                "deployments",
                "autoDeleteOnStop",
                "memory",
                "jvmOptions",
                "groups",
                "--start"
        );
    }


    private Collection<ServiceInfoSnapshot> runCloudService(
            Properties properties,
            int count,
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticServices,
            Collection<String> nodes,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            Collection<String> deletedFilesAfterStop,
            ProcessConfiguration processConfiguration,
            int startPort
    ) {
        Collection<ServiceInfoSnapshot> serviceInfoSnapshots = Iterables.newArrayList(count);
        Collection<ServiceTemplate> temps = Iterables.newArrayList();
        Collection<ServiceDeployment> deploy = Iterables.newArrayList();

        if (properties.containsKey("templates")) {
            String[] templateEntries = properties.get("templates").split(";");

            for (String item : templateEntries) {
                if (item.length() > 3 && item.contains(":") && item.contains("/")) {
                    String[] split = item.split(":");

                    if (split.length == 2 && split[1].contains("/")) {
                        String[] split0 = split[1].split("/");

                        temps.add(new ServiceTemplate(
                                split0[0],
                                split0[1],
                                split[0]
                        ));
                    }
                }
            }
        } else {
            temps.addAll(templates);
        }

        if (properties.containsKey("deployments")) {
            String[] templateEntries = properties.get("deployments").split(";");

            for (String item : templateEntries) {
                if (item.length() > 3 && item.contains(":") && item.contains("/")) {
                    String[] split = item.split(":");

                    if (split.length == 2 && split[1].contains("/")) {
                        String[] split0 = split[1].split("/");

                        deploy.add(
                                new ServiceDeployment(new ServiceTemplate(
                                        split0[0],
                                        split0[1],
                                        split[0]
                                ), Iterables.newArrayList()));
                    }
                }
            }
        } else {
            deploy.addAll(deployments);
        }

        for (int i = 0; i < count; i++) {
            ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().createCloudService(new ServiceTask(
                    includes,
                    temps,
                    deploy,
                    properties.getOrDefault("name", name),
                    properties.getOrDefault("runtime", runtime),
                    properties.getOrDefault("autoDeleteOnStop", String.valueOf(autoDeleteOnStop)).equalsIgnoreCase("true"),
                    properties.getOrDefault("static", String.valueOf(staticServices)).equalsIgnoreCase("true"),
                    properties.containsKey("node") ? Arrays.asList(properties.get("node").split(";")) : nodes,
                    properties.containsKey("groups") ? Arrays.asList(properties.get("groups").split(";")) : groups,
                    properties.containsKey("deletedFilesAfterStop") ? Arrays.asList(properties.get("deletedFilesAfterStop").split(";")) : deletedFilesAfterStop,
                    new ProcessConfiguration(
                            processConfiguration.getEnvironment(),
                            properties.containsKey("memory") && Validate.testStringParseToInt(properties.get("memory")) ?
                                    Integer.parseInt(properties.get("memory")) : processConfiguration.getMaxHeapMemorySize(),
                            properties.containsKey("jvmOptions") ?
                                    Arrays.asList(properties.get("jvmOptions").split(";")) :
                                    processConfiguration.getJvmOptions()
                    ),
                    Validate.testStringParseToInt(properties.getOrDefault("port", String.valueOf(startPort))) ?
                            Integer.parseInt(properties.getOrDefault("port", String.valueOf(startPort)))
                            :
                            46949,
                    0
            ));

            if (serviceInfoSnapshot != null) {
                serviceInfoSnapshots.add(serviceInfoSnapshot);
            }
        }

        return serviceInfoSnapshots;
    }
}
