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
import de.dytanic.cloudnet.service.EmptyGroupConfiguration;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorageUtil;

import java.util.*;
import java.util.stream.Collectors;

public final class CommandTasks extends CommandDefault implements ITabCompleter {

    public CommandTasks() {
        super("tasks");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "tasks list | name=<name>",
                    "tasks reload",
                    "tasks create group <name>",
                    "tasks create task <name> <" + Arrays.toString(ServiceEnvironmentType.values()) + ">",
                    "tasks delete group <name>",
                    "tasks delete task <name>",
                    "tasks group <group>",
                    "tasks group <group> add inclusion <url> <target>",
                    "tasks group <group> add template <storage> <prefix> <name>",
                    "tasks group <group> add deployment <storage> <prefix> <name> [excludes spigot.jar;logs/;plugins/]",
                    "tasks group <group> remove inclusion <url> <target>",
                    "tasks group <group> remove template <storage> <prefix> <name>",
                    "tasks group <group> remove deployment <storage> <prefix> <name>",
                    "tasks task <name>",
                    "tasks task <name> set maxHeapMemory <mb>",
                    "tasks task <name> set maintenance <true : false>",
                    "tasks task <name> set autoDeleteOnStop <true : false>",
                    "tasks task <name> set static <true : false>",
                    "tasks task <name> set startPort <port>",
                    "tasks task <name> set minServiceCount <number>",
                    "tasks task <name> set env <" + Arrays.toString(ServiceEnvironmentType.values()) + ">",
                    "tasks task <name> add group <name>",
                    "tasks task <name> remove group <name>",
                    "tasks task <name> add node <node>",
                    "tasks task <name> remove node <name>",
                    "tasks task <name> add inclusion <url> <target>",
                    "tasks task <name> add template <storage> <prefix> <name>",
                    "tasks task <name> add deployment <storage> <prefix> <name> [excludes: spigot.jar;logs;plugins]",
                    "tasks task <name> remove inclusion <url> <target>",
                    "tasks task <name> remove template <storage> <prefix> <name>",
                    "tasks task <name> remove deployment <storage> <prefix> <name>"
            );
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            getCloudServiceManager().reload();
            sender.sendMessage(LanguageManager.getMessage("command-tasks-reload-success"));
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("- Tasks", " ");

            for (ServiceTask serviceTask : this.getCloudServiceManager().getServiceTasks()) {
                if (properties.containsKey("name") &&
                        !properties.get("name").toLowerCase().contains(serviceTask.getName().toLowerCase())) {
                    continue;
                }

                sender.sendMessage(serviceTask.getName() + " | MinServiceCount: " + serviceTask.getMinServiceCount() + " | Nodes: " +
                        (serviceTask.getAssociatedNodes().isEmpty() ? "All" : serviceTask.getAssociatedNodes()) + " | StartPort: " +
                        serviceTask.getStartPort());
            }

            sender.sendMessage(" ", "- Groups", " ");

