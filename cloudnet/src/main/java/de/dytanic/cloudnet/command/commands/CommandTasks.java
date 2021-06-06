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
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.bool;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactEnum;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.integer;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.common.WildcardUtil;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.DefaultLogFormatter;
import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeCollection;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeInt;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeIntRange;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeServiceVersion;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeString;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CommandTasks extends CommandServiceConfigurationBase {

  public CommandTasks() {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> setupTask(
            CloudNet.getInstance().getConsole(), sender),
          SubCommand::onlyConsole,
          exactStringIgnoreCase("setup")
        )
        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          CloudNet.getInstance().getServiceTaskProvider().reload();
          sender.sendMessage(LanguageManager.getMessage("command-tasks-reload-success"));
        }, anyStringIgnoreCase("reload", "rl"))
        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          sender.sendMessage("- Tasks", " ");

          for (ServiceTask serviceTask : CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()) {
            if (properties.containsKey("name") &&
              !serviceTask.getName().toLowerCase().contains(properties.get("name").toLowerCase())) {
              continue;
            }

            sender.sendMessage(
              serviceTask.getName() + " | MinServiceCount: " + serviceTask.getMinServiceCount() + " | Nodes: " +
                (serviceTask.getAssociatedNodes().isEmpty() ? "All" : serviceTask.getAssociatedNodes())
                + " | StartPort: " +
                serviceTask.getStartPort());
          }
        }, subCommand -> subCommand.enableProperties().appendUsage("| name=NAME"), exactStringIgnoreCase("list"))

        .applyHandler(CommandTasks::handleCreateCommands)
        .applyHandler(CommandTasks::handleDeleteCommands)

        .prefix(exactStringIgnoreCase("task"))
        .prefix(dynamicString(
          "name",
          LanguageManager.getMessage("command-tasks-task-not-found"),
          name -> WildcardUtil.anyMatch(
            CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks(),
            name
          ),
          () -> CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()
            .stream()
            .map(ServiceTask::getName)
            .collect(Collectors.toList())
        ))

        .preExecute(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
            internalProperties.put("tasks", WildcardUtil.filterWildcard(
              CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks(),
              (String) args.argument(1)
            ).toArray(new ServiceConfigurationBase[0]))
        )

        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks((ServiceConfigurationBase[]) internalProperties.get("tasks"), task -> displayTask(sender, task))
        )

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
          internalProperties -> (ServiceConfigurationBase[]) internalProperties.get("tasks"),
          serviceConfigurationBase -> CloudNet.getInstance().getServiceTaskProvider()
            .addPermanentServiceTask((ServiceTask) serviceConfigurationBase)
        ))
        .applyHandler(builder -> handleGeneralRemoveCommands(
          builder,
          internalProperties -> (ServiceConfigurationBase[]) internalProperties.get("tasks"),
          serviceConfigurationBase -> CloudNet.getInstance().getServiceTaskProvider()
            .addPermanentServiceTask((ServiceTask) serviceConfigurationBase)
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

            CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(new ServiceTask(
              new ArrayList<>(),
              new ArrayList<>(Collections.singletonList(ServiceTemplate.local(name, "default"))),
              new ArrayList<>(),
              name,
              "jvm",
              true,
              false,
              new ArrayList<>(
                Collections.singletonList(CloudNet.getInstance().getConfig().getIdentity().getUniqueId())),
              new ArrayList<>(Collections.singletonList(name)),
              new ProcessConfiguration(
                type,
                type.isMinecraftProxy() ? 256 : 512,
                new ArrayList<>(),
                new ArrayList<>()
              ),
              type.getDefaultStartPort(),
              0
            ));

            TemplateStorageUtil.createAndPrepareTemplate(ServiceTemplate.local(name, "default"), type);

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

          CloudNet.getInstance().getServiceTaskProvider().removePermanentServiceTask(name);
          sender.sendMessage(LanguageManager.getMessage("command-tasks-delete-task"));

          for (ServiceInfoSnapshot cloudService : CloudNet.getInstance().getCloudServiceProvider()
            .getCloudServices(name)) {
            cloudService.provider().stop();
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
        forEachTasks((ServiceConfigurationBase[]) internalProperties.get("tasks"), serviceTask -> {
          CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);

          sender.sendMessage(LanguageManager.getMessage("command-tasks-set-property-success")
            .replace("%property%", (String) args.argument(3))
            .replace("%name%", serviceTask.getName())
            .replace("%value%", String.valueOf(args.argument(4)))
          );
        });
      })

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.setMinServiceCount((Integer) args.argument(4))
          ),
        exactStringIgnoreCase("minServiceCount"),
        integer("amount", amount -> amount >= 0)
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.setMaintenance((Boolean) args.argument(4))
          ),
        exactStringIgnoreCase("maintenance"),
        bool("enabled")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.getProcessConfiguration().setMaxHeapMemorySize((Integer) args.argument(4))
          ),
        exactStringIgnoreCase("maxHeapMemory"),
        integer("memory", memory -> memory > 0)
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.setStartPort((Integer) args.argument(4))
          ),
        exactStringIgnoreCase("startPort"),
        integer("startPort", 0, 65535)
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.setAutoDeleteOnStop((Boolean) args.argument(4))
          ),
        exactStringIgnoreCase("autoDeleteOnStop"),
        bool("autoDeleteOnStop")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.setStaticServices((Boolean) args.argument(4))
          ),
        exactStringIgnoreCase("staticServices"),
        bool("staticServices")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.getProcessConfiguration().setEnvironment((ServiceEnvironmentType) args.argument(4))
          ),
        exactStringIgnoreCase("environment"),
        exactEnum(ServiceEnvironmentType.class)
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.setDisableIpRewrite((boolean) args.argument("value").orElse(false))
          ),
        exactStringIgnoreCase("disableIpRewrite"),
        bool("value")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          forEachTasks(
            (ServiceConfigurationBase[]) internalProperties.get("tasks"),
            task -> task.setJavaCommand((String) args.argument("value").orElse(null))
          ),
        exactStringIgnoreCase("javaCommand"),
        dynamicString("value")
      )

      .removeLastPostHandler();
  }

  private static void handleTaskAddCommands(SubCommandBuilder builder) {
    builder
      .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) ->
        forEachTasks((ServiceConfigurationBase[]) internalProperties.get("tasks"),
          serviceTask -> CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask)
        )
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          String node = (String) args.argument(4);
          forEachTasks((ServiceConfigurationBase[]) internalProperties.get("tasks"), serviceTask -> {
            if (serviceTask.getAssociatedNodes().contains(node)) {
              sender.sendMessage(LanguageManager.getMessage("command-tasks-add-node-already-added"));
            } else {
              serviceTask.getAssociatedNodes().add(node);
              sender.sendMessage(LanguageManager.getMessage("command-tasks-add-node-success"));
            }
          });
        },
        exactStringIgnoreCase("node"),
        dynamicString(
          "uniqueId",
          LanguageManager.getMessage("command-tasks-node-not-found"),
          uniqueId ->
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
          String group = (String) args.argument(4);
          forEachTasks((ServiceConfigurationBase[]) internalProperties.get("tasks"), serviceTask -> {
            if (serviceTask.getGroups().contains(group)) {
              sender.sendMessage(LanguageManager.getMessage("command-tasks-add-group-already-added"));
            } else {
              serviceTask.getGroups().add(group);
              sender.sendMessage(LanguageManager.getMessage("command-tasks-add-group-success"));
            }
          });
        },
        exactStringIgnoreCase("group"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-service-base-group-not-found"),
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
        forEachTasks((ServiceConfigurationBase[]) internalProperties.get("tasks"), serviceTask ->
          CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask))
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachTasks(
          (ServiceConfigurationBase[]) internalProperties.get("tasks"), serviceTask -> {
            serviceTask.getAssociatedNodes().remove(args.argument(4));
            sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-node-success"));
          }),
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
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachTasks(
          (ServiceConfigurationBase[]) internalProperties.get("tasks"), serviceTask -> {
            serviceTask.getGroups().remove(args.argument(4));
            sender.sendMessage(LanguageManager.getMessage("command-tasks-remove-group-success"));
          }),
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

    Collection<String> messages = new ArrayList<>(Arrays.asList(
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
      "* Process Parameters: " + serviceTask.getProcessConfiguration().getProcessParameters().toString(),
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
          public boolean isValidInput(@NotNull String input) {
            return super.isValidInput(input) && !input.trim().isEmpty() &&
              !CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(input);
          }

          @Override
          public String getInvalidInputMessage(@NotNull String input) {
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
          public boolean isValidInput(@NotNull String input) {
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
            ServiceEnvironmentType type =
              animation.hasResult("environment") ? (ServiceEnvironmentType) animation.getResult("environment") : null;
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
      Collection<String> associatedNodes =
        animation.hasResult("nodes") ? (Collection<String>) animation.getResult("nodes") :
          new ArrayList<>(Collections.singletonList(CloudNet.getInstance().getConfig().getIdentity().getUniqueId()));

      Pair<ServiceVersionType, ServiceVersion> serviceVersion = (Pair<ServiceVersionType, ServiceVersion>) animation
        .getResult("serviceVersion");

      ServiceTemplate template = ServiceTemplate.local(name, "default");

      try {
        TemplateStorageUtil.createAndPrepareTemplate(template, environmentType);
      } catch (IOException exception) {
        exception.printStackTrace();
      }

      if (serviceVersion != null) {
        CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(
          serviceVersion.getFirst(),
          serviceVersion.getSecond(),
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
          new ArrayList<>(),
          new ArrayList<>()
        ),
        startPort,
        minServiceCount
      );

      CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);

      sender.sendMessage(LanguageManager.getMessage("command-tasks-setup-create-success").replace("%name%", name));

      if (!CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name)) {
        createEmptyGroupConfiguration(name);
        sender.sendMessage(LanguageManager.getMessage("command-service-base-create-group"));
      }
    });

    console.clearScreen();
    console.startAnimation(animation);
  }

  private static void forEachTasks(ServiceConfigurationBase[] configurations, Consumer<ServiceTask> consumer) {
    for (ServiceConfigurationBase configuration : configurations) {
      consumer.accept((ServiceTask) configuration);
    }
  }

}
