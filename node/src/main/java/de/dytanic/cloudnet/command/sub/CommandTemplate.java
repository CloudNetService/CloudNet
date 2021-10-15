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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import de.dytanic.cloudnet.util.JavaVersionResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

@CommandAlias("t")
@CommandPermission("cloudnet.command.templates")
@CommandDescription("Manages the templates and allows installation of .jar files")
public class CommandTemplate {

  @Parser(suggestions = "serviceTemplate")
  public ServiceTemplate defaultServiceTemplateParser(CommandContext<CommandSource> $, Queue<String> input) {
    ServiceTemplate template = ServiceTemplate.parse(input.remove());
    if (template == null || template.nullableStorage() == null) {
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("ca-question-list-invalid-template"));
    }
    return template;
  }

  @Suggestions("serviceTemplate")
  public List<String> suggestServiceTemplate(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getLocalTemplateStorage().getTemplates()
      .stream()
      .map(ServiceTemplate::toString)
      .collect(Collectors.toList());
  }

  @Parser
  public TemplateStorage defaultTemplateStorageParser(CommandContext<CommandSource> $, Queue<String> input) {
    TemplateStorage templateStorage = CloudNet.getInstance().getTemplateStorage(input.remove());
    if (templateStorage == null) {
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("ca-question-list-template-invalid-storage"));
    }
    return templateStorage;
  }

  @Suggestions("templateStorage")
  public List<String> suggestTemplateStorage(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getAvailableTemplateStorages()
      .stream()
      .map(INameable::getName)
      .collect(Collectors.toList());
  }

  @Parser(suggestions = "serviceVersionType")
  public ServiceVersionType defaultVersionTypeParser(CommandContext<CommandSource> $, Queue<String> input) {
    String versionTypeName = input.remove().toLowerCase();
    return CloudNet.getInstance().getServiceVersionProvider().getServiceVersionType(versionTypeName)
      .orElseThrow(() -> new ArgumentNotAvailableException(
        LanguageManager.getMessage("ca-question-list-invalid-service-version")));
  }

  @Suggestions("serviceVersionType")
  public List<String> suggestServiceVersionType(CommandContext<CommandSource> $, String input) {
    return new ArrayList<>(CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().keySet());
  }

  @Parser(suggestions = "environmentType")
  public ServiceEnvironmentType defaultEnvironmentTypeParser(CommandContext<CommandSource> $, Queue<String> input) {
    return ServiceEnvironmentType.valueOf(input.remove());
  }

  @Suggestions("environmentType")
  public List<String> suggestEnvironmentType(CommandContext<CommandSource> $, String input) {
    return Arrays.stream(ServiceEnvironmentType.VALUES).map(Enum::name).collect(Collectors.toList());
  }

  @CommandMethod("template|t list [storage]")
  public void displayTemplates(CommandSource source, @Argument("storage") TemplateStorage templateStorage) {
    TemplateStorage resultingStorage =
      templateStorage == null ? CloudNet.getInstance().getLocalTemplateStorage() : templateStorage;

    List<String> messages = new ArrayList<>();
    messages.add(LanguageManager.getMessage("command-template-list-templates")
      .replace("%storage%", resultingStorage.getName()));

    for (ServiceTemplate template : resultingStorage.getTemplates()) {
      messages.add("  " + template.toString());
    }

    source.sendMessage(messages);
  }

  @CommandMethod("template|t versions|v")
  public void displayTemplateVersions(CommandSource source) {
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

    source.sendMessage(LanguageManager.getMessage("command-template-list-versions"));

    //todo source.sendMessage(ColumnTextFormatter.formatInColumns(versions, "\n", 4).split("\n"));
  }

  @CommandMethod("template|t install <template> <versionType> <version>")
  public void installTemplate(
    CommandSource source,
    @Argument("template") ServiceTemplate serviceTemplate,
    @Argument("versionType") ServiceVersionType versionType,
    @Argument("version") String version,
    @Flag("force") boolean forceInstall,
    @Flag("executable") @Quoted String executable
  ) {

    ServiceVersion serviceVersion = versionType.getVersion(version).orElse(null);
    if (serviceVersion == null) {
      source.sendMessage(LanguageManager.getMessage("ca-question-list-invalid-service-version"));
      return;
    }

    String resolvedExecutable = executable == null ? "java" : executable;
    JavaVersion javaVersion = JavaVersionResolver.resolveFromJavaExecutable(resolvedExecutable);
    if (javaVersion == null) {
      source.sendMessage(LanguageManager.getMessage("ca-question-list-invalid-java-executable"));
      return;
    }

    if (!versionType.canInstall(serviceVersion, javaVersion)) {
      source.sendMessage(LanguageManager.getMessage("command-template-install-wrong-java")
        .replace("%version%", versionType.getName() + "-" + serviceVersion.getName())
        .replace("%java%", javaVersion.getName())
      );
      if (!forceInstall) {
        return;
      }
    }

    CloudNet.getInstance().getMainThread().runTask(() -> {
      source.sendMessage(LanguageManager.getMessage("command-template-install-try")
        .replace("%version%", versionType.getName() + "-" + serviceVersion.getName())
        .replace("%template%", serviceTemplate.toString())
      );

      InstallInformation installInformation = InstallInformation.builder(versionType, serviceVersion)
        .toTemplate(serviceTemplate)
        .executable(resolvedExecutable.equals("java") ? null : resolvedExecutable)
        .build();

      if (CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(installInformation, forceInstall)) {
        source.sendMessage(LanguageManager.getMessage("command-template-install-success")
          .replace("%version%", versionType.getName() + "-" + serviceVersion.getName())
          .replace("%template%", serviceTemplate.toString())
        );
      } else {
        source.sendMessage(LanguageManager.getMessage("command-template-install-failed")
          .replace("%version%", versionType.getName() + "-" + serviceVersion.getName())
          .replace("%template%", serviceTemplate.toString())
        );
      }
    });

  }

  @CommandMethod("template|t delete|rm|del <template>")
  public void deleteTemplate(CommandSource source, @Argument("template") ServiceTemplate template) {
    SpecificTemplateStorage templateStorage = template.storage();
    if (!templateStorage.exists()) {
      source.sendMessage(LanguageManager.getMessage("command-template-delete-template-not-found")
        .replace("%template%", template.getFullName())
        .replace("%storage%", template.getStorage()));
      return;
    }

    templateStorage.delete();
    source.sendMessage("Deleted template");
  }

  @CommandMethod("template|t create <template> <environment>")
  public void createTemplate(
    CommandSource source,
    @Argument("template") ServiceTemplate template,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    SpecificTemplateStorage templateStorage = template.storage();
    if (templateStorage.exists()) {
      source.sendMessage(LanguageManager.getMessage("command-template-create-template-already-exists"));
      return;
    }

    try {
      if (TemplateStorageUtil.createAndPrepareTemplate(template.storage(), environmentType)) {
        source.sendMessage(LanguageManager.getMessage("command-template-create-success")
          .replace("%template%", template.getFullName())
          .replace("%storage%", template.getStorage())
        );
      }
    } catch (IOException exception) {
      source.sendMessage(LanguageManager.getMessage("command-template-create-failed")
        .replace("%template%", template.getFullName())
        .replace("%storage%", template.getStorage())
      );
    }
  }

  @CommandMethod("template|t copy|cp <sourceTemplate> <targetTemplate>")
  public void copyTemplate(
    CommandSource source,
    @Argument("sourceTemplate") ServiceTemplate sourceTemplate,
    @Argument("targetTemplate") ServiceTemplate targetTemplate
  ) {
    if (sourceTemplate.equals(targetTemplate)) {
      source.sendMessage(LanguageManager.getMessage("command-template-copy-same-source-and-target"));
      return;
    }

    SpecificTemplateStorage sourceStorage = sourceTemplate.storage();
    SpecificTemplateStorage targetStorage = targetTemplate.storage();

    CloudNet.getInstance().getMainThread().runTask(() -> {
      source.sendMessage(LanguageManager.getMessage("command-template-copy")
        .replace("%sourceTemplate%", sourceTemplate.toString())
        .replace("%targetTemplate%", targetTemplate.toString())
      );

      targetStorage.delete();
      targetStorage.create();
      try (ZipInputStream stream = sourceStorage.asZipInputStream()) {
        if (stream == null) {
          source.sendMessage(LanguageManager.getMessage("command-template-copy-failed"));
          return;
        }

        targetStorage.deploy(stream);
        source.sendMessage(LanguageManager.getMessage("command-template-copy-success")
          .replace("%sourceTemplate%", sourceTemplate.toString())
          .replace("%targetTemplate%", targetTemplate.toString())
        );
      } catch (IOException exception) {
        source.sendMessage(LanguageManager.getMessage("command-template-copy-failed"));
      }
    });
  }
}
