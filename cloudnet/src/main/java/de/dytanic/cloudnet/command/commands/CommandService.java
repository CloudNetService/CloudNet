package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.WildcardUtil;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

public class CommandService extends SubCommandHandler {
    public CommandService() {
        super(
                SubCommandBuilder.create()

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
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
                                },
                                subCommand -> subCommand.enableProperties().appendUsage("| id=<text> | task=<text> | group=<text> | --names"),
                                anyStringIgnoreCase("list", "l")
                        )


                        .prefix(dynamicString(
                                "name",
                                LanguageManager.getMessage("command-service-service-not-found"),
                                input -> !WildcardUtil.filterWildcard(CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(), input).isEmpty(),
                                () -> CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
                                        .map(ServiceInfoSnapshot::getName)
                                        .collect(Collectors.toList())
                        ))
                        .preExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            Collection<ServiceInfoSnapshot> serviceInfoSnapshots = WildcardUtil.filterWildcard(
                                    CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(),
                                    (String) args.argument("name").get()
                            );
                            internalProperties.put("services", serviceInfoSnapshots);
                        })


                        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> display(sender, serviceInfoSnapshot, false)))
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> display(sender, serviceInfoSnapshot, true)),
                                anyStringIgnoreCase("info", "i")
                        )

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().start()),
                                exactStringIgnoreCase("start")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    if (properties.containsKey("--force")) {
                                        forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().kill());
                                    } else {
                                        forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().stop());
                                    }
                                },
                                subCommand -> subCommand.enableProperties().appendUsage("| --force"),
                                anyStringIgnoreCase("stop", "shutdown")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().delete()),
                                anyStringIgnoreCase("delete", "del")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().includeWaitingServiceInclusions()),
                                exactStringIgnoreCase("includeInclusions")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().includeWaitingServiceTemplates()),
                                exactStringIgnoreCase("includeTemplates")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().deployResources()),
                                exactStringIgnoreCase("deployResources")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().restart()),
                                exactStringIgnoreCase("restart")
                        )

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
                                        forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().runCommand((String) args.argument("command").get())),
                                subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length).setMaxArgs(Integer.MAX_VALUE),
                                anyStringIgnoreCase("command", "cmd"),
                                dynamicString("command")
                        )

                        .prefix(exactStringIgnoreCase("add"))
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> {
                                    ServiceTemplate template = (ServiceTemplate) args.argument("storage:prefix/name").get();
                                    Collection<String> excludes = (Collection<String>) args.argument("excludedFiles separated by \";\"").orElse(new ArrayList<>());

                                    serviceInfoSnapshot.provider().addServiceDeployment(new ServiceDeployment(template, excludes));

                                    sender.sendMessage(LanguageManager.getMessage("command-service-add-deployment-success"));
                                }),
                                subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length - 1).setMaxArgs(Integer.MAX_VALUE),
                                exactStringIgnoreCase("deployment"),
                                template("storage:prefix/name"),
                                collection("excludedFiles separated by \";\"")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> {
                                    ServiceTemplate template = (ServiceTemplate) args.argument("storage:prefix/name").get();

                                    serviceInfoSnapshot.provider().addServiceTemplate(template);

                                    sender.sendMessage(LanguageManager.getMessage("command-service-add-template-success"));
                                }),
                                exactStringIgnoreCase("template"),
                                template("storage:prefix/name")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(internalProperties, serviceInfoSnapshot -> {
                                    String url = (String) args.argument("url").get();
                                    String target = (String) args.argument("targetPath").get();

                                    serviceInfoSnapshot.provider().addServiceRemoteInclusion(new ServiceRemoteInclusion(url, target));

                                    sender.sendMessage(LanguageManager.getMessage("command-service-add-inclusion-success"));
                                }),
                                exactStringIgnoreCase("inclusion"),
                                url("url"),
                                dynamicString("targetPath")
                        )

                        .getSubCommands(),
                "service", "ser"
        );
        super.prefix = "cloudnet";
        super.permission = "cloudnet.command." + super.names[0];
        super.description = LanguageManager.getMessage("command-description-service");
    }


    private static void forEachService(Map<String, Object> internalProperties, Consumer<ServiceInfoSnapshot> consumer) {
        for (Object serviceInfoSnapshot : ((Collection<?>) internalProperties.get("services"))) {
            if (serviceInfoSnapshot instanceof ServiceInfoSnapshot) {
                consumer.accept((ServiceInfoSnapshot) serviceInfoSnapshot);
            }
        }
    }

    private static void display(ICommandSender sender, ServiceInfoSnapshot serviceInfoSnapshot, boolean full) {
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
                "PID: " + serviceInfoSnapshot.getProcessSnapshot().getPid(),
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

}
