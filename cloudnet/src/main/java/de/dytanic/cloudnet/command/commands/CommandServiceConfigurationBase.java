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
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.collection;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.template;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.url;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.CommandInterrupt;
import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.service.EmptyGroupConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandServiceConfigurationBase extends SubCommandHandler {

  protected CommandServiceConfigurationBase(Collection<SubCommand> subCommands, String... names) {
    super(subCommands, names);
  }

  protected static void handleGeneralAddCommands(SubCommandBuilder builder,
    Function<Map<String, Object>, ServiceConfigurationBase[]> configurationBaseFunction,
    Consumer<ServiceConfigurationBase> updateHandler) {
    builder
      .prefix(exactStringIgnoreCase("add"))

      .postExecute(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), updateHandler))

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            ServiceTemplate template = (ServiceTemplate) args.argument(4);
            Collection<String> excludes = args.length() > 5 ? (Collection<String>) args.argument(5) : new ArrayList<>();

            configuration.getDeployments().add(new ServiceDeployment(template, excludes));

            sender.sendMessage(LanguageManager.getMessage("command-service-base-add-deployment-success"));
          }),
        subCommand -> subCommand.setMinArgs(5).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("deployment"),
        template("storage:prefix/name", true),
        collection("excludedFiles separated by \";\"")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            ServiceTemplate template = (ServiceTemplate) args.argument(4);

            configuration.getTemplates().add(template);

            sender.sendMessage(LanguageManager.getMessage("command-service-base-add-template-success"));
          }),
        exactStringIgnoreCase("template"),
        template("storage:prefix/name", true)
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            String url = (String) args.argument(4);
            String target = (String) args.argument(5);

            configuration.getIncludes().add(new ServiceRemoteInclusion(url, target));

            sender.sendMessage(LanguageManager.getMessage("command-service-base-add-inclusion-success"));
          }),
        exactStringIgnoreCase("inclusion"),
        url("url"),
        dynamicString("targetPath")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            String value = (String) args.argument("value").get();
            if (configuration.getJvmOptions().contains(value)) {
              sender.sendMessage(LanguageManager.getMessage("command-service-base-add-jvm-option-already-existing"));
              return;
            }
            configuration.getJvmOptions().add(value);
            sender.sendMessage(LanguageManager.getMessage("command-service-base-add-jvm-option-success"));
          }),
        subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("jvmOption"),
        dynamicString("value")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            String value = (String) args.argument("value").get();

            if (configuration.getProcessParameters().contains(value)) {
              sender.sendMessage(
                LanguageManager.getMessage("command-service-base-add-process-parameters-option-already-existing"));
              return;
            }
            configuration.getProcessParameters().add(value);
            sender
              .sendMessage(LanguageManager.getMessage("command-service-base-add-process-parameters-option-success"));
          }),
        subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("processParameter"),
        dynamicString("value")
      )

      .removeLastPrefix()
      .removeLastPostHandler();
  }

  protected static void handleGeneralRemoveCommands(SubCommandBuilder builder,
    Function<Map<String, Object>, ServiceConfigurationBase[]> configurationBaseFunction,
    Consumer<ServiceConfigurationBase> updateHandler) {
    builder
      .prefix(anyStringIgnoreCase("remove", "rm"))

      .postExecute(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), updateHandler))

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            ServiceTemplate template = (ServiceTemplate) args.argument(4);

            configuration.getDeployments().removeAll(configuration.getDeployments().stream()
              .filter(deployment -> deployment.getTemplate().equals(template))
              .collect(Collectors.toList())
            );

            sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-deployment-success"));
          }),
        exactStringIgnoreCase("deployment"),
        template("storage:prefix/name")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            ServiceTemplate template = (ServiceTemplate) args.argument(4);

            configuration.getTemplates().removeAll(configuration.getTemplates().stream()
              .filter(serviceTemplate -> serviceTemplate.equals(template))
              .collect(Collectors.toList())
            );

            sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-template-success"));
          }),
        exactStringIgnoreCase("template"),
        template("storage:prefix/name")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            String url = (String) args.argument(4);
            String target = (String) args.argument(5);

            configuration.getIncludes().removeAll(configuration.getIncludes().stream()
              .filter(inclusion -> inclusion.getDestination().equalsIgnoreCase(target))
              .filter(inclusion -> inclusion.getUrl().equalsIgnoreCase(url))
              .collect(Collectors.toList())
            );

            sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-inclusion-success"));
          }),
        exactStringIgnoreCase("inclusion"),
        url("url"),
        dynamicString("targetPath")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            String value = (String) args.argument("value").get();
            if (!configuration.getJvmOptions().contains(value)) {
              sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-jvm-option-not-found"));
              throw new CommandInterrupt();
            }
            configuration.getJvmOptions().remove(value);
            sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-jvm-option-success"));
          }),
        subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("jvmOption"),
        dynamicString("value")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            String value = (String) args.argument("value").get();

            if (!configuration.getProcessParameters().contains(value)) {
              sender.sendMessage(
                LanguageManager.getMessage("command-service-base-remove-process-parameter-option-not-found"));
              throw new CommandInterrupt();
            }
            configuration.getProcessParameters().remove(value);
            sender
              .sendMessage(LanguageManager.getMessage("command-service-base-remove-process-parameter-option-success"));
          }),
        subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("processParameter"),
        dynamicString("value")
      )

      .removeLastPrefix()

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachConfigurations(
          configurationBaseFunction.apply(internalProperties), configuration -> {
            configuration.getJvmOptions().clear();
            sender.sendMessage(LanguageManager.getMessage("command-service-base-clear-jvm-options-success"));
          }),
        exactStringIgnoreCase("clear"),
        exactStringIgnoreCase("jvmOptions")
      )

      .removeLastPostHandler();
  }

  protected static void applyDisplayMessagesForServiceConfigurationBase(Collection<String> messages,
    ServiceConfigurationBase configurationBase) {
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
      messages
        .add("Template:  " + deployment.getTemplate().getStorage() + ":" + deployment.getTemplate().getTemplatePath());
      messages.add("Excludes: " + deployment.getExcludes());
    }

    messages.add(" ");
    messages.add("* JVM Options");
    for (String jvmOption : configurationBase.getJvmOptions()) {
      messages.add(" - " + jvmOption);
    }

    messages.add(" ");
    messages.add("* Process Parameters");
    for (String parameter : configurationBase.getProcessParameters()) {
      messages.add(" - " + parameter);
    }

    messages.add(" ");

    messages.addAll(Arrays.asList(configurationBase.getProperties().toPrettyJson().split("\n")));
    messages.add(" ");
  }

  protected static void createEmptyGroupConfiguration(String name) {
    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(new EmptyGroupConfiguration(name));
  }

  protected static void forEachConfigurations(ServiceConfigurationBase[] configurations,
    Consumer<ServiceConfigurationBase> consumer) {
    Arrays.asList(configurations).forEach(consumer);
  }

}
