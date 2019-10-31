package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.template.ITemplateStorage;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandService extends CommandDefault implements ITabCompleter {

    public CommandService() {
        super("service", "services", "serv", "ser");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "service list | id=<text> | task=<text> | group=<text> | --names",
                    "service foreach | task=<text> | id=<text> | group=<text> ",
                    "--start | --stop | --delete | --restart | --includeInclusions | --includeTemplates | --deployResources",
                    "service <uniqueId | name>",
                    "service <uniqueId | name> info",
                    "service <uniqueId | name> start",
                    "service <uniqueId | name> stop | --force",
                    "service <uniqueId | name> delete",
                    "service <uniqueId | name> restart",
                    "service <uniqueId | name> command <command>",
                    "service <uniqueId | name> includeInclusions",
                    "service <uniqueId | name> includeTemplates",
                    "service <uniqueId | name> deployResources",
                    "service <uniqueId | name> add inclusion <url> <target>",
                    "service <uniqueId | name> add template <storage> <prefix> <name>",
                    "service <uniqueId | name> add deployment <storage> <prefix> <name> [excludes spigot.jar;logs/;plugins/]"
            );
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {

            Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
                    .filter(serviceInfoSnapshot -> !properties.containsKey("id")
                            || serviceInfoSnapshot.getServiceId().getUniqueId().toString().toLowerCase().contains(properties.get("id").toLowerCase()))
                    .filter(serviceInfoSnapshot -> !properties.containsKey("group")
                            || Iterables.contains(properties.get("group"), serviceInfoSnapshot.getConfiguration().getGroups()))
                    .filter(serviceInfoSnapshot -> !properties.containsKey("task")
                            || properties.get("task").toLowerCase().contains(serviceInfoSnapshot.getServiceId().getTaskName().toLowerCase()))
                    .collect(Collectors.toSet());

            for (ServiceInfoSnapshot serviceInfoSnapshot : targetServiceInfoSnapshots) {
                if (!properties.containsKey("names")) {
                    sender.sendMessage(
                            serviceInfoSnapshot.getServiceId().getUniqueId().toString().split("-")[0] +
                                    " | Name: " + serviceInfoSnapshot.getServiceId().getName() +
                                    " | Node: " + serviceInfoSnapshot.getServiceId().getNodeUniqueId() +
                                    " | Status: " + serviceInfoSnapshot.getLifeCycle() +
                                    " | Address: " + serviceInfoSnapshot.getAddress().getHost() + ":" +
                                    serviceInfoSnapshot.getAddress().getPort() +
                                    " | " + (serviceInfoSnapshot.isConnected() ? "Connected" : "Not Connected")
                    );
                } else {
                    sender.sendMessage(serviceInfoSnapshot.getServiceId().getTaskName() + "-" + serviceInfoSnapshot.getServiceId().getTaskServiceId() +
                            " | " + serviceInfoSnapshot.getServiceId().getUniqueId());
                }
            }
            sender.sendMessage(String.format("=> Showing %d service(s)", targetServiceInfoSnapshots.size()));

            return;
        }

        if (args[0].equalsIgnoreCase("foreach")) {
            for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices()) {
                if (properties.containsKey("id") &&
                        !serviceInfoSnapshot.getServiceId().getUniqueId().toString()
                                .toLowerCase().contains(properties.get("id").toLowerCase())) {
                    continue;
                }

                if (properties.containsKey("group") &&
                        !Iterables.contains(properties.get("group"), serviceInfoSnapshot.getConfiguration().getGroups())) {
                    continue;
                }

                if (properties.containsKey("task") &&
                        !properties.get("task").toLowerCase().contains(
                                serviceInfoSnapshot.getServiceId().getTaskName().toLowerCase())) {
                    continue;
                }

                if (properties.containsKey("delete")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).delete();
                    continue;
                }

                if (properties.containsKey("restart")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).restart();
                    continue;
                }

                if (properties.containsKey("includeInclusions")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).includeWaitingServiceInclusions();
                    continue;
                }

                if (properties.containsKey("includeTemplates")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).includeWaitingServiceTemplates();
                    continue;
                }

                if (properties.containsKey("deployResources")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).deployResources();
                    continue;
                }

                if (properties.containsKey("start")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).start();
                    continue;
                }

                if (properties.containsKey("stop")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).stop();
                }
            }
        }

        ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceByNameOrUniqueId(args[0]);

        //Handle service
        if (serviceInfoSnapshot != null) {
            if (args.length == 1) {
                this.display(sender, serviceInfoSnapshot, false);
                return;
            }

            if (args.length > 4) {
                if (args[1].equalsIgnoreCase("add")) {
                    switch (args[2]) {
                        case "template":
                            if (args.length == 6) {
                                if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, args[3])) {
                                    ServiceTemplate serviceTemplate = new ServiceTemplate(args[4], args[5], args[3]);

                                    if (CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, args[3]).has(serviceTemplate)) {
                                        CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).addServiceTemplate(serviceTemplate);

                                        sender.sendMessage(LanguageManager.getMessage("command-service-add-template-success"));
                                    }
                                }
                            }
                            break;
                        case "deployment":
                            if (args.length > 5) {
                                if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, args[3])) {
                                    ServiceDeployment serviceDeployment = new ServiceDeployment(new ServiceTemplate(args[4], args[5], args[3]), Iterables.newArrayList());

                                    if (args.length == 7) {
                                        serviceDeployment.getExcludes().addAll(Arrays.asList(args[6].split(";")));
                                    }

                                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).addServiceDeployment(serviceDeployment);

                                    sender.sendMessage(LanguageManager.getMessage("command-service-add-deployment-success"));
                                }
                            }
                            break;
                        case "inclusion":
                            if (args.length == 5) {
                                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).addServiceRemoteInclusion(new ServiceRemoteInclusion(args[3], args[4]));

                                sender.sendMessage(LanguageManager.getMessage("command-service-add-inclusion-success"));
                            }
                            break;
                    }

                    return;
                }
            }

            if (args.length > 2 && args[1].equalsIgnoreCase("command")) {
                StringBuilder stringBuilder = new StringBuilder();

                for (int i = 2; i < args.length; i++) {
                    stringBuilder.append(args[i]).append(" ");
                }

                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).runCommand(stringBuilder.substring(0, stringBuilder.length() - 1));
                return;
            }

            if (args[1].equalsIgnoreCase("start")) {
                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).start();
                return;
            }

            if (args[1].equalsIgnoreCase("stop")) {
                if (!properties.containsKey("--force")) {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).stop();
                } else {
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).kill();
                }

                return;
            }

            if (args[1].equalsIgnoreCase("delete")) {
                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).delete();
                return;
            }

            if (args[1].equalsIgnoreCase("includeInclusions")) {
                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).includeWaitingServiceInclusions();
                return;
            }

            if (args[1].equalsIgnoreCase("includeTemplates")) {
                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).includeWaitingServiceTemplates();
                return;
            }

            if (args[1].equalsIgnoreCase("deployResources")) {
                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).deployResources();
                return;
            }

            if (args[1].equalsIgnoreCase("restart")) {
                CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).restart();
                return;
            }

            if (args[1].equalsIgnoreCase("info")) {
                this.display(sender, serviceInfoSnapshot, true);
            }
        }
    }

    private void display(ICommandSender sender, ServiceInfoSnapshot serviceInfoSnapshot, boolean full) {
        Collection<String> list = Iterables.newArrayList();

        list.addAll(Arrays.asList(
                " ",
                "* CloudService: " + serviceInfoSnapshot.getServiceId().getUniqueId().toString(),
                "* Name: " + serviceInfoSnapshot.getServiceId().getTaskName() + "-" + serviceInfoSnapshot.getServiceId().getTaskServiceId(),
                "* Port: " + serviceInfoSnapshot.getConfiguration().getPort(),
                "* Connected: " + serviceInfoSnapshot.isConnected(),
                "* Lifecycle: " + serviceInfoSnapshot.getLifeCycle(),
                "* Groups: " + Arrays.toString(serviceInfoSnapshot.getConfiguration().getGroups()),
                " "
        ));

        list.add("* Includes:");

        for (ServiceRemoteInclusion inclusion : serviceInfoSnapshot.getConfiguration().getIncludes()) {
            list.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
        }

        list.add(" ");
        list.add("* Templates:");

        for (ServiceTemplate template : serviceInfoSnapshot.getConfiguration().getTemplates()) {
            list.add("- " + template.getStorage() + ":" + template.getTemplatePath());
        }

        list.add(" ");
        list.add("* Deployments:");

        for (ServiceDeployment deployment : serviceInfoSnapshot.getConfiguration().getDeployments()) {
            list.add("- ");
            list.add("Template:  " + deployment.getTemplate().getStorage() + ":" + deployment.getTemplate().getTemplatePath());
            list.add("Excludes: " + deployment.getExcludes());
        }

        list.add(" ");
        list.add("* ServiceInfoSnapshot | " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(serviceInfoSnapshot.getCreationTime()));

        list.addAll(Arrays.asList(
                "CPU usage: " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(serviceInfoSnapshot.getProcessSnapshot().getCpuUsage()) + "%",
                "Threads: " + serviceInfoSnapshot.getProcessSnapshot().getThreads().size(),
                "Heap usage: " + (serviceInfoSnapshot.getProcessSnapshot().getHeapUsageMemory() / 1048576) + "/" +
                        (serviceInfoSnapshot.getProcessSnapshot().getMaxHeapMemory() / 1048576) + "MB",
                "Loaded classes: " + serviceInfoSnapshot.getProcessSnapshot().getCurrentLoadedClassCount(),
                "Unloaded classes: " + serviceInfoSnapshot.getProcessSnapshot().getUnloadedClassCount(),
                "Total loaded classes: " + serviceInfoSnapshot.getProcessSnapshot().getTotalLoadedClassCount(),
                " "
        ));

        if (full) {
            list.add("Properties:");
            list.addAll(Arrays.asList(serviceInfoSnapshot.getProperties().toPrettyJson().split("\n")));
            list.add(" ");
        }

        sender.sendMessage(list.toArray(new String[0]));
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        return CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
                .map(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getName())
                .collect(Collectors.toList());
    }
}