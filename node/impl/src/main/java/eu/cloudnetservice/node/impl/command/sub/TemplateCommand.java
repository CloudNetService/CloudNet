/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.command.sub;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.impl.template.TemplateStorageUtil;
import eu.cloudnetservice.node.impl.tick.DefaultTickLoop;
import eu.cloudnetservice.node.impl.version.ServiceVersion;
import eu.cloudnetservice.node.impl.version.ServiceVersionProvider;
import eu.cloudnetservice.node.impl.version.ServiceVersionType;
import eu.cloudnetservice.utils.base.column.ColumnFormatter;
import eu.cloudnetservice.utils.base.column.RowedFormatter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.Nullable;

@Singleton
@CommandAlias("t")
@Permission("cloudnet.command.template")
@Description("command-template-description")
public final class TemplateCommand {

  private static final RowedFormatter<ServiceTemplate> LIST_FORMATTER = RowedFormatter.<ServiceTemplate>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Storage", "Prefix", "Name").build())
    .column(ServiceTemplate::storageName)
    .column(ServiceTemplate::prefix)
    .column(ServiceTemplate::name)
    .build();
  private static final LoadingCache<TemplateStorage, Collection<ServiceTemplate>> STORED_TEMPLATES = Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.SECONDS)
    .build(TemplateStorage::templates);

  private final DefaultTickLoop tickLoop;
  private final TemplateStorageUtil storageUtil;
  private final ServiceRegistry serviceRegistry;
  private final ServiceVersionProvider serviceVersionProvider;
  private final TemplateStorageProvider templateStorageProvider;

  @Inject
  public TemplateCommand(
    @NonNull DefaultTickLoop tickLoop,
    @NonNull TemplateStorageUtil storageUtil,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull ServiceVersionProvider serviceVersionProvider,
    @NonNull TemplateStorageProvider templateStorageProvider
  ) {
    this.tickLoop = tickLoop;
    this.storageUtil = storageUtil;
    this.serviceRegistry = serviceRegistry;
    this.serviceVersionProvider = serviceVersionProvider;
    this.templateStorageProvider = templateStorageProvider;
  }

  @Parser(suggestions = "serviceTemplate")
  public @NonNull ServiceTemplate defaultServiceTemplateParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var template = ServiceTemplate.parse(input.readString());
    if (template == null || template.findStorage() == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-template-not-valid"));
    }
    return template;
  }

  @Suggestions("serviceTemplate")
  public @NonNull Stream<String> suggestServiceTemplate() {
    return this.templateStorageProvider.availableTemplateStorages().stream()
      .map(this.templateStorageProvider::templateStorage)
      .filter(Objects::nonNull)
      .flatMap(storage -> STORED_TEMPLATES.get(storage).stream())
      .map(ServiceTemplate::toString);
  }

  @Parser
  public @NonNull TemplateStorage defaultTemplateStorageParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var storage = input.readString();
    var templateStorage = this.templateStorageProvider.templateStorage(storage);
    if (templateStorage == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-template-storage-not-found", storage));
    }

    return templateStorage;
  }

  @Suggestions("templateStorage")
  public @NonNull Collection<String> suggestTemplateStorage() {
    return this.templateStorageProvider.availableTemplateStorages();
  }

  @Parser(suggestions = "version")
  public @NonNull ServiceVersion defaultVersionParser(
    @NonNull @Service I18n i18n,
    @NonNull CommandInput input,
    @NonNull CommandContext<?> context
  ) {
    var version = input.readString();
    ServiceVersionType type = context.get("versionType");

    var serviceVersion = type.version(version);
    if (serviceVersion == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-template-invalid-version"));
    }

    return serviceVersion;
  }

  @Suggestions("version")
  public @NonNull Stream<String> suggestVersions(@NonNull CommandContext<?> context) {
    ServiceVersionType type = context.get("versionType");
    return type.versions()
      .stream()
      .filter(ServiceVersion::canRun)
      .map(Named::name);
  }

  @Parser(suggestions = "serviceEnvironments")
  public @NonNull ServiceEnvironmentType defaultServiceEnvironmentTypeParser(
    @NonNull @Service I18n i18n,
    @NonNull CommandInput input
  ) {
    var env = input.readString();
    var type = this.serviceVersionProvider.environmentType(env);
    if (type != null) {
      return type;
    }

    throw new ArgumentNotAvailableException(i18n.translate("command-template-environment-not-found", env));
  }

  @Suggestions("serviceEnvironments")
  public @NonNull Set<String> suggestServiceEnvironments() {
    return this.serviceVersionProvider.knownEnvironments().keySet();
  }

  @Command("template|t list [storage]")
  public void displayTemplates(
    @NonNull CommandSource source,
    @Nullable @Argument("storage") TemplateStorage templateStorage
  ) {
    Collection<ServiceTemplate> templates;
    // get all templates if no specific template is given
    if (templateStorage == null) {
      templates = this.serviceRegistry.registrations(TemplateStorage.class).stream()
        .flatMap(provider -> provider.serviceInstance().templates().stream())
        .toList();
    } else {
      templates = templateStorage.templates();
    }

    source.sendMessage(LIST_FORMATTER.format(templates));
  }

  @Command("template|t delete|rm|del <template>")
  public void deleteTemplate(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    var templateStorage = template.storage();
    if (!templateStorage.contains(template)) {
      source.sendMessage(i18n.translate("command-template-delete-template-not-found",
        template.fullName(),
        template.storageName()));
      return;
    }

    templateStorage.delete(template);
    source.sendMessage(i18n.translate("command-template-delete-success", template.toString(), templateStorage.name()));
  }

  @Command("template|t create <template> <environment>")
  public void createTemplate(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("template") ServiceTemplate template,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    var templateStorage = template.storage();
    if (templateStorage.contains(template)) {
      source.sendMessage(i18n.translate("command-template-create-template-already-exists",
        template.fullName(),
        template.storageName()));
      return;
    }

    try {
      if (this.storageUtil.createAndPrepareTemplate(template, template.storage(), environmentType)) {
        source.sendMessage(
          i18n.translate("command-template-create-success", template.fullName(), template.storageName()));
      }
    } catch (IOException exception) {
      source.sendMessage(i18n.translate("command-template-create-failed", template.fullName(), template.storageName()));
    }
  }

  @Command("template|t copy|cp <sourceTemplate> <targetTemplate>")
  public void copyTemplate(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("sourceTemplate") ServiceTemplate sourceTemplate,
    @NonNull @Argument("targetTemplate") ServiceTemplate targetTemplate
  ) {
    if (sourceTemplate.equals(targetTemplate)) {
      source.sendMessage(i18n.translate("command-template-copy-same-source-and-target"));
      return;
    }

    var sourceStorage = sourceTemplate.storage();
    var targetStorage = targetTemplate.storage();

    this.tickLoop.runTask(() -> {
      source.sendMessage(i18n.translate("command-template-copy", sourceTemplate, targetTemplate));

      targetStorage.delete(targetTemplate);
      targetStorage.create(targetTemplate);
      try (var stream = sourceStorage.openZipInputStream(sourceTemplate)) {
        if (stream == null) {
          source.sendMessage(i18n.translate("command-template-copy-failed"));
          return;
        }

        targetStorage.deploy(targetTemplate, stream);
        source.sendMessage(i18n.translate("command-template-copy-success", sourceTemplate, targetTemplate));
      } catch (IOException exception) {
        source.sendMessage(i18n.translate("command-template-copy-failed"));
      }
    });
  }
}
