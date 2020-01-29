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

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

public class CommandTasks extends CommandServiceConfigurationBase {
    public CommandTasks() {
        super(
                SubCommandBuilder.create()

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> setupTask(CloudNet.getInstance().getConsole(), sender),
                                SubCommand::onlyConsole,
                                exactStringIgnoreCase("setup")
                        )
                        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            CloudNet.getInstance().getCloudServiceManager().reload();
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-reload-success"));
                        }, anyStringIgnoreCase("reload", "rl"))
                        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
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
                        }, subCommand -> subCommand.enableProperties().appendUsage("| name=NAME"), exactStringIgnoreCase("list"))

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
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
                                        internalProperties.put("task", CloudNet.getInstance().getServiceTaskProvider().getServiceTask((String) args.argument(1)))
                        )

                        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> displayTask(sender, (ServiceTask) internalProperties.get("task")))

                        .prefix(exactStringIgnoreCase("set"))
                        .applyHandler(CommandTasks::handleTaskSetCommands)
                        .removeLastPrefix()

                        .prefix(exactStringIgnoreCase("add"))
                        .applyHandler(CommandTasks::handleTaskAddCommands)
                        .removeLastPrefix()

                        .prefix(anyStringIgnoreCase("remove", "rm"))
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

                        .getSubCommands(),
                "tasks"
        );

        super.prefix = "cloudnet";
        super.permission = "cloudnet.command.tasks";
        super.description = LanguageManager.getMessage("command-description-tasks");
    }

    private static void handleCreateCommands(SubCommandBuilder builder) {
        builder
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            try {
                                String name = (String) args.argument("name").get();
                                ServiceEnvironmentType type = (ServiceEnvironmentType) args.argument(QuestionAnswerTypeEnum.class).get();

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
                                        type.getDefaultStartPort(),
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
                                    sender.sendMessage(LanguageManager.getMessage("command-service-base-create-group"));
                                }

                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }
                        },
                        exactStringIgnoreCase("create"),
                        dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-task-already-existing"),
                                name -> !CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(name)
                        ),
                        exactEnum(ServiceEnvironmentType.class)
                )

                .removeLastPrefix();
    }

    private static void handleDeleteCommands(SubCommandBuilder builder) {
        builder
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            String name = (String) args.argument("name").get();

                            CloudNet.getInstance().getCloudServiceManager().removePermanentServiceTask(name);
                            sender.sendMessage(LanguageManager.getMessage("command-tasks-delete-task"));

                            for (ServiceInfoSnapshot cloudService : CloudNet.getInstance().getCloudServiceProvider().getCloudServices(name)) {
                                CloudNet.getInstance().getCloudServiceProvider(cloudService).stop();
                            }
                        },
                        exactStringIgnoreCase("delete"),
                        dynamicString(
                                "name",
                                LanguageManager.getMessage("command-tasks-task-not-found"),
                                name -> CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(name),
                                () -> CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
                                        .map(ServiceTask::getName)
                                        .collect(Collectors.toList())
                        )
                )

                .removeLastPrefix();
    }

    private static void handleTaskSetCommands(SubCommandBuilder builder) {
        builder
                .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                    ServiceTask serviceTask = (ServiceTask) internalProperties.get("task");

                    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);

                    sender.sendMessage(LanguageManager.getMessage("command-tasks-set-property-success")
                            .replace("%property%", (String) args.argument(3))
                            .replace("%name%", serviceTask.getName())
                            .replace("%value%", String.valueOf(args.argument(4)))
                    );
                })

                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setMinServiceCount((Integer) args.argument(4));
                        },
                        exactStringIgnoreCase("minServiceCount"),
                        integer("amount", amount -> amount >= 0)
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setMaintenance((Boolean) args.argument(4));
                        },
                        exactStringIgnoreCase("maintenance"),
                        boolean_("enabled")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getProcessConfiguration().setMaxHeapMemorySize((Integer) args.argument(4));
                        },
                        exactStringIgnoreCase("maxHeapMemory"),
                        integer("memory", memory -> memory > 0)
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setStartPort((Integer) args.argument(4));
                        },
                        exactStringIgnoreCase("startPort"),
                        integer("startPort", 0, 65535)
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setAutoDeleteOnStop((Boolean) args.argument(4));
                        },
                        exactStringIgnoreCase("autoDeleteOnStop"),
                        boolean_("autoDeleteOnStop")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).setStaticServices((Boolean) args.argument(4));
                        },
                        exactStringIgnoreCase("staticServices"),
                        boolean_("staticServices")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getProcessConfiguration().setEnvironment((ServiceEnvironmentType) args.argument(4));
                        },
                        exactStringIgnoreCase("environment"),
                        exactEnum(ServiceEnvironmentType.class)
                )

                .removeLastPostHandler();
    }

    private static void handleTaskAddCommands(SubCommandBuilder builder) {
        builder
                .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask((ServiceTask) internalProperties.get("task"));
                })

                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getAssociatedNodes().add((String) args.argument(4));
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
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getGroups().add((String) args.argument(4));
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
                .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) ->
                        CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask((ServiceTask) internalProperties.get("task"))
                )

                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getAssociatedNodes().remove((String) args.argument(4));
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
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ((ServiceTask) internalProperties.get("task")).getGroups().add((String) args.argument(4));
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


    private static void setupTask(IConsole console, ICommandSender sender) {
        IFormatter logFormatter = console.hasColorSupport() ? new ColouredLogFormatter() : new DefaultLogFormatter();
        ConsoleQuestionListAnimation animation = new ConsoleQuestionListAnimation(
                "TaskSetup",
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
                        "startPort",
                        LanguageManager.getMessage("command-tasks-setup-question-startport"),
                        new QuestionAnswerTypeIntRange(0, 65535) {
                            @Override
                            public String getRecommendation() {
                                ServiceEnvironmentType type = animation.hasResult("environment") ? (ServiceEnvironmentType) animation.getResult("environment") : null;
                                return type != null ? String.valueOf(type.getDefaultStartPort()) : "44955";
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
                createEmptyGroupConfiguration(name);
                sender.sendMessage(LanguageManager.getMessage("command-service-base-create-group"));
            }
        });


        console.clearScreen();
        console.startAnimation(animation);
    }

}