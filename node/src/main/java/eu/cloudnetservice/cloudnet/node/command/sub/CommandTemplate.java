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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

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
  public @NonNull ServiceTemplate defaultServiceTemplateParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var template = ServiceTemplate.parse(input.remove());
    if (template == null || template.knownStorage() == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-template-not-valid"));
    }
    return template;
  }

  @Suggestions("serviceTemplate")
  public @NonNull List<String> suggestServiceTemplate(@NonNull CommandContext<?> $, @NonNull String input) {
    return CloudNet.instance().localTemplateStorage().templates()
      .stream()
      .map(ServiceTemplate::toString)
      .toList();
  }

  @Parser
  public @NonNull TemplateStorage defaultTemplateStorageParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var storage = input.remove();
    var templateStorage = CloudNet.instance().templateStorage(storage);
    if (templateStorage == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-template-storage-not-found", storage));
    }

    return templateStorage;
  }

  @Suggestions("templateStorage")
  public @NonNull List<String> suggestTemplateStorage(@NonNull CommandContext<?> $, @NonNull String input) {
    return CloudNet.instance().availableTemplateStorages()
      .stream()
      .map(Nameable::name)
      .toList();
  }

  @Parser(suggestions = "serviceVersionType")
  public @NonNull ServiceVersionType defaultVersionTypeParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var versionTypeName = input.remove().toLowerCase();
    return CloudNet.instance().serviceVersionProvider().getServiceVersionType(versionTypeName)
      .orElseThrow(() -> new ArgumentNotAvailableException(
        I18n.trans("command-template-invalid-version-type")));
  }

  @Suggestions("serviceVersionType")
  public @NonNull List<String> suggestServiceVersionType(
    @NonNull CommandContext<?> $,
    @NonNull String input
  ) {
    return new ArrayList<>(CloudNet.instance().serviceVersionProvider().serviceVersionTypes().keySet());
  }

  @Parser(suggestions = "version")
  public @NonNull ServiceVersion defaultVersionParser(
    @NonNull CommandContext<?> context,
    @NonNull Queue<String> input
  ) {
    var version = input.remove();
    ServiceVersionType type = context.get("versionType");

    return type.version(version).orElseThrow(
      () -> new ArgumentNotAvailableException(I18n.trans("command-template-invalid-version")));
  }

  @Suggestions("version")
  public @NonNull List<String> suggestVersions(@NonNull CommandContext<?> context, @NonNull String input) {
    ServiceVersionType type = context.get("versionType");
    return type.versions()
      .stream()
      .filter(ServiceVersion::canRun)
      .map(Nameable::name)
      .toList();
  }

  @Parser(suggestions = "serviceEnvironments")
  public @NonNull ServiceEnvironmentType defaultServiceEnvironmentTypeParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var env = input.remove();
    return CloudNet.instance().serviceVersionProvider().getEnvironmentType(env)
      .orElseThrow(() ->
        new ArgumentNotAvailableException(I18n.trans("command-template-environment-not-found", env)));
  }

  @Suggestions("serviceEnvironments")
  public @NonNull List<String> suggestServiceEnvironments(@NonNull CommandContext<?> context, @NonNull String input) {
    return List.copyOf(CloudNet.instance().serviceVersionProvider().knownEnvironments().keySet());
  }

  @CommandMethod("template|t list [storage]")
  public void displayTemplates(
    @NonNull CommandSource source,
    @Nullable @Argument("storage") TemplateStorage templateStorage
  ) {
    Collection<ServiceTemplate> templates;
    // get all templates if no specific template is given
    if (templateStorage == null) {
      templates = CloudNet.instance().serviceRegistry().providers(TemplateStorage.class).stream()
        .flatMap(storage -> storage.templates().stream())
        .toList();
    } else {
      templates = templateStorage.templates();
    }

    source.sendMessage(LIST_FORMATTER.format(templates));
  }

  @CommandMethod("template|t versions|v [versionType]")
  public void displayTemplateVersions(
    @NonNull CommandSource source,
    @Nullable @Argument("versionType") ServiceVersionType versionType
  ) {
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
    @NonNull CommandSource source,
    @NonNull @Argument("template") ServiceTemplate serviceTemplate,
    @NonNull @Argument("versionType") ServiceVersionType versionType,
    @NonNull @Argument("version") ServiceVersion serviceVersion,
    @Flag("force") boolean forceInstall,
    @Flag("no-cache") boolean noCache,
    @Nullable @Flag("executable") @Quoted String executable
  ) {
    var resolvedExecutable = executable == null ? "java" : executable;
    var javaVersion = JavaVersionResolver.resolveFromJavaExecutable(resolvedExecutable);
    if (javaVersion == null) {
      source.sendMessage(I18n.trans("command-tasks-setup-question-javacommand-invalid"));
      return;
    }

    var fullVersionName = versionType.name() + "-" + serviceVersion.name();

    if (!versionType.canInstall(serviceVersion, javaVersion)) {
      source.sendMessage(I18n.trans("command-template-install-wrong-java",
        fullVersionName,
        javaVersion.name()));
      if (!forceInstall) {
        return;
      }
    }

    CloudNet.instance().mainThread().runTask(() -> {
      source.sendMessage(I18n.trans("command-template-install-try",
        fullVersionName,
        serviceTemplate));

      var installInformation = InstallInformation.builder()
        .serviceVersionType(versionType)
        .serviceVersion(serviceVersion)
        .cacheFiles(!noCache)
        .toTemplate(serviceTemplate)
        .executable(resolvedExecutable.equals("java") ? null : resolvedExecutable)
        .build();

      if (CloudNet.instance().serviceVersionProvider().installServiceVersion(installInformation, forceInstall)) {
        source.sendMessage(I18n.trans("command-template-install-success", fullVersionName, serviceTemplate));
      } else {
        source.sendMessage(I18n.trans("command-template-install-failed", fullVersionName, serviceTemplate));
      }
    });

  }

  @CommandMethod("template|t delete|rm|del <template>")
  public void deleteTemplate(@NonNull CommandSource source, @NonNull @Argument("template") ServiceTemplate template) {
    var templateStorage = template.storage();
    if (!templateStorage.exists()) {
      source.sendMessage(I18n.trans("command-template-delete-template-not-found",
        template.fullName(),
        template.storageName()));
      return;
    }

    templateStorage.delete();
    source.sendMessage(I18n.trans("command-template-delete-success", template.toString(), templateStorage.name()));
  }

  @CommandMethod("template|t create <template> <environment>")
  public void createTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("template") ServiceTemplate template,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    var templateStorage = template.storage();
    if (templateStorage.exists()) {
      source.sendMessage(I18n.trans("command-template-create-template-already-exists"));
      return;
    }

    try {
      if (TemplateStorageUtil.createAndPrepareTemplate(template, template.storage(), environmentType)) {
        source.sendMessage(I18n.trans("command-template-create-success", template.fullName(), template.storageName()));
      }
    } catch (IOException exception) {
      source.sendMessage(I18n.trans("command-template-create-failed", template.fullName(), template.storageName()));
    }
  }

  @CommandMethod("template|t copy|cp <sourceTemplate> <targetTemplate>")
  public void copyTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("sourceTemplate") ServiceTemplate sourceTemplate,
    @NonNull @Argument("targetTemplate") ServiceTemplate targetTemplate
  ) {
    if (sourceTemplate.equals(targetTemplate)) {
      source.sendMessage(I18n.trans("command-template-copy-same-source-and-target"));
      return;
    }

    var sourceStorage = sourceTemplate.storage();
    var targetStorage = targetTemplate.storage();

    CloudNet.instance().mainThread().runTask(() -> {
      source.sendMessage(I18n.trans("command-template-copy", sourceTemplate, targetTemplate));

      targetStorage.delete();
      targetStorage.create();
      try (var stream = sourceStorage.asZipInputStream()) {
        if (stream == null) {
          source.sendMessage(I18n.trans("command-template-copy-failed"));
          return;
        }

        targetStorage.deploy(stream);
        source.sendMessage(I18n.trans("command-template-copy-success", sourceTemplate, targetTemplate));
      } catch (IOException exception) {
        source.sendMessage(I18n.trans("command-template-copy-failed"));
      }
    });
  }
}
