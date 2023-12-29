/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowedFormatter;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.jvm.JavaVersion;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.TickLoop;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.util.JavaVersionResolver;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import eu.cloudnetservice.node.version.ServiceVersionType;
import eu.cloudnetservice.node.version.information.FileSystemVersionInstaller;
import eu.cloudnetservice.node.version.information.TemplateVersionInstaller;
import eu.cloudnetservice.node.version.information.VersionInstaller;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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

@Singleton
@CommandAlias("v")
@CommandPermission("cloudnet.command.version")
@Description("command-version-description")
public final class VersionCommand {

  private static final RowedFormatter<Tuple2<ServiceVersionType, ServiceVersion>> VERSIONS =
    RowedFormatter.<Tuple2<ServiceVersionType, ServiceVersion>>builder()
      .defaultFormatter(ColumnFormatter.builder()
        .columnTitles("Target", "Name", "Deprecated", "Min Java", "Max Java")
        .build())
      .column(pair -> pair.first().name())
      .column(pair -> pair.second().name())
      .column(pair -> pair.second().deprecated())
      .column(pair -> pair.second().minJavaVersion().orElse(JavaVersion.JAVA_21).name())
      .column(pair -> pair.second().maxJavaVersion().map(JavaVersion::name).orElse("No maximum"))
      .build();

  private final TickLoop tickLoop;
  private final CloudServiceManager serviceManager;
  private final ServiceVersionProvider serviceVersionProvider;

  @Inject
  public VersionCommand(
    @NonNull TickLoop tickLoop,
    @NonNull CloudServiceManager serviceManager,
    @NonNull ServiceVersionProvider serviceVersionProvider
  ) {
    this.tickLoop = tickLoop;
    this.serviceManager = serviceManager;
    this.serviceVersionProvider = serviceVersionProvider;
  }

  @Parser(suggestions = "serviceVersionType")
  public @NonNull ServiceVersionType parseVersionType(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var versionType = this.serviceVersionProvider.getServiceVersionType(input.remove());
    if (versionType != null) {
      return versionType;
    }

    throw new ArgumentNotAvailableException(I18n.trans("command-template-invalid-version-type"));
  }

  @Suggestions("serviceVersionType")
  public @NonNull List<String> suggestVersionType(@NonNull CommandContext<?> $, @NonNull String input) {
    return new ArrayList<>(this.serviceVersionProvider.serviceVersionTypes().keySet());
  }

  @Parser(name = "staticServiceDirectory", suggestions = "staticServices")
  public @NonNull Path parseStaticServiceDirectory(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var suppliedName = input.remove();
    var baseDirectory = this.serviceManager.persistentServicesDirectory();

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
    var baseDirectory = this.serviceManager.persistentServicesDirectory();
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
    Collection<Tuple2<ServiceVersionType, ServiceVersion>> versions;
    if (versionType == null) {
      versions = this.serviceVersionProvider
        .serviceVersionTypes()
        .values().stream()
        .flatMap(type -> type.versions().stream()
          .sorted(Comparator.comparing(ServiceVersion::name))
          .map(version -> new Tuple2<>(type, version)))
        .toList();
    } else {
      versions = this.serviceVersionProvider.serviceVersionTypes()
        .get(StringUtil.toLower(versionType.name()))
        .versions()
        .stream()
        .sorted(Comparator.comparing(ServiceVersion::name))
        .map(version -> new Tuple2<>(versionType, version))
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
    this.tickLoop.runTask(() -> {
      source.sendMessage(I18n.trans("command-version-install-try"));

      if (this.serviceVersionProvider.installServiceVersion(installer, force)) {
        source.sendMessage(I18n.trans("command-version-install-success"));
      } else {
        source.sendMessage(I18n.trans("command-version-install-failed"));
      }
    });
  }
}
