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
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactEnum;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.template;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.driver.util.ColumnTextFormatter;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

public class CommandTemplate extends SubCommandHandler {

  public CommandTemplate() {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            String storageName = (String) args.argument("storage").orElse("local");
            TemplateStorage storage = CloudNet.getInstance().getTemplateStorage(storageName);

            List<String> messages = new ArrayList<>();
            messages
              .add(LanguageManager.getMessage("command-template-list-templates").replace("%storage%", storageName));

            for (ServiceTemplate template : storage.getTemplates()) {
              messages.add("  " + template.getTemplatePath());
            }

            sender.sendMessage(messages.toArray(new String[0]));
          },
          subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length - 1)
            .setMaxArgs(subCommand.getRequiredArguments().length),
          exactStringIgnoreCase("list"),
          dynamicString(
            "storage",
            LanguageManager.getMessage("command-template-storage-not-found"),
            input -> CloudNet.getInstance().getTemplateStorage(input) != null
          )
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            List<String> versions = new ArrayList<>();

            for (ServiceVersionType versionType : CloudNet.getInstance().getServiceVersionProvider()
              .getServiceVersionTypes().values()) {
              List<String> messages = new ArrayList<>();

              messages.add("  " + versionType.getName() + ":");

              for (ServiceVersion version : versionType.getVersions()) {
                messages.add("    " + version.getName());
              }

              versions.add(String.join("\n", messages));
            }

            sender.sendMessage(LanguageManager.getMessage("command-template-list-versions"));

            sender.sendMessage(ColumnTextFormatter.formatInColumns(versions, "\n", 4).split("\n"));
          },
          exactStringIgnoreCase("versions")
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {

            ServiceTemplate template = (ServiceTemplate) args.argument("template").get();
            ServiceVersionType versionType = CloudNet.getInstance().getServiceVersionProvider()
              .getServiceVersionType((String) args.argument("versionType").get()).get();

            Optional<ServiceVersion> optionalVersion = versionType.getVersion((String) args.argument("version").get());

            if (!optionalVersion.isPresent()) { //the version might be available, but not for the given version type
              sender.sendMessage(LanguageManager.getMessage("command-template-invalid-version"));
              return;
            }

            ServiceVersion version = optionalVersion.get();

            boolean forceInstall = properties.containsKey("force");

            if (!versionType.canInstall(version)) {
              sender.sendMessage(LanguageManager.getMessage("command-template-install-wrong-java")
                .replace("%version%", versionType.getName() + "-" + version.getName())
                .replace("%java%", JavaVersion.getRuntimeVersion().getName())
              );
              if (!forceInstall) {
                return;
              }
            }

            CloudNet.getInstance().scheduleTask(() -> {
              sender.sendMessage(LanguageManager.getMessage("command-template-install-try")
                .replace("%version%", versionType.getName() + "-" + version.getName())
                .replace("%template%", template.toString())
              );

              if (CloudNet.getInstance().getServiceVersionProvider()
                .installServiceVersion(versionType, version, template, forceInstall)) {
                sender.sendMessage(LanguageManager.getMessage("command-template-install-success")
                  .replace("%version%", versionType.getName() + "-" + version.getName())
                  .replace("%template%", template.toString())
                );
              } else {
                sender.sendMessage(LanguageManager.getMessage("command-template-install-failed")
                  .replace("%version%", versionType.getName() + "-" + version.getName())
                  .replace("%template%", template.toString())
                );
              }
              return null;
            });
          },
          subCommand -> subCommand
            .enableProperties()
            .appendUsage("| --force | example: template install Lobby/default paperspigot 1.13.2"),
          exactStringIgnoreCase("install"),
          template("template", true),
          dynamicString(
            "versionType",
            LanguageManager.getMessage("command-template-invalid-version-type"),
            input -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionType(input).isPresent(),
            () -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().keySet()
          ),
          dynamicString(
            "version",
            LanguageManager.getMessage("command-template-invalid-version"),
            input -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().values().stream()
              .map(ServiceVersionType::getVersions)
              .flatMap(Collection::parallelStream)
              .map(ServiceVersion::getName)
              .anyMatch(name -> name.equalsIgnoreCase(input)),
            () -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().values().stream()
              .map(ServiceVersionType::getVersions)
              .flatMap(Collection::parallelStream)
              .map(ServiceVersion::getName)
              .collect(Collectors.toList())
          )
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            ServiceTemplate template = (ServiceTemplate) args.argument("template").get();
            SpecificTemplateStorage storage = template.storage();

            if (!storage.exists()) {
              sender.sendMessage(LanguageManager.getMessage("command-template-delete-template-not-found")
                .replace("%template%", template.getTemplatePath())
                .replace("%storage%", template.getStorage())
              );
              return;
            }

            storage.delete();
            sender.sendMessage(LanguageManager.getMessage("command-template-delete-success")
              .replace("%template%", template.getTemplatePath())
              .replace("%storage%", template.getStorage())
            );
          },
          anyStringIgnoreCase("delete", "remove", "rm"),
          template("template", true)
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            ServiceTemplate template = (ServiceTemplate) args.argument("template").get();
            SpecificTemplateStorage storage = template.storage();
            ServiceEnvironmentType environment = (ServiceEnvironmentType) args.argument(QuestionAnswerTypeEnum.class)
              .get();

            if (storage.exists()) {
              sender.sendMessage(LanguageManager.getMessage("command-template-create-template-already-exists")
                .replace("%template%", template.getTemplatePath())
                .replace("%storage%", template.getStorage())
              );
              return;
            }

            try {
              if (TemplateStorageUtil.createAndPrepareTemplate(template, environment)) {
                sender.sendMessage(LanguageManager.getMessage("command-template-create-success")
                  .replace("%template%", template.getTemplatePath())
                  .replace("%storage%", template.getStorage())
                );
              }
            } catch (IOException exception) {
              sender.sendMessage(LanguageManager.getMessage("command-template-create-failed")
                .replace("%template%", template.getTemplatePath())
                .replace("%storage%", template.getStorage())
              );
              exception.printStackTrace();
            }
          },
          anyStringIgnoreCase("create", "new"),
          template("template"),
          exactEnum(ServiceEnvironmentType.class)
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            ServiceTemplate sourceTemplate = (ServiceTemplate) args.argument("storage:prefix/name (sourceTemplate)")
              .get();
            ServiceTemplate targetTemplate = (ServiceTemplate) args.argument("storage:prefix/name (targetTemplate)")
              .get();
            SpecificTemplateStorage sourceStorage = sourceTemplate.storage();
            SpecificTemplateStorage targetStorage = targetTemplate.storage();

            if (sourceTemplate.equals(targetTemplate)) {
              sender.sendMessage(LanguageManager.getMessage("command-template-copy-same-source-and-target"));
              return;
            }

            CloudNet.getInstance().scheduleTask(() -> {
              sender.sendMessage(LanguageManager.getMessage("command-template-copy")
                .replace("%sourceTemplate%", sourceTemplate.toString())
                .replace("%targetTemplate%", targetTemplate.toString())
              );

              targetStorage.delete();
              targetStorage.create();

              try (ZipInputStream stream = sourceStorage.asZipInputStream()) {
                if (stream == null) {
                  sender.sendMessage(LanguageManager.getMessage("command-template-copy-failed"));
                  return null;
                }

                targetStorage.deploy(stream);
                sender.sendMessage(LanguageManager.getMessage("command-template-copy-success")
                  .replace("%sourceTemplate%", sourceTemplate.toString())
                  .replace("%targetTemplate%", targetTemplate.toString())
                );
              }

              return null;
            });
          },
          anyStringIgnoreCase("copy", "cp"),
          template("storage:prefix/name (sourceTemplate)"),
          template("storage:prefix/name (targetTemplate)")
        )

        .getSubCommands(),
      "template", "t"
    );
    super.prefix = "cloudnet";
    super.permission = "cloudnet.command." + super.names[0];
    super.description = LanguageManager.getMessage("command-description-template");
  }
}