            for (GroupConfiguration groupConfiguration : this.getCloudServiceManager().getGroupConfigurations()) {
                if (properties.containsKey("name") &&
                        !properties.get("name").toLowerCase().contains(groupConfiguration.getName().toLowerCase())) {
                    continue;
                }

                sender.sendMessage("- " + groupConfiguration.getName());
            }
            return;
        }

        if (args.length <= 1) {
            return;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 3 && args[1].equalsIgnoreCase("group")) {
                if (this.createGroupConfiguration(args[2])) {
                    sender.sendMessage(LanguageManager.getMessage("command-tasks-create-group"));
                    return;
                }
            }

            if (args.length == 4 && args[1].equalsIgnoreCase("task")) {
                if (!this.getCloudServiceManager().isTaskPresent(args[2])) {
                    try {
                        ServiceEnvironmentType type = ServiceEnvironmentType.valueOf(args[3].toUpperCase());

                        this.getCloudServiceManager().addPermanentServiceTask(new ServiceTask(
                                Iterables.newArrayList(),
                                Iterables.newArrayList(Collections.singletonList(
                                        new ServiceTemplate(args[2], "default", LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE)
                                )),
                                Iterables.newArrayList(),
                                args[2],
                                "jvm",
                                true,
                                false,
                                Iterables.newArrayList(new String[]{getCloudNet().getConfig().getIdentity().getUniqueId()}),
                                Iterables.newArrayList(Collections.singletonList(args[2])),
                                new ProcessConfiguration(
                                        ServiceEnvironmentType.valueOf(args[3].toUpperCase()),
                                        type == ServiceEnvironmentType.BUNGEECORD || type == ServiceEnvironmentType.VELOCITY ||
                                                type == ServiceEnvironmentType.PROX_PROX ? 128 : 372,
                                        Iterables.newArrayList()
                                ),
                                type == ServiceEnvironmentType.BUNGEECORD || type == ServiceEnvironmentType.VELOCITY ||
                                        type == ServiceEnvironmentType.PROX_PROX ? 25565 : 44955,
                                0
                        ));

                        LocalTemplateStorageUtil.createAndPrepareTemplate(
                                CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE),
                                args[2],
                                "default",
                                ServiceEnvironmentType.valueOf(args[3].toUpperCase())
                        );

                        sender.sendMessage(LanguageManager.getMessage("command-tasks-create-task"));

                        if (this.createGroupConfiguration(args[2])) {
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-create-group"));
                            return;
                        }

                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }

            return;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length == 3) {
                if (args[1].equalsIgnoreCase("group")) {
                    getCloudServiceManager().removeGroupConfiguration(args[2]);
                    sender.sendMessage(LanguageManager.getMessage("command-tasks-delete-group"));
                    return;
                }

                if (args[1].equalsIgnoreCase("task")) {
                    getCloudServiceManager().removePermanentServiceTask(args[2]);
                    sender.sendMessage(LanguageManager.getMessage("command-tasks-delete-task"));
                    return;
                }
            }
        }

        if (args[0].equalsIgnoreCase("task")) {
            List<ServiceTask> serviceTasks = Iterables.filter(this.getCloudServiceManager().getServiceTasks(), serviceTask -> serviceTask.getName().toLowerCase().contains(args[1].toLowerCase()));

            if (serviceTasks.isEmpty()) {
                return;
            }

            ServiceTask serviceTask;

            if (serviceTasks.size() > 1) {
                serviceTask = Iterables.first(this.getCloudServiceManager().getServiceTasks(), serviceTask1 -> serviceTask1.getName().equalsIgnoreCase(args[1]));
            } else {
                serviceTask = serviceTasks.get(0);
            }

            if (serviceTask != null) {
                if (args.length == 2) {
                    this.display(sender, serviceTask);
                    return;
                }

                if (args.length > 4) {
                    if (args[2].equalsIgnoreCase("set")) {
                        switch (args[3].toLowerCase()) {
                            case "maxheapmemory":
                                if (Validate.testStringParseToInt(args[4])) {
                                    serviceTask.getProcessConfiguration().setMaxHeapMemorySize(Integer.parseInt(args[4]));
                                    this.updateServiceTask(serviceTask);
                                    this.sendMessage0(sender, serviceTask.getName(), "maxHeapMemory", serviceTask.getProcessConfiguration().getMaxHeapMemorySize());
                                }
                                break;
                            case "startport":
                                if (Validate.testStringParseToInt(args[4])) {
                                    int value = Integer.parseInt(args[4]);

                                    if (value > 1024 && value < 65535) {
                                        serviceTask.setStartPort(value);
                                        this.updateServiceTask(serviceTask);
                                        this.sendMessage0(sender, serviceTask.getName(), "startPort", serviceTask.getStartPort());
                                    }
                                }
                                break;
                            case "minservicecount":
                                if (Validate.testStringParseToInt(args[4])) {
                                    int value = Integer.parseInt(args[4]);

                                    serviceTask.setMinServiceCount(value);
                                    this.updateServiceTask(serviceTask);
                                    this.sendMessage0(sender, serviceTask.getName(), "minServiceCount", serviceTask.getMinServiceCount());
                                }
                                break;
                            case "maintenance":
                                serviceTask.setMaintenance(args[4].equalsIgnoreCase("true"));
                                this.updateServiceTask(serviceTask);
                                this.sendMessage0(sender, serviceTask.getName(), "maintenance", serviceTask.isMaintenance());
                                break;
                            case "autodeleteonstop":
                                serviceTask.setAutoDeleteOnStop(args[4].equalsIgnoreCase("true"));
                                this.updateServiceTask(serviceTask);
                                this.sendMessage0(sender, serviceTask.getName(), "autoDeleteOnStop", serviceTask.isAutoDeleteOnStop());
                                break;
                            case "static":
                                serviceTask.setStaticServices(args[4].equalsIgnoreCase("true"));
                                this.updateServiceTask(serviceTask);
                                this.sendMessage0(sender, serviceTask.getName(), "staticServices", serviceTask.isStaticServices());
                                break;
                            case "env":
                                try {
                                    serviceTask.getProcessConfiguration().setEnvironment(ServiceEnvironmentType.valueOf(args[4].toUpperCase()));
                                    this.updateServiceTask(serviceTask);
                                    this.sendMessage0(sender, serviceTask.getName(), "environment", serviceTask.getProcessConfiguration().getEnvironment());
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                                break;
                        }
                        return;
                    }

                    if (args[2].equalsIgnoreCase("add")) {
                        switch (args[3].toLowerCase()) {
                            case "group":
                                if (CloudNet.getInstance().getGroupConfigurations().stream().anyMatch(groupConfiguration -> groupConfiguration.getName().equals(args[4]))) {
                                    serviceTask.getGroups().add(args[4]);
                                    this.updateServiceTask(serviceTask);
                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-add-group-success"));
                                } else {
                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-add-group-no-group-found"));
                                }
                                break;
                            case "node":
                                serviceTask.getAssociatedNodes().add(args[4]);
                                this.updateServiceTask(serviceTask);
                                sender.sendMessage(LanguageManager.getMessage("command-tasks-add-node-success"));
                                break;
                            case "template":
                                if (args.length == 7) {
                                    if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, args[4])) {
                                        ITemplateStorage templateStorage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, args[4]);
                                        ServiceTemplate serviceTemplate = new ServiceTemplate(args[5], args[6], args[4]);

                                        if (!templateStorage.has(serviceTemplate)) {
                                            if (!templateStorage.create(serviceTemplate)) {
                                                sender.sendMessage(LanguageManager.getMessage("command-tasks-add-template-create-failed"));
                                                break;
                                            }
                                        }

                                        serviceTask.getTemplates().add(serviceTemplate);
                                        updateServiceTask(serviceTask);

                                        sender.sendMessage(LanguageManager.getMessage("command-tasks-add-template-success"));
                                    }
                                }
                                break;
                            case "deployment":
                                if (args.length > 6) {
                                    if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, args[4])) {
                                        ServiceDeployment serviceDeployment = new ServiceDeployment(new ServiceTemplate(args[5], args[6], args[4]), Iterables.newArrayList());

                                        if (args.length == 8) {
                                            serviceDeployment.getExcludes().addAll(Arrays.asList(args[7].split(";")));
                                        }

                                        serviceTask.getDeployments().add(serviceDeployment);
                                        updateServiceTask(serviceTask);

                                        sender.sendMessage(LanguageManager.getMessage("command-tasks-add-deployment-success"));
                                    }
                                }
                                break;
                            case "inclusion":
                                if (args.length == 6) {
                                    serviceTask.getIncludes().add(new ServiceRemoteInclusion(args[4], args[5]));
                                    updateServiceTask(serviceTask);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-add-inclusion-success"));
                                }
                                break;
                        }
                    }

                    if (args[2].equalsIgnoreCase("remove")) {
                        switch (args[3].toLowerCase()) {
                            case "group":
                                serviceTask.getGroups().remove(args[4]);
                                this.updateServiceTask(serviceTask);
                                sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-group-success"));
                                break;
                            case "node":
                                serviceTask.getAssociatedNodes().remove(args[4]);
                                this.updateServiceTask(serviceTask);
                                sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-node-success"));
                                break;
                            case "template":
                                if (args.length == 7) {
                                    serviceTask.getTemplates().removeAll(serviceTask.getTemplates().stream()
                                            .filter(template ->
                                                    template.getPrefix().equals(args[5]) &&
                                                            template.getName().equals(args[6]) &&
                                                            template.getStorage().equals(args[4])
                                            ).collect(Collectors.toList())
                                    );
                                    updateServiceTask(serviceTask);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-template-success"));
                                }
                                break;
                            case "deployment":
                                if (args.length == 7) {
                                    serviceTask.getDeployments().removeAll(serviceTask.getDeployments().stream()
                                            .filter(deployment ->
                                                    deployment.getTemplate().getPrefix().equals(args[5]) &&
                                                            deployment.getTemplate().getName().equals(args[6]) &&
                                                            deployment.getTemplate().getStorage().equals(args[4])
                                            ).collect(Collectors.toList())
                                    );
                                    updateServiceTask(serviceTask);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-deployment-success"));
                                }
                                break;
                            case "inclusion":
                                if (args.length == 6) {
                                    serviceTask.getIncludes().removeAll(serviceTask.getIncludes().stream()
                                            .filter(inclusion ->
                                                    inclusion.getUrl().equals(args[4]) &&
                                                            inclusion.getDestination().equals(args[5])
                                            ).collect(Collectors.toList())
                                    );
                                    updateServiceTask(serviceTask);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-inclusion-success"));
                                }
                                break;
                        }
                    }
                }

            }
            return;
        }

        if (args[0].equalsIgnoreCase("group")) {
            GroupConfiguration groupConfiguration = Iterables.first(this.getCloudServiceManager().getGroupConfigurations(), groupConfiguration1 -> groupConfiguration1.getName().toLowerCase().contains(args[1].toLowerCase()));

            if (groupConfiguration != null) {
                if (args.length == 2) {
                    this.display(sender, groupConfiguration);
                    return;
                }

                if (args.length > 5) {
                    if (args[2].equalsIgnoreCase("add")) {
                        switch (args[3].toLowerCase()) {
                            case "template":
                                if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, args[4])) {
                                    ITemplateStorage templateStorage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, args[4]);
                                    ServiceTemplate serviceTemplate = new ServiceTemplate(args[5], args[6], args[4]);

                                    if (!templateStorage.has(serviceTemplate)) {
                                        if (!templateStorage.create(serviceTemplate)) {
                                            sender.sendMessage(LanguageManager.getMessage("command-tasks-add-template-create-failed"));
                                            break;
                                        }
                                    }

                                    groupConfiguration.getTemplates().add(serviceTemplate);
                                    updateGroupConfiguration(groupConfiguration);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-add-template-success"));
                                }
                                break;
                            case "deployment":
                                if (args.length > 6) {
                                    if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, args[4])) {
                                        ServiceDeployment serviceDeployment = new ServiceDeployment(new ServiceTemplate(args[5], args[6], args[4]), Iterables.newArrayList());

                                        if (args.length == 8) {
                                            serviceDeployment.getExcludes().addAll(Arrays.asList(args[7].split(";")));
                                        }

                                        groupConfiguration.getDeployments().add(serviceDeployment);
                                        updateGroupConfiguration(groupConfiguration);

                                        sender.sendMessage(LanguageManager.getMessage("command-tasks-add-deployment-success"));
                                    }
                                }
                                break;
                            case "inclusion":
                                if (args.length == 6) {
                                    groupConfiguration.getIncludes().add(new ServiceRemoteInclusion(args[4], args[5]));
                                    updateGroupConfiguration(groupConfiguration);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-add-inclusion-success"));
                                }
                                break;
                        }
                    }

                    if (args[2].equalsIgnoreCase("remove")) {
                        switch (args[3].toLowerCase()) {
                            case "template":
                                if (args.length == 7) {
                                    groupConfiguration.getTemplates().removeAll(groupConfiguration.getTemplates().stream()
                                            .filter(template ->
                                                    template.getPrefix().equals(args[5]) &&
                                                            template.getName().equals(args[6]) &&
                                                            template.getStorage().equals(args[4])
                                            ).collect(Collectors.toList())
                                    );
                                    updateGroupConfiguration(groupConfiguration);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-template-success"));
                                }
                                break;
                            case "deployment":
                                if (args.length == 7) {
                                    groupConfiguration.getDeployments().removeAll(groupConfiguration.getDeployments().stream()
                                            .filter(deployment ->
                                                    deployment.getTemplate().getPrefix().equals(args[5]) &&
                                                            deployment.getTemplate().getName().equals(args[6]) &&
                                                            deployment.getTemplate().getStorage().equals(args[4])
                                            ).collect(Collectors.toList())
                                    );
                                    updateGroupConfiguration(groupConfiguration);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-deployment-success"));
                                }
                                break;
                            case "inclusion":
                                if (args.length == 6) {
                                    groupConfiguration.getIncludes().removeAll(groupConfiguration.getIncludes().stream()
                                            .filter(inclusion ->
                                                    inclusion.getUrl().equals(args[4]) &&
                                                            inclusion.getDestination().equals(args[5])
                                            ).collect(Collectors.toList())
                                    );
                                    updateGroupConfiguration(groupConfiguration);

                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-inclusion-success"));
                                }
                                break;
                        }
                    }
                }

            }
        }
    }

    private void display(ICommandSender sender, ServiceTask serviceTask) {
        Collection<String> list = Iterables.newArrayList();

        list.addAll(Arrays.asList(
                " ",
                "* Name: " + serviceTask.getName(),
                "* Minimal Services: " + serviceTask.getMinServiceCount(),
                "* Associated nodes: " + serviceTask.getAssociatedNodes().toString(),
                "* Groups: " + serviceTask.getGroups().toString(),
                "* Start Port: " + serviceTask.getStartPort(),
                " ",
                "- Process Configuration",
                "* Environment: " + serviceTask.getProcessConfiguration().getEnvironment(),
                "* Max HeapMemory: " + serviceTask.getProcessConfiguration().getMaxHeapMemorySize(),
                "* JVM Options: " + serviceTask.getProcessConfiguration().getJvmOptions().toString(),
                " "
        ));

        list.add("* Includes:");

        for (ServiceRemoteInclusion inclusion : serviceTask.getIncludes()) {
            list.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
        }

        list.add(" ");
        list.add("* Templates:");

        for (ServiceTemplate template : serviceTask.getTemplates()) {
            list.add("- " + template.getStorage() + ":" + template.getTemplatePath());
        }

        list.add(" ");
        list.add("* Deployments:");

        for (ServiceDeployment deployment : serviceTask.getDeployments()) {
            list.add("- ");
            list.add("Template:  " + deployment.getTemplate().getStorage() + ":" + deployment.getTemplate().getTemplatePath());
            list.add("Excludes: " + deployment.getExcludes());
        }

        list.add(" ");

        list.addAll(Arrays.asList(serviceTask.getProperties().toPrettyJson().split("\n")));
        list.add(" ");

        sender.sendMessage(list.toArray(new String[0]));
    }

    private void display(ICommandSender sender, GroupConfiguration groupConfiguration) {
        Collection<String> list = Iterables.newArrayList();

        list.addAll(Arrays.asList(
                " ",
                "* Name: " + groupConfiguration.getName(),
                " "
        ));

        list.add("* Includes:");

        for (ServiceRemoteInclusion inclusion : groupConfiguration.getIncludes()) {
            list.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
        }

        list.add(" ");
        list.add("* Templates:");

        for (ServiceTemplate template : groupConfiguration.getTemplates()) {
            list.add("- " + template.getStorage() + ":" + template.getTemplatePath());
        }

        list.add(" ");
        list.add("* Deployments:");

        for (ServiceDeployment deployment : groupConfiguration.getDeployments()) {
            list.add("- ");
            list.add("Template:  " + deployment.getTemplate().getStorage() + ":" + deployment.getTemplate().getTemplatePath());
            list.add("Excludes: " + deployment.getExcludes());
        }

        list.add(" ");

        list.addAll(Arrays.asList(groupConfiguration.getProperties().toPrettyJson().split("\n")));
        list.add(" ");

        sender.sendMessage(list.toArray(new String[0]));
    }

    private void updateServiceTask(ServiceTask serviceTask) {
        this.getCloudServiceManager().addPermanentServiceTask(serviceTask);
    }

    private void updateGroupConfiguration(GroupConfiguration groupConfiguration) {
        this.getCloudServiceManager().addGroupConfiguration(groupConfiguration);
    }

    private void sendMessage0(ICommandSender sender, String name, String property, Object value) {
        sender.sendMessage(
                LanguageManager.getMessage("command-tasks-set-property-success")
                        .replace("%name%", name)
                        .replace("%property%", property)
                        .replace("%value%", value.toString())
        );
    }

    private ICloudServiceManager getCloudServiceManager() {
        return getCloudNet().getCloudServiceManager();
    }

    private boolean createGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        boolean value = false;

        if (!this.getCloudServiceManager().isGroupConfigurationPresent(name)) {
            this.getCloudServiceManager().addGroupConfiguration(new EmptyGroupConfiguration(name));
            value = true;
        }

        return value;
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        if (args.length == 1) {
            return Arrays.asList("group", "task", "delete", "create", "reload", "list");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("delete")) {
                return Arrays.asList("group", "task");
            }
            if (args[0].equalsIgnoreCase("group")) {
                return getCloudNet().getGroupConfigurations().stream().map(GroupConfiguration::getName).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("task")) {
                return getCloudNet().getPermanentServiceTasks().stream().map(ServiceTask::getName).collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("delete")) {
                if (args[1].equalsIgnoreCase("group")) {
                    return getCloudNet().getGroupConfigurations().stream().map(GroupConfiguration::getName).collect(Collectors.toList());
                }
                if (args[1].equalsIgnoreCase("task")) {
                    return getCloudNet().getPermanentServiceTasks().stream().map(ServiceTask::getName).collect(Collectors.toList());
                }
            }
            if (args[0].equalsIgnoreCase("group")) {
                return Collections.singletonList("add");
            }
            if (args[0].equalsIgnoreCase("task")) {
                return Arrays.asList("add", "set", "remove");
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("create")) {
                if (args[1].equalsIgnoreCase("task")) {
                    return Arrays.stream(ServiceEnvironmentType.values()).map(Objects::toString).collect(Collectors.toList());
                }
            }
            if (args[0].equalsIgnoreCase("group")) {
                if (args[2].equalsIgnoreCase("add")) {
                    return Arrays.asList("inclusion", "template", "deployment");
                }
            }
            if (args[0].equalsIgnoreCase("task")) {
                if (args[2].equalsIgnoreCase("set")) {
                    return Arrays.asList("maxHeapMemory", "maintenance", "autoDeleteOnStop", "static", "startPort", "minServiceCount", "env");
                }
                if (args[2].equalsIgnoreCase("add")) {
                    return Arrays.asList("group", "node", "inclusion", "template", "deployment");
                }
                if (args[2].equalsIgnoreCase("remove")) {
                    return Arrays.asList("group", "node");
                }
            }
        }

        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("task")) {
                if (args[2].equalsIgnoreCase("set")) {
                    if (args[3].equalsIgnoreCase("maxHeapMemory")) {
                        return Arrays.asList("128", "256", "512", "1024", "2048", "4096", "8192", "16384");
                    }
                    if (args[3].equalsIgnoreCase("maintenance")) {
                        return Arrays.asList("true", "false");
                    }
                    if (args[3].equalsIgnoreCase("autoDeleteOnStop")) {
                        return Arrays.asList("true", "false");
                    }
                    if (args[3].equalsIgnoreCase("static")) {
                        return Arrays.asList("true", "false");
                    }
                    if (args[3].equalsIgnoreCase("env")) {
                        return Arrays.stream(ServiceEnvironmentType.values()).map(Objects::toString).collect(Collectors.toList());
                    }
                }
                if (args[2].equalsIgnoreCase("remove")) {
                    if (args[3].equalsIgnoreCase("group")) {
                        ServiceTask serviceTask = getCloudNet().getServiceTask(args[1]);
                        if (serviceTask == null)
                            return null;
                        return serviceTask.getGroups();
                    }
                    if (args[3].equalsIgnoreCase("node")) {
                        ServiceTask serviceTask = getCloudNet().getServiceTask(args[1]);
                        if (serviceTask == null)
                            return null;
                        return serviceTask.getAssociatedNodes();
                    }
                }
            }
        }

        return null;
    }
}
