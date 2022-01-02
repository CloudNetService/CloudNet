/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.cloudnet.common.column.ColumnFormatter;
import eu.cloudnetservice.cloudnet.common.column.RowBasedFormatter;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.annotation.CommandAlias;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.template.TemplateStorageUtil;
import eu.cloudnetservice.cloudnet.node.template.install.InstallInformation;
import eu.cloudnetservice.cloudnet.node.template.install.ServiceVersion;
import eu.cloudnetservice.cloudnet.node.template.install.ServiceVersionType;
import eu.cloudnetservice.cloudnet.node.util.JavaVersionResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

@CommandAlias("t")
@CommandPermission("cloudnet.command.templates")
@Description("Manages the templates and allows installation of application files")
public final class CommandTemplate {

  private static final RowBasedFormatter<ServiceTemplate> LIST_FORMATTER = RowBasedFormatter.<ServiceTemplate>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Storage", "Prefix", "Name").build())
    .column(ServiceTemplate::storageName)
    .column(ServiceTemplate::prefix)
    .column(ServiceTemplate::name)
    .build();
  private static final RowBasedFormatter<Pair<ServiceVersionType, ServiceVersion>> VERSIONS =
    RowBasedFormatter.<Pair<ServiceVersionType, ServiceVersion>>builder()
      .defaultFormatter(ColumnFormatter.builder()
        .columnTitles("Target", "Name", "Deprecated", "Min Java", "Max Java")
        .build())
      .column(pair -> pair.first().name())
      .column(pair -> pair.second().name())
      .column(pair -> pair.second().deprecated())
      .column(pair -> pair.second().minJavaVersion().orElse(JavaVersion.JAVA_8).name())
      .column(pair -> pair.second().maxJavaVersion().map(JavaVersion::name).orElse("No maximum"))
      .build();

  @Parser(suggestions = "serviceTemplate")
  public ServiceTemplate defaultServiceTemplateParser(CommandContext<CommandSource> $, Queue<String> input) {
    var template = ServiceTemplate.parse(input.remove());
    if (template == null || template.knownStorage() == null) {
      throw new ArgumentNotAvailableException(I18n.trans("ca-question-list-invalid-template"));
    }
    return template;
  }

  @Parser
  public ServiceEnvironmentType defaultServiceEnvironmentTypeParser(CommandContext<?> $, Queue<String> input) {
    return CloudNet.instance().serviceVersionProvider().getEnvironmentType(input.remove())
      .orElseThrow(() -> new ArgumentNotAvailableException("No such version type"));
  }

  @Suggestions("serviceTemplate")
  public List<String> suggestServiceTemplate(CommandContext<CommandSource> $, String input) {
    return CloudNet.instance().localTemplateStorage().templates()
      .stream()
      .map(ServiceTemplate::toString)
      .toList();
  }

  @Parser
  public TemplateStorage defaultTemplateStorageParser(CommandContext<CommandSource> $, Queue<String> input) {
    var templateStorage = CloudNet.instance().templateStorage(input.remove());
    if (templateStorage == null) {
      throw new ArgumentNotAvailableException(I18n.trans("ca-question-list-template-invalid-storage"));
    }

    return templateStorage;
  }

  @Suggestions("templateStorage")
  public List<String> suggestTemplateStorage(CommandContext<CommandSource> $, String input) {
    return CloudNet.instance().availableTemplateStorages()
      .stream()
      .map(Nameable::name)
      .toList();
  }

  @Parser(suggestions = "serviceVersionType")
  public ServiceVersionType defaultVersionTypeParser(CommandContext<CommandSource> $, Queue<String> input) {
    var versionTypeName = input.remove().toLowerCase();
    return CloudNet.instance().serviceVersionProvider().getServiceVersionType(versionTypeName)
      .orElseThrow(() -> new ArgumentNotAvailableException(
        I18n.trans("ca-question-list-invalid-service-version")));
  }

  @Suggestions("serviceVersionType")
  public List<String> suggestServiceVersionType(CommandContext<CommandSource> $, String input) {
    return new ArrayList<>(CloudNet.instance().serviceVersionProvider().serviceVersionTypes().keySet());
  }

  @Parser(suggestions = "version")
  public ServiceVersion defaultVersionParser(CommandContext<CommandSource> context, Queue<String> input) {
    var version = input.remove();
    ServiceVersionType type = context.get("versionType");

    return type.version(version).orElseThrow(
      () -> new ArgumentNotAvailableException(I18n.trans("command-template-invalid-version")));
  }

  @Suggestions("version")
  public List<String> suggestVersions(CommandContext<CommandSource> context, String input) {
    ServiceVersionType type = context.get("versionType");
    return type.versions()
      .stream()
      .filter(ServiceVersion::canRun)
      .map(Nameable::name)
      .toList();
  }

  @CommandMethod("template|t list [storage]")
  public void displayTemplates(CommandSource source, @Argument("storage") TemplateStorage templateStorage) {
    Collection<ServiceTemplate> templates;
    // get all templates if no specific template is given
    if (templateStorage == null) {
      templates = CloudNet.instance().servicesRegistry().services(TemplateStorage.class).stream()
        .flatMap(storage -> storage.templates().stream())
        .toList();
    } else {
      templates = templateStorage.templates();
    }

    source.sendMessage(LIST_FORMATTER.format(templates));
  }

