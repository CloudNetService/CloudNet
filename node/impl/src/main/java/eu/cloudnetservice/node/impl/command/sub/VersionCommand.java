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

import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.impl.tick.DefaultTickLoop;
import eu.cloudnetservice.node.impl.util.JavaVersionResolver;
import eu.cloudnetservice.node.impl.version.ServiceVersion;
import eu.cloudnetservice.node.impl.version.ServiceVersionProvider;
import eu.cloudnetservice.node.impl.version.ServiceVersionType;
import eu.cloudnetservice.node.impl.version.information.FileSystemVersionInstaller;
import eu.cloudnetservice.node.impl.version.information.TemplateVersionInstaller;
import eu.cloudnetservice.node.impl.version.information.VersionInstaller;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.utils.base.StringUtil;
import eu.cloudnetservice.utils.base.column.ColumnFormatter;
import eu.cloudnetservice.utils.base.column.RowedFormatter;
import eu.cloudnetservice.utils.base.io.FileUtil;
import io.vavr.Tuple2;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.Nullable;

@Singleton
@CommandAlias("v")
@Permission("cloudnet.command.version")
@Description("command-version-description")
public final class VersionCommand {

  private static final RowedFormatter<Tuple2<ServiceVersionType, ServiceVersion>> VERSIONS =
    RowedFormatter.<Tuple2<ServiceVersionType, ServiceVersion>>builder()
      .defaultFormatter(ColumnFormatter.builder()
        .columnTitles("Target", "Name", "Deprecated", "Min Java", "Max Java")
        .build())
      .column(pair -> pair._1().name())
      .column(pair -> pair._2().name())
      .column(pair -> pair._2().deprecated())
      .column(pair -> pair._2().minJavaVersion().orElse(JavaVersion.JAVA_23).name())
      .column(pair -> pair._2().maxJavaVersion().map(JavaVersion::name).orElse("No maximum"))
      .build();

  private final DefaultTickLoop tickLoop;
  private final CloudServiceManager serviceManager;
  private final ServiceVersionProvider serviceVersionProvider;

  @Inject
  public VersionCommand(
    @NonNull DefaultTickLoop tickLoop,
    @NonNull CloudServiceManager serviceManager,
    @NonNull ServiceVersionProvider serviceVersionProvider
  ) {
    this.tickLoop = tickLoop;
    this.serviceManager = serviceManager;
    this.serviceVersionProvider = serviceVersionProvider;
  }

  @Parser(suggestions = "serviceVersionType")
  public @NonNull ServiceVersionType parseVersionType(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var versionType = this.serviceVersionProvider.serviceVersionType(input.readString());
    if (versionType != null) {
      return versionType;
    }

    throw new ArgumentNotAvailableException(i18n.translate("command-template-invalid-version-type"));
  }

  @Suggestions("serviceVersionType")
  public @NonNull Set<String> suggestVersionType() {
    return this.serviceVersionProvider.serviceVersionTypes().keySet();
  }

  @Parser(name = "staticServiceDirectory", suggestions = "staticServices")
  public @NonNull Path parseStaticServiceDirectory(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var suppliedName = input.readString();
    var baseDirectory = this.serviceManager.persistentServicesDirectory();

    // check for path traversal
    var serviceDirectory = baseDirectory.resolve(suppliedName);
    FileUtil.ensureChild(baseDirectory, serviceDirectory);

    // check if the requested service directory exists
    if (Files.exists(serviceDirectory)) {
      return serviceDirectory;
    }

    throw new ArgumentNotAvailableException(i18n.translate("command-version-static-service-invalid"));
  }

  @Suggestions("staticServices")
  public @NonNull Stream<String> suggestStaticServices() {
    var baseDirectory = this.serviceManager.persistentServicesDirectory();
    try {
      return Files.walk(baseDirectory, 1)
        .filter(Files::isDirectory)
        .filter(path -> !path.equals(baseDirectory)) // prevents the base directory to show up in the suggestions
        .map(Path::getFileName)
        .map(Path::toString);
    } catch (IOException exception) {
      return Stream.empty();
    }
  }

  @Command("version|v list [versionType]")
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

  @Command("version|v installtemplate|it <template> <versionType> <version>")
  public void installTemplate(
    @NonNull @Service I18n i18n,
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
      i18n,
      source,
      () -> TemplateVersionInstaller.builder().toTemplate(serviceTemplate),
      versionType,
      serviceVersion,
      executable,
      forceInstall,
      noCache);
    if (installer != null) {
      this.executeInstallation(i18n, source, installer, forceInstall);
    }
  }

  @Command("version|v installstatic|is <serviceName> <versionType> <version>")
  public void installStaticService(
    @NonNull @Service I18n i18n,
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
      i18n,
      source,
      () -> FileSystemVersionInstaller.builder().workingDirectory(serviceDirectory),
      versionType,
      serviceVersion,
      executable,
      forceInstall,
      noCache);
    if (installer != null) {
      this.executeInstallation(i18n, source, installer, forceInstall);
    }
  }

  private @Nullable VersionInstaller buildVersionInstaller(
    @NonNull @Service I18n i18n,
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
      source.sendMessage(i18n.translate("command-tasks-setup-question-javacommand-invalid"));
      return null;
    }

    // check if the given version is installable
    var fullVersionName = versionType.name() + "-" + serviceVersion.name();
    if (!versionType.canInstall(serviceVersion, javaVersion)) {
      source.sendMessage(i18n.translate("command-version-install-wrong-java",
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

  private void executeInstallation(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull VersionInstaller installer,
    boolean force
  ) {
    this.tickLoop.runTask(() -> {
      source.sendMessage(i18n.translate("command-version-install-try"));

      if (this.serviceVersionProvider.installServiceVersion(installer, force)) {
        source.sendMessage(i18n.translate("command-version-install-success"));
      } else {
        source.sendMessage(i18n.translate("command-version-install-failed"));
      }
    });
  }
}
