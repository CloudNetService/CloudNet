package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.DefaultLogFormatter;
import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.*;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.service.EmptyGroupConfiguration;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArguments.*;

public class CommandTasks extends SubCommandHandler {
    public CommandTasks() {
        super(
                SubCommandBuilder.create()

                        .generateCommand(
                                (sender, command, args, commandLine, properties, internalProperties) -> setupTask(CloudNet.getInstance().getConsole(), sender),
                                SubCommand::onlyConsole,
                                exactStringIgnoreCase("setup")
                        )
                        .generateCommand((sender, command, args, commandLine, properties, internalProperties) -> {
                            CloudNet.getInstance().getCloudServiceManager().reload();
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-reload-success"));
                        }, exactStringIgnoreCase("reload"))
                        .generateCommand((sender, command, args, commandLine, properties, internalProperties) -> {
                            sender.sendMessage("- Tasks", " ");

                            for (ServiceTask serviceTask : CloudNet.getInstance().getCloudServiceManager().getServiceTasks()) {
                                if (properties.containsKey("name") &&
                                        !serviceTask.getName().toLowerCase().contains(properties.get("name").toLowerCase())) {
                                    continue;
                                }

                                sender.sendMessage(serviceTask.getName() + " | MinServiceCount: " + serviceTask.getMinServiceCount() + " | Nodes: " +
                                        (serviceTask.getAssociatedNodes().isEmpty() ? "All" : serviceTask.getAssociatedNodes()) + " | StartPort: " +
                                        serviceTask.getStartPort());
                            }

                            sender.sendMessage(" ", "- Groups", " ");

                            for (GroupConfiguration groupConfiguration : CloudNet.getInstance().getCloudServiceManager().getGroupConfigurations()) {
                                if (properties.containsKey("name") &&
                                        !groupConfiguration.getName().toLowerCase().contains(properties.get("name").toLowerCase())) {
                                    continue;
                                }

                                sender.sendMessage("- " + groupConfiguration.getName());
                            }
                        }, subCommand -> subCommand.setMinArgs(1).setMaxArgs(Integer.MAX_VALUE), exactStringIgnoreCase("list"))

                        .applyHandler(CommandTasks::handleCreateCommands)
                        .applyHandler(CommandTasks::handleDeleteCommands)

                        .prefix(exactStringIgnoreCase("task"))
                        .prefix(dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-task-not-found"),
                                name -> CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(name),
                                () -> CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()
                                        .stream()
                                        .map(ServiceTask::getName)
                                        .collect(Collectors.toList())
                        ))

                        .preExecute(
                                (sender, command, args, commandLine, properties, internalProperties) ->
                                        internalProperties.put("task", CloudNet.getInstance().getServiceTaskProvider().getServiceTask((String) args[1]))
                        )

                        .generateCommand((sender, command, args, commandLine, properties, internalProperties) -> displayTask(sender, (ServiceTask) internalProperties.get("task")))

                        .prefix(exactStringIgnoreCase("set"))
                        .applyHandler(CommandTasks::handleTaskSetCommands)
                        .removeLastPrefix()

                        .prefix(exactStringIgnoreCase("add"))
                        .applyHandler(CommandTasks::handleTaskAddCommands)
                        .removeLastPrefix()

                        .prefix(exactStringIgnoreCase("remove"))
                        .applyHandler(CommandTasks::handleTaskRemoveCommands)
                        .removeLastPrefix()

                        .applyHandler(builder -> handleGeneralAddCommands(
                                builder,
                                internalProperties -> (ServiceConfigurationBase) internalProperties.get("task"),
                                serviceConfigurationBase -> CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask((ServiceTask) serviceConfigurationBase)
                        ))
                        .applyHandler(builder -> handleGeneralRemoveCommands(
                                builder,
                                internalProperties -> (ServiceConfigurationBase) internalProperties.get("task"),
                                serviceConfigurationBase -> CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask((ServiceTask) serviceConfigurationBase)
                        ))

                        .clearAll()

                        .prefix(exactStringIgnoreCase("group"))
                        .prefix(dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-group-not-found"),
                                name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
                                () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations()
                                        .stream()
                                        .map(GroupConfiguration::getName)
                                        .collect(Collectors.toList())
                        ))

                        .preExecute((sender, command, args, commandLine, properties, internalProperties) ->
                                internalProperties.put("group", CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfiguration((String) args[1]))
                        )

