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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.template.TemplateStorageUtil;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionType;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("t")
@CommandPermission("cloudnet.command.template")
@Description("command-template-description")
public final class TemplateCommand {

  private static final RowBasedFormatter<ServiceTemplate> LIST_FORMATTER = RowBasedFormatter.<ServiceTemplate>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Storage", "Prefix", "Name").build())
    .column(ServiceTemplate::storageName)
    .column(ServiceTemplate::prefix)
    .column(ServiceTemplate::name)
    .build();
  private static final LoadingCache<TemplateStorage, Collection<ServiceTemplate>> STORED_TEMPLATES = Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.SECONDS)
    .build(TemplateStorage::templates);

  @Parser(suggestions = "serviceTemplate")
  public @NonNull ServiceTemplate defaultServiceTemplateParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var template = ServiceTemplate.parse(input.remove());
    if (template == null || template.findStorage() == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-template-not-valid"));
    }
    return template;
  }

  @Suggestions("serviceTemplate")
  public @NonNull List<String> suggestServiceTemplate(@NonNull CommandContext<?> $, @NonNull String input) {
    return Node.instance().templateStorageProvider().availableTemplateStorages().stream()
      .map(storage -> Node.instance().templateStorageProvider().templateStorage(storage))
      .filter(Objects::nonNull)
      .flatMap(storage -> STORED_TEMPLATES.get(storage).stream())
      .map(ServiceTemplate::toString)
      .toList();
  }

  @Parser
  public @NonNull TemplateStorage defaultTemplateStorageParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var storage = input.remove();
    var templateStorage = Node.instance().templateStorageProvider().templateStorage(storage);
    if (templateStorage == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-template-storage-not-found", storage));
    }

    return templateStorage;
  }

  @Suggestions("templateStorage")
  public @NonNull List<String> suggestTemplateStorage(@NonNull CommandContext<?> $, @NonNull String input) {
    return List.copyOf(Node.instance().templateStorageProvider().availableTemplateStorages());
  }

  @Parser(suggestions = "version")
  public @NonNull ServiceVersion defaultVersionParser(
    @NonNull CommandContext<?> context,
    @NonNull Queue<String> input
  ) {
    var version = input.remove();
    ServiceVersionType type = context.get("versionType");

    var serviceVersion = type.version(version);
    if (serviceVersion == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-template-invalid-version"));
    }

    return serviceVersion;
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
    var type = Node.instance().serviceVersionProvider().getEnvironmentType(env);
    if (type != null) {
      return type;
    }

    throw new ArgumentNotAvailableException(I18n.trans("command-template-environment-not-found", env));
  }

  @Suggestions("serviceEnvironments")
  public @NonNull List<String> suggestServiceEnvironments(@NonNull CommandContext<?> context, @NonNull String input) {
    return List.copyOf(Node.instance().serviceVersionProvider().knownEnvironments().keySet());
  }

  @CommandMethod("template|t list [storage]")
  public void displayTemplates(
    @NonNull CommandSource source,
    @Nullable @Argument("storage") TemplateStorage templateStorage
  ) {
    Collection<ServiceTemplate> templates;
    // get all templates if no specific template is given
    if (templateStorage == null) {
      templates = Node.instance().serviceRegistry().providers(TemplateStorage.class).stream()
        .flatMap(storage -> storage.templates().stream())
        .toList();
    } else {
      templates = templateStorage.templates();
    }

    source.sendMessage(LIST_FORMATTER.format(templates));
  }

  @CommandMethod("template|t delete|rm|del <template>")
  public void deleteTemplate(@NonNull CommandSource source, @NonNull @Argument("template") ServiceTemplate template) {
    var templateStorage = template.storage();
    if (!templateStorage.contains(template)) {
      source.sendMessage(I18n.trans("command-template-delete-template-not-found",
        template.fullName(),
        template.storageName()));
      return;
    }

    templateStorage.delete(template);
    source.sendMessage(I18n.trans("command-template-delete-success", template.toString(), templateStorage.name()));
  }

  @CommandMethod("template|t create <template> <environment>")
  public void createTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("template") ServiceTemplate template,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    var templateStorage = template.storage();
    if (templateStorage.contains(template)) {
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

    Node.instance().mainThread().runTask(() -> {
      source.sendMessage(I18n.trans("command-template-copy", sourceTemplate, targetTemplate));

      targetStorage.delete(targetTemplate);
      targetStorage.create(targetTemplate);
      try (var stream = sourceStorage.openZipInputStream(sourceTemplate)) {
        if (stream == null) {
          source.sendMessage(I18n.trans("command-template-copy-failed"));
          return;
        }

        targetStorage.deploy(targetTemplate, stream);
        source.sendMessage(I18n.trans("command-template-copy-success", sourceTemplate, targetTemplate));
      } catch (IOException exception) {
        source.sendMessage(I18n.trans("command-template-copy-failed"));
      }
    });
  }
}
