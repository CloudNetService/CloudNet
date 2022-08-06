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
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.JavaVersion;
import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.util.JavaVersionResolver;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionType;
import eu.cloudnetservice.node.version.information.FileSystemVersionInstaller;
import eu.cloudnetservice.node.version.information.TemplateVersionInstaller;
import eu.cloudnetservice.node.version.information.VersionInstaller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("v")
@CommandPermission("cloudnet.command.version")
@Description("command-version-description")
public final class VersionCommand {

  private static final RowBasedFormatter<Pair<ServiceVersionType, ServiceVersion>> VERSIONS =
    RowBasedFormatter.<Pair<ServiceVersionType, ServiceVersion>>builder()
      .defaultFormatter(ColumnFormatter.builder()
        .columnTitles("Target", "Name", "Deprecated", "Min Java", "Max Java")
        .build())
      .column(pair -> pair.first().name())
      .column(pair -> pair.second().name())
      .column(pair -> pair.second().deprecated())
      .column(pair -> pair.second().minJavaVersion().orElse(JavaVersion.JAVA_17).name())
      .column(pair -> pair.second().maxJavaVersion().map(JavaVersion::name).orElse("No maximum"))
      .build();

  @Parser(suggestions = "serviceVersionType")
  public @NonNull ServiceVersionType parseVersionType(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var versionType = Node.instance().serviceVersionProvider().getServiceVersionType(input.remove());
    if (versionType != null) {
      return versionType;
    }

    throw new ArgumentNotAvailableException(I18n.trans("command-template-invalid-version-type"));
  }

  @Suggestions("serviceVersionType")
  public @NonNull List<String> suggestVersionType(@NonNull CommandContext<?> $, @NonNull String input) {
    return new ArrayList<>(Node.instance().serviceVersionProvider().serviceVersionTypes().keySet());
  }

  @Parser(name = "staticServiceDirectory", suggestions = "staticServices")
  public @NonNull Path parseStaticServiceDirectory(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var suppliedName = input.remove();
    var baseDirectory = Node.instance().cloudServiceProvider().persistentServicesDirectory();

    // check for path traversal
    var serviceDirectory = baseDirectory.resolve(suppliedName);
    FileUtil.ensureChild(baseDirectory, serviceDirectory);

    // check if the requested service directory exists
    if (Files.exists(serviceDirectory)) {
      return serviceDirectory;
    }

    throw new ArgumentNotAvailableException(I18n.trans("command-version-static-service-invalid"));
  }

  @Suggestions("staticServices")
  public @NonNull List<String> suggestStaticServices(
    @NonNull CommandContext<?> $,
    @NonNull String input
  ) {
    var baseDirectory = Node.instance().cloudServiceProvider().persistentServicesDirectory();
    try {
      return Files.walk(baseDirectory, 1)
        .filter(Files::isDirectory)
        .filter(path -> !path.equals(baseDirectory)) // prevents the base directory to show up in the suggestions
        .map(Path::getFileName)
        .map(Path::toString)
        .collect(Collectors.toList());
    } catch (IOException exception) {
      return List.of();
    }
  }

  @CommandMethod("version|v list [versionType]")
  public void displayTemplateVersions(
    @NonNull CommandSource source,
    @Nullable @Argument("versionType") ServiceVersionType versionType
  ) {
    Collection<Pair<ServiceVersionType, ServiceVersion>> versions;
    if (versionType == null) {
      versions = Node.instance().serviceVersionProvider()
        .serviceVersionTypes()
        .values().stream()
        .flatMap(type -> type.versions().stream()
          .sorted(Comparator.comparing(ServiceVersion::name))
          .map(version -> new Pair<>(type, version)))
        .toList();
    } else {
      versions = Node.instance().serviceVersionProvider().serviceVersionTypes()
        .get(StringUtil.toLower(versionType.name()))
        .versions()
        .stream()
        .sorted(Comparator.comparing(ServiceVersion::name))
        .map(version -> new Pair<>(versionType, version))
        .toList();
    }

    source.sendMessage(VERSIONS.format(versions));
  }

  @CommandMethod("version|v installtemplate|it <template> <versionType> <version>")
  public void installTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("template") ServiceTemplate serviceTemplate,
    @NonNull @Argument("versionType") ServiceVersionType versionType,
    @NonNull @Argument("version") ServiceVersion serviceVersion,
    @Flag("force") boolean forceInstall,
    @Flag("no-cache") boolean noCache,
    @Nullable @Flag("executable") @Quoted String executable
  ) {
    // try to build the installer based on the supplied information
    var installer = this.buildVersionInstaller(
      source,
      () -> TemplateVersionInstaller.builder().toTemplate(serviceTemplate),
      versionType,
      serviceVersion,
      executable,
      forceInstall,
      noCache);
    if (installer != null) {
      this.executeInstallation(source, installer, forceInstall);
    }
  }

  @CommandMethod("version|v installstatic|is <serviceName> <versionType> <version>")
  public void installStaticService(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "serviceName", parserName = "staticServiceDirectory") Path serviceDirectory,
    @NonNull @Argument("versionType") ServiceVersionType versionType,
    @NonNull @Argument("version") ServiceVersion serviceVersion,
    @Flag("force") boolean forceInstall,
    @Flag("no-cache") boolean noCache,
    @Nullable @Flag("executable") @Quoted String executable
  ) {
    // try to build the installer based on the supplied information
    var installer = this.buildVersionInstaller(
      source,
      () -> FileSystemVersionInstaller.builder().workingDirectory(serviceDirectory),
      versionType,
      serviceVersion,
      executable,
      forceInstall,
      noCache);
    if (installer != null) {
      this.executeInstallation(source, installer, forceInstall);
    }
  }

  private @Nullable VersionInstaller buildVersionInstaller(
    @NonNull CommandSource source,
    @NonNull Supplier<VersionInstaller.Builder<? extends VersionInstaller, ?>> factory,
    @NonNull ServiceVersionType versionType,
    @NonNull ServiceVersion serviceVersion,
    @Nullable String executable,
    boolean forceInstall,
    boolean noCache
  ) {
    // validate java executable
    var resolvedExecutable = executable == null ? "java" : executable;
    var javaVersion = JavaVersionResolver.resolveFromJavaExecutable(resolvedExecutable);
    if (javaVersion == null) {
      source.sendMessage(I18n.trans("command-tasks-setup-question-javacommand-invalid"));
      return null;
    }

    // check if the given version is installable
    var fullVersionName = versionType.name() + "-" + serviceVersion.name();
    if (!versionType.canInstall(serviceVersion, javaVersion)) {
      source.sendMessage(I18n.trans("command-version-install-wrong-java",
        fullVersionName,
        javaVersion.name()));
      // just yolo it - if requested
      if (!forceInstall) {
        return null;
      }
    }

    // build the version installer
    return factory.get()
      .serviceVersionType(versionType)
      .serviceVersion(serviceVersion)
      .cacheFiles(!noCache)
      .executable(resolvedExecutable.equals("java") ? null : resolvedExecutable)
      .build();
  }

  private void executeInstallation(@NonNull CommandSource source, @NonNull VersionInstaller installer, boolean force) {
    Node.instance().mainThread().runTask(() -> {
      source.sendMessage(I18n.trans("command-version-install-try"));

      if (Node.instance().serviceVersionProvider().installServiceVersion(installer, force)) {
        source.sendMessage(I18n.trans("command-version-install-success"));
      } else {
        source.sendMessage(I18n.trans("command-version-install-failed"));
      }
    });
  }
}