                        .generateCommand((sender, command, args, commandLine, properties, internalProperties) -> displayGroup(sender, (GroupConfiguration) internalProperties.get("group")))

                        .applyHandler(builder -> handleGeneralAddCommands(
                                builder,
                                internalProperties -> (ServiceConfigurationBase) internalProperties.get("group"),
                                serviceConfigurationBase -> CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration((GroupConfiguration) serviceConfigurationBase)
                        ))
                        .applyHandler(builder -> handleGeneralRemoveCommands(
                                builder,
                                internalProperties -> (ServiceConfigurationBase) internalProperties.get("group"),
                                serviceConfigurationBase -> CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration((GroupConfiguration) serviceConfigurationBase)
                        ))

                        .getSubCommands(),
                "tasks"
        );

        super.permission = "cloudnet.command.tasks";
        super.description = LanguageManager.getMessage("command-description-tasks");
    }

    private static void handleCreateCommands(SubCommandBuilder builder) {
        builder
                .prefix(exactStringIgnoreCase("create"))

                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            try {
                                String name = (String) args[2];
                                ServiceEnvironmentType type = (ServiceEnvironmentType) args[3];

                                CloudNet.getInstance().getCloudServiceManager().addPermanentServiceTask(new ServiceTask(
                                        Iterables.newArrayList(),
                                        Iterables.newArrayList(Collections.singletonList(
                                                new ServiceTemplate(name, "default", LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE)
                                        )),
                                        Iterables.newArrayList(),
                                        name,
                                        "jvm",
                                        true,
                                        false,
                                        new ArrayList<>(Collections.singletonList(CloudNet.getInstance().getConfig().getIdentity().getUniqueId())),
                                        new ArrayList<>(Collections.singletonList(name)),
                                        new ProcessConfiguration(
                                                type,
                                                type == ServiceEnvironmentType.BUNGEECORD || type == ServiceEnvironmentType.VELOCITY ||
                                                        type == ServiceEnvironmentType.PROX_PROX ? 128 : 372,
                                                Iterables.newArrayList()
                                        ),
                                        type == ServiceEnvironmentType.BUNGEECORD || type == ServiceEnvironmentType.VELOCITY ||
                                                type == ServiceEnvironmentType.PROX_PROX ? 25565 : 44955,
                                        0
                                ));

                                TemplateStorageUtil.createAndPrepareTemplate(
                                        CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE),
                                        name,
                                        "default",
                                        type
                                );

                                sender.sendMessage(LanguageManager.getMessage("command-tasks-create-task"));

                                if (!CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name)) {
                                    createEmptyGroupConfiguration(name);
                                    sender.sendMessage(LanguageManager.getMessage("command-tasks-create-group"));
                                }

                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }
                        },
                        exactStringIgnoreCase("task"),
                        dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-task-already-existing"),
                                name -> !CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(name)
                        ),
                        exactEnum(ServiceEnvironmentType.class)
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            createEmptyGroupConfiguration((String) args[2]);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-create-group"));
                        },
                        exactStringIgnoreCase("group"),
                        dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-group-already-existing"),
                                name -> !CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name)
                        )
                )

                .removeLastPrefix();
    }

    private static void handleDeleteCommands(SubCommandBuilder builder) {
        builder
                .prefix(exactStringIgnoreCase("delete"))

                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            CloudNet.getInstance().getCloudServiceManager().removePermanentServiceTask((String) args[2]);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-delete-task"));
                        },
                        exactStringIgnoreCase("task"),
                        dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-task-not-found"),
                                name -> CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(name),
                                () -> CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
                                        .map(ServiceTask::getName)
                                        .collect(Collectors.toList())
                        )
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            CloudNet.getInstance().getCloudServiceManager().removeGroupConfiguration((String) args[2]);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-delete-group"));
                        },
                        exactStringIgnoreCase("group"),
                        dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-group-not-found"),
                                name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
                                () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
                                        .map(GroupConfiguration::getName)
                                        .collect(Collectors.toList())
                        )
                )

                .removeLastPrefix();
    }

    private static void handleTaskSetCommands(SubCommandBuilder builder) {
        builder
                .postExecute((sender, command, args, commandLine, properties, internalProperties) -> {
                    ServiceTask serviceTask = (ServiceTask) internalProperties.get("task");

                    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);

                    sender.sendMessage(LanguageManager.getMessage("command-tasks-set-property-success")
                            .replace("%property%", (String) args[3])
                            .replace("%name%", serviceTask.getName())
                            .replace("%value%", String.valueOf(args[4]))
                    );
                })

                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setMinServiceCount((Integer) args[4]);
                        },
                        exactStringIgnoreCase("minServiceCount"),
                        integer("amount", amount -> amount >= 0)
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setMaintenance((Boolean) args[4]);
                        },
                        exactStringIgnoreCase("maintenance"),
                        boolean_("enabled")
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getProcessConfiguration().setMaxHeapMemorySize((Integer) args[4]);
                        },
                        exactStringIgnoreCase("maxHeapMemory"),
                        integer("memory", memory -> memory > 0)
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setStartPort((Integer) args[4]);
                        },
                        exactStringIgnoreCase("startPort"),
                        integer("startPort", 0, 65535)
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setAutoDeleteOnStop((Boolean) args[4]);
                        },
                        exactStringIgnoreCase("autoDeleteOnStop"),
                        boolean_("autoDeleteOnStop")
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setStaticServices((Boolean) args[4]);
                        },
                        exactStringIgnoreCase("staticServices"),
                        boolean_("staticServices")
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getProcessConfiguration().setEnvironment((ServiceEnvironmentType) args[4]);
                        },
                        exactStringIgnoreCase("environment"),
                        exactEnum(ServiceEnvironmentType.class)
                )

                .removeLastPostHandler();
    }

    private static void handleTaskAddCommands(SubCommandBuilder builder) {
        builder
                .postExecute((sender, command, args, commandLine, properties, internalProperties) -> {
                    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask((ServiceTask) internalProperties.get("task"));
                })

                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getAssociatedNodes().add((String) args[4]);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-add-node-success"));
                        },
                        exactStringIgnoreCase("node"),
                        dynamicString(
                                "uniqueId",
                                LanguageManager.getMessage("command-tasks-node-not-found"), uniqueId ->
                                        CloudNet.getInstance().getConfig().getIdentity().getUniqueId().equalsIgnoreCase(uniqueId) ||
                                                CloudNet.getInstance().getConfig().getClusterConfig().getNodes().stream()
                                                        .anyMatch(node -> node.getUniqueId().equalsIgnoreCase(uniqueId)),
                                () -> {
                                    Collection<String> nodes = CloudNet.getInstance().getConfig().getClusterConfig().getNodes().stream()
                                            .map(NetworkClusterNode::getUniqueId)
                                            .collect(Collectors.toList());
                                    nodes.add(CloudNet.getInstance().getConfig().getIdentity().getUniqueId());
                                    return nodes;
                                }
                        )
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getGroups().add((String) args[4]);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-add-group-success"));
                        },
                        exactStringIgnoreCase("group"),
                        dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-group-not-found"),
                                name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
                                () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
                                        .map(GroupConfiguration::getName)
                                        .collect(Collectors.toList())
                        )
                )

                .removeLastPostHandler();
    }

    private static void handleTaskRemoveCommands(SubCommandBuilder builder) {
        builder
                .postExecute((sender, command, args, commandLine, properties, internalProperties) -> {
                    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask((ServiceTask) internalProperties.get("task"));
                })

                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getAssociatedNodes().remove((String) args[4]);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-node-success"));
                        },
                        exactStringIgnoreCase("node"),
                        dynamicString(
                                "uniqueId",
                                () -> {
                                    Collection<String> nodes = CloudNet.getInstance().getConfig().getClusterConfig().getNodes().stream()
                                            .map(NetworkClusterNode::getUniqueId)
                                            .collect(Collectors.toList());
                                    nodes.add(CloudNet.getInstance().getConfig().getIdentity().getUniqueId());
                                    return nodes;
                                }
                        )
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getGroups().add((String) args[4]);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-group-success"));
                        },
                        exactStringIgnoreCase("group"),
                        dynamicString(
                                "name",
                                () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
                                        .map(GroupConfiguration::getName)
                                        .collect(Collectors.toList())
                        )
                )

                .removeLastPostHandler();
    }

    private static void handleGeneralAddCommands(SubCommandBuilder builder, Function<Map<String, Object>, ServiceConfigurationBase> configurationBaseFunction,
                                                 Consumer<ServiceConfigurationBase> updateHandler) {
        builder
                .prefix(exactStringIgnoreCase("add"))

                .postExecute((sender, command, args, commandLine, properties, internalProperties) -> updateHandler.accept(configurationBaseFunction.apply(internalProperties)))

                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args[4];
                            Collection<String> excludes = args.length > 5 ? (Collection<String>) args[5] : new ArrayList<>();

                            configuration.getDeployments().add(new ServiceDeployment(template, excludes));

                            sender.sendMessage(LanguageManager.getMessage("command-tasks-add-deployment-success"));
                        },
                        subCommand -> subCommand.setMinArgs(5).setMaxArgs(Integer.MAX_VALUE),
                        exactStringIgnoreCase("deployment"),
                        template("storage:prefix/name"),
                        collection("excludedFiles separated by \";\"")
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args[4];

                            configuration.getTemplates().add(template);

                            sender.sendMessage(LanguageManager.getMessage("command-tasks-add-template-success"));
                        },
                        exactStringIgnoreCase("template"),
                        template("storage:prefix/name")
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            String url = (String) args[4];
                            String target = (String) args[5];

                            configuration.getIncludes().add(new ServiceRemoteInclusion(url, target));

                            sender.sendMessage(LanguageManager.getMessage("command-tasks-add-inclusion-success"));
                        },
                        exactStringIgnoreCase("inclusion"),
                        url("url"),
                        dynamicString("targetPath")
                )

                .removeLastPrefix()
                .removeLastPostHandler();
    }

    private static void handleGeneralRemoveCommands(SubCommandBuilder builder, Function<Map<String, Object>, ServiceConfigurationBase> configurationBaseFunction,
                                                    Consumer<ServiceConfigurationBase> updateHandler) {
        builder
                .prefix(exactStringIgnoreCase("remove"))

                .postExecute((sender, command, args, commandLine, properties, internalProperties) -> updateHandler.accept(configurationBaseFunction.apply(internalProperties)))

                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args[4];

                            configuration.getDeployments().removeAll(configuration.getDeployments().stream()
                                    .filter(deployment -> deployment.getTemplate().equals(template))
                                    .collect(Collectors.toList())
                            );

                            sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-deployment-success"));
                        },
                        exactStringIgnoreCase("deployment"),
                        template("storage:prefix/name")
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args[4];

                            configuration.getTemplates().removeAll(configuration.getTemplates().stream()
                                    .filter(serviceTemplate -> serviceTemplate.equals(template))
                                    .collect(Collectors.toList())
                            );

                            sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-template-success"));
                        },
                        exactStringIgnoreCase("template"),
                        template("storage:prefix/name")
                )
                .generateCommand(
                        (sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            String url = (String) args[4];
                            String target = (String) args[5];

                            configuration.getIncludes().removeAll(configuration.getIncludes().stream()
                                    .filter(inclusion -> inclusion.getDestination().equalsIgnoreCase(target))
                                    .filter(inclusion -> inclusion.getUrl().equalsIgnoreCase(url))
                                    .collect(Collectors.toList())
                            );

                            sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-inclusion-success"));
                        },
                        exactStringIgnoreCase("inclusion"),
                        url("url"),
                        dynamicString("targetPath")
                )

                .removeLastPrefix()
                .removeLastPostHandler();
    }

    private static void displayTask(ICommandSender sender, ServiceTask serviceTask) {
        Collection<String> messages = Iterables.newArrayList();

        messages.addAll(Arrays.asList(
                " ",
                "* Name: " + serviceTask.getName(),
                "* Minimal Services: " + serviceTask.getMinServiceCount(),
                "* Associated nodes: " + serviceTask.getAssociatedNodes().toString(),
                "* Groups: " + serviceTask.getGroups().toString(),
                "* Start Port: " + serviceTask.getStartPort(),
                "* Static services: " + serviceTask.isStaticServices(),
                "* Auto delete on stop: " + serviceTask.isAutoDeleteOnStop(),
                "* Maintenance: " + serviceTask.isMaintenance(),
                "* Delete files on stop: " + serviceTask.getDeletedFilesAfterStop(),
                " ",
                "- Process Configuration",
                "* Environment: " + serviceTask.getProcessConfiguration().getEnvironment(),
                "* Max HeapMemory: " + serviceTask.getProcessConfiguration().getMaxHeapMemorySize(),
                "* JVM Options: " + serviceTask.getProcessConfiguration().getJvmOptions().toString(),
                " "
        ));

        applyDisplayMessagesForServiceConfigurationBase(messages, serviceTask);

        sender.sendMessage(messages.toArray(new String[0]));
    }

    private static void displayGroup(ICommandSender sender, GroupConfiguration groupConfiguration) {
        Collection<String> messages = Iterables.newArrayList();

        messages.addAll(Arrays.asList(
                " ",
                "* Name: " + groupConfiguration.getName(),
                " "
        ));

        applyDisplayMessagesForServiceConfigurationBase(messages, groupConfiguration);

        sender.sendMessage(messages.toArray(new String[0]));
    }

    private static void applyDisplayMessagesForServiceConfigurationBase(Collection<String> messages, ServiceConfigurationBase configurationBase) {
        messages.add("* Includes:");

        for (ServiceRemoteInclusion inclusion : configurationBase.getIncludes()) {
            messages.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
        }

        messages.add(" ");
        messages.add("* Templates:");

        for (ServiceTemplate template : configurationBase.getTemplates()) {
            messages.add("- " + template.getStorage() + ":" + template.getTemplatePath());
        }

        messages.add(" ");
        messages.add("* Deployments:");

        for (ServiceDeployment deployment : configurationBase.getDeployments()) {
            messages.add("- ");
            messages.add("Template:  " + deployment.getTemplate().getStorage() + ":" + deployment.getTemplate().getTemplatePath());
            messages.add("Excludes: " + deployment.getExcludes());
        }

        messages.add(" ");

        messages.addAll(Arrays.asList(configurationBase.getProperties().toPrettyJson().split("\n")));
        messages.add(" ");
    }

    private static void createEmptyGroupConfiguration(String name) {
        CloudNet.getInstance().getCloudServiceManager().addGroupConfiguration(new EmptyGroupConfiguration(name));
    }


    private static void setupTask(IConsole console, ICommandSender sender) {
        IFormatter logFormatter = console.hasColorSupport() ? new ColouredLogFormatter() : new DefaultLogFormatter();
        ConsoleQuestionListAnimation animation = new ConsoleQuestionListAnimation(
                () -> CloudNet.getInstance().getQueuedConsoleLogHandler().getCachedQueuedLogEntries()
                        .stream()
                        .map(logFormatter::format)
                        .collect(Collectors.toList()),
                () -> "&f _____              _       &b           _                 \n" +
                        "&f/__   \\  __ _  ___ | | __  &b ___   ___ | |_  _   _  _ __  \n" +
                        "&f  / /\\/ / _` |/ __|| |/ /  &b/ __| / _ \\| __|| | | || '_ \\ \n" +
                        "&f / /   | (_| |\\__ \\|   <  &b \\__ \\|  __/| |_ | |_| || |_) |\n" +
                        "&f \\/     \\__,_||___/|_|\\_\\&b  |___/ \\___| \\__| \\__,_|| .__/ \n" +
                        "&f                             &b                     |_|    ",
                () -> "Task creation complete!",
                "&r> &e"
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "name",
                        LanguageManager.getMessage("command-tasks-setup-question-name"),
                        new QuestionAnswerTypeString() {
                            @Override
                            public boolean isValidInput(String input) {
                                return super.isValidInput(input) && !input.trim().isEmpty() &&
                                        !CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(input);
                            }

                            @Override
                            public String getInvalidInputMessage(String input) {
                                if (CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(input)) {
                                    return "&c" + LanguageManager.getMessage("command-tasks-setup-task-already-exists");
                                }
                                return super.getInvalidInputMessage(input);
                            }
                        }
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "memory",
                        LanguageManager.getMessage("command-tasks-setup-question-memory"),
                        new QuestionAnswerTypeInt() {
                            @Override
                            public boolean isValidInput(String input) {
                                return super.isValidInput(input) && Integer.parseInt(input) > 0;
                            }

                            @Override
                            public String getRecommendation() {
                                return "512";
                            }

                            @Override
                            public List<String> getCompletableAnswers() {
                                return Arrays.asList("128", "256", "512", "1024", "2048", "4096", "8192");
                            }
                        }
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "maintenance",
                        LanguageManager.getMessage("command-tasks-setup-question-maintenance"),
                        new QuestionAnswerTypeBoolean()
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "autoDeleteOnStop",
                        LanguageManager.getMessage("command-tasks-setup-question-auto-delete"),
                        new QuestionAnswerTypeBoolean() {
                            @Override
                            public String getRecommendation() {
                                return super.getTrueString();
                            }
                        }
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "staticServices",
                        LanguageManager.getMessage("command-tasks-setup-question-static"),
                        new QuestionAnswerTypeBoolean()
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "startPort",
                        LanguageManager.getMessage("command-tasks-setup-question-startport"),
                        new QuestionAnswerTypeIntRange(0, 65535) {
                            @Override
                            public String getRecommendation() {
                                return "44955";
                            }
                        }
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "minServiceCount",
                        LanguageManager.getMessage("command-tasks-setup-question-minservices"),
                        new QuestionAnswerTypeInt()
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "environment",
                        LanguageManager.getMessage("command-tasks-setup-question-environment"),
                        new QuestionAnswerTypeEnum<ServiceEnvironmentType>(ServiceEnvironmentType.class) {
                            @Override
                            public String getRecommendation() {
                                return ServiceEnvironmentType.MINECRAFT_SERVER.name();
                            }
                        }
                )
        );

        animation.addEntry(
                new QuestionListEntry<>(
                        "serviceVersion",
                        LanguageManager.getMessage("command-tasks-setup-question-application"),
                        new QuestionAnswerTypeServiceVersion(
                                () -> (ServiceEnvironmentType) animation.getResult("environment"),
                                CloudNet.getInstance().getServiceVersionProvider()
                        )
                )
        );

        if (!CloudNet.getInstance().getConfig().getClusterConfig().getNodes().isEmpty()) {
            Collection<String> possibleNodes = CloudNet.getInstance().getConfig().getClusterConfig().getNodes()
                    .stream()
                    .map(NetworkClusterNode::getUniqueId)
                    .collect(Collectors.toList());
            possibleNodes.add(CloudNet.getInstance().getConfig().getIdentity().getUniqueId());
            animation.addEntry(
                    new QuestionListEntry<>(
                            "nodes",
                            LanguageManager.getMessage("command-tasks-setup-question-nodes"),
                            new QuestionAnswerTypeCollection(possibleNodes)
                    )
            );
        }

        animation.addFinishHandler(() -> {
            if (animation.isCancelled()) {
                return;
            }

            sender.sendMessage(LanguageManager.getMessage("command-tasks-setup-create-begin"));

            String name = (String) animation.getResult("name");
            int memory = (int) animation.getResult("memory");
            boolean maintenance = (boolean) animation.getResult("maintenance");
            boolean autoDeleteOnStop = (boolean) animation.getResult("autoDeleteOnStop");
            boolean staticServices = (boolean) animation.getResult("staticServices");
            int startPort = (int) animation.getResult("startPort");
            int minServiceCount = (int) animation.getResult("minServiceCount");
            ServiceEnvironmentType environmentType = (ServiceEnvironmentType) animation.getResult("environment");
            Collection<String> associatedNodes = animation.hasResult("nodes") ? (Collection<String>) animation.getResult("nodes") :
                    new ArrayList<>(Collections.singletonList(CloudNet.getInstance().getConfig().getIdentity().getUniqueId()));

            Pair<ServiceVersionType, ServiceVersion> serviceVersion = (Pair<ServiceVersionType, ServiceVersion>) animation.getResult("serviceVersion");

            ServiceTemplate template = new ServiceTemplate(name, "default", LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);
            ITemplateStorage templateStorage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

            try {
                TemplateStorageUtil.createAndPrepareTemplate(
                        templateStorage,
                        name,
                        "default",
                        environmentType
                );
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            if (serviceVersion != null) {
                CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(
                        serviceVersion.getFirst(),
                        serviceVersion.getSecond(),
                        templateStorage,
                        template
                );
            }

            ServiceTask serviceTask = new ServiceTask(
                    new ArrayList<>(),
                    new ArrayList<>(Collections.singletonList(template)),
                    new ArrayList<>(),
                    name,
                    "jvm",
                    maintenance,
                    autoDeleteOnStop,
                    staticServices,
                    associatedNodes,
                    new ArrayList<>(Collections.singletonList(name)),
                    new ArrayList<>(),
                    new ProcessConfiguration(
                            environmentType,
                            memory,
                            new ArrayList<>()
                    ),
                    startPort,
                    minServiceCount
            );

            CloudNet.getInstance().getCloudServiceManager().addPermanentServiceTask(serviceTask);

            sender.sendMessage(LanguageManager.getMessage("command-tasks-setup-create-success").replace("%name%", name));

            if (!CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name)) {
                CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(new EmptyGroupConfiguration(name));
                sender.sendMessage(LanguageManager.getMessage("command-tasks-create-group"));
            }
        });


        console.clearScreen();
        console.startAnimation(animation);
    }

}