  @CommandMethod("template|t versions|v [versionType]")
  public void displayTemplateVersions(CommandSource source, @Argument("versionType") ServiceVersionType versionType) {
    Collection<Pair<ServiceVersionType, ServiceVersion>> versions;
    if (versionType == null) {
      versions = CloudNet.instance().serviceVersionProvider()
        .serviceVersionTypes()
        .values().stream()
        .flatMap(type -> type.versions().stream()
          .sorted(Comparator.comparing(ServiceVersion::name))
          .map(version -> new Pair<>(type, version)))
        .toList();
    } else {
      versions = CloudNet.instance().serviceVersionProvider().serviceVersionTypes()
        .get(versionType.name().toLowerCase())
        .versions()
        .stream()
        .sorted(Comparator.comparing(ServiceVersion::name))
        .map(version -> new Pair<>(versionType, version))
        .toList();
    }

    source.sendMessage(VERSIONS.format(versions));
  }

  @CommandMethod("template|t install <template> <versionType> <version>")
  public void installTemplate(
    CommandSource source,
    @Argument("template") ServiceTemplate serviceTemplate,
    @Argument("versionType") ServiceVersionType versionType,
    @Argument("version") ServiceVersion serviceVersion,
    @Flag("force") boolean forceInstall,
    @Flag("no-cache") boolean noCache,
    @Flag("executable") @Quoted String executable
  ) {
    var resolvedExecutable = executable == null ? "java" : executable;
    var javaVersion = JavaVersionResolver.resolveFromJavaExecutable(resolvedExecutable);
    if (javaVersion == null) {
      source.sendMessage(I18n.trans("ca-question-list-invalid-java-executable"));
      return;
    }

    if (!versionType.canInstall(serviceVersion, javaVersion)) {
      source.sendMessage(I18n.trans("command-template-install-wrong-java")
        .replace("%version%", versionType.name() + "-" + serviceVersion.name())
        .replace("%java%", javaVersion.name()));
      if (!forceInstall) {
        return;
      }
    }

    CloudNet.instance().mainThread().runTask(() -> {
      source.sendMessage(I18n.trans("command-template-install-try")
        .replace("%version%", versionType.name() + "-" + serviceVersion.name())
        .replace("%template%", serviceTemplate.toString()));

      var installInformation = InstallInformation.builder()
        .serviceVersionType(versionType)
        .serviceVersion(serviceVersion)
        .cacheFiles(!noCache)
        .toTemplate(serviceTemplate)
        .executable(resolvedExecutable.equals("java") ? null : resolvedExecutable)
        .build();

      if (CloudNet.instance().serviceVersionProvider().installServiceVersion(installInformation, forceInstall)) {
        source.sendMessage(I18n.trans("command-template-install-success")
          .replace("%version%", versionType.name() + "-" + serviceVersion.name())
          .replace("%template%", serviceTemplate.toString()));
      } else {
        source.sendMessage(I18n.trans("command-template-install-failed")
          .replace("%version%", versionType.name() + "-" + serviceVersion.name())
          .replace("%template%", serviceTemplate.toString()));
      }
    });

  }

  @CommandMethod("template|t delete|rm|del <template>")
  public void deleteTemplate(CommandSource source, @Argument("template") ServiceTemplate template) {
    var templateStorage = template.storage();
    if (!templateStorage.exists()) {
      source.sendMessage(I18n.trans("command-template-delete-template-not-found")
        .replace("%template%", template.fullName())
        .replace("%storage%", template.storageName()));
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
    var templateStorage = template.storage();
    if (templateStorage.exists()) {
      source.sendMessage(I18n.trans("command-template-create-template-already-exists"));
      return;
    }

    try {
      if (TemplateStorageUtil.createAndPrepareTemplate(template, template.storage(), environmentType)) {
        source.sendMessage(I18n.trans("command-template-create-success")
          .replace("%template%", template.fullName())
          .replace("%storage%", template.storageName()));
      }
    } catch (IOException exception) {
      source.sendMessage(I18n.trans("command-template-create-failed")
        .replace("%template%", template.fullName())
        .replace("%storage%", template.storageName()));
    }
  }

  @CommandMethod("template|t copy|cp <sourceTemplate> <targetTemplate>")
  public void copyTemplate(
    CommandSource source,
    @Argument("sourceTemplate") ServiceTemplate sourceTemplate,
    @Argument("targetTemplate") ServiceTemplate targetTemplate
  ) {
    if (sourceTemplate.equals(targetTemplate)) {
      source.sendMessage(I18n.trans("command-template-copy-same-source-and-target"));
      return;
    }

    var sourceStorage = sourceTemplate.storage();
    var targetStorage = targetTemplate.storage();

    CloudNet.instance().mainThread().runTask(() -> {
      source.sendMessage(I18n.trans("command-template-copy")
        .replace("%sourceTemplate%", sourceTemplate.toString())
        .replace("%targetTemplate%", targetTemplate.toString()));

      targetStorage.delete();
      targetStorage.create();
      try (var stream = sourceStorage.asZipInputStream()) {
        if (stream == null) {
          source.sendMessage(I18n.trans("command-template-copy-failed"));
          return;
        }

        targetStorage.deploy(stream);
        source.sendMessage(I18n.trans("command-template-copy-success")
          .replace("%sourceTemplate%", sourceTemplate.toString())
          .replace("%targetTemplate%", targetTemplate.toString()));
      } catch (IOException exception) {
        source.sendMessage(I18n.trans("command-template-copy-failed"));
      }
    });
  }
}
