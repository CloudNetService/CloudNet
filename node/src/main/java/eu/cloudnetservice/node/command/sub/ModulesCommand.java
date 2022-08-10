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
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.ext.updater.util.ChecksumUtil;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.console.animation.progressbar.ConsoleProgressWrappers;
import eu.cloudnetservice.node.module.ModuleEntry;
import eu.cloudnetservice.node.module.ModulesHolder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("module")
@CommandPermission("cloudnet.command.modules")
@Description("command-modules-description")
public final class ModulesCommand {

  private static final Logger LOGGER = LogManager.logger(ModulesCommand.class);

  private static final RowBasedFormatter<ModuleWrapper> MODULES_FORMATTER = RowBasedFormatter.<ModuleWrapper>builder()
    .defaultFormatter(ColumnFormatter.builder()
      .columnTitles("Name", "Version", "Author", "Lifecycle", "Description")
      .build())
    .column(wrapper -> wrapper.module().name())
    .column(wrapper -> wrapper.module().version())
    .column(wrapper -> {
      var author = wrapper.moduleConfiguration().author();
      return author == null ? "No author provided" : author;
    })
    .column(ModuleWrapper::moduleLifeCycle)
    .column(wrapper -> {
      var description = wrapper.moduleConfiguration().description();
      return description == null ? "No description provided" : description;
    })
    .build();
  private static final RowBasedFormatter<ModuleEntry> MODULE_ENTRY_FORMATTER = RowBasedFormatter.<ModuleEntry>builder()
    .defaultFormatter(ColumnFormatter.builder()
      .columnTitles("Name", "Author", "Description", "Website")
      .build())
    .column(ModuleEntry::name)
    .column(entry -> String.join(", ", entry.maintainers()))
    .column(ModuleEntry::description)
    .column(ModuleEntry::website)
    .build();

  private final ModuleProvider provider = Node.instance().moduleProvider();
  private final ModulesHolder availableModules = Node.instance().modulesHolder();

  @Parser(name = "modulePath", suggestions = "modulePath")
  public @NonNull Path modulePathParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var fileName = input.remove();
    // resolve the path to the module
    var path = this.provider.moduleDirectoryPath().resolve(fileName);
    // check if the file exists
    if (Files.notExists(path)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-modules-module-file-not-found", fileName));
    }
    // dont allow directories
    if (Files.isDirectory(path)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-modules-module-not-a-file", fileName));
    }

    return path;
  }

  @Suggestions("modulePath")
  public @NonNull List<String> suggestModulePath(@NonNull CommandContext<?> $, @NonNull String input) {
    var moduleDirectory = this.provider.moduleDirectoryPath();
    try {
      return Files.walk(moduleDirectory, 1)
        .filter(path -> !moduleDirectory.equals(path))
        .filter(path -> !Files.isDirectory(path))
        .filter(this::canLoadModule)
        .map(path -> path.getFileName().toString())
        .toList();
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }

  @Parser(name = "existingModule", suggestions = "existingModule")
  public @NonNull ModuleWrapper existingModuleParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-loaded", moduleName));
    }
    return wrapper;
  }

  @Suggestions("existingModule")
  public @NonNull List<String> suggestExistingModule(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.provider.modules()
      .stream()
      .map(module -> module.module().name())
      .toList();
  }

  @Parser(name = "toStartModule", suggestions = "toStartModule")
  public @NonNull ModuleWrapper loadedModuleParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null || !wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STARTED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-loaded", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toStartModule")
  public @NonNull List<String> suggestStartModule(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STARTED))
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "toReloadModule", suggestions = "toReloadModule")
  public @NonNull ModuleWrapper reloadedModuleParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null || !wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.RELOADING)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-started", moduleName));
    }

    if (wrapper.moduleConfiguration().runtimeModule()) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-runtime-module", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toReloadModule")
  public @NonNull List<String> suggestReloadModule(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.RELOADING))
      .filter(wrapper -> !wrapper.moduleConfiguration().runtimeModule())
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "toStopModule", suggestions = "toStopModule")
  public @NonNull ModuleWrapper stoppedModuleParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null || !wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STOPPED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-started", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toStopModule")
  public @NonNull List<String> suggestStopModule(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STOPPED))
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "toUnloadModule", suggestions = "toUnloadModule")
  public @NonNull ModuleWrapper unloadedModuleParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null || !wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.UNLOADED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-stopped", moduleName));
    }
    // runtime modules are unloaded on cloud stop only
    if (wrapper.moduleConfiguration().runtimeModule()) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-runtime-module", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toUnloadModule")
  public @NonNull List<String> suggestUnloadModule(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.UNLOADED))
      .filter(wrapper -> !wrapper.moduleConfiguration().runtimeModule())
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "availableModule", suggestions = "availableModules")
  public @NonNull ModuleEntry availableModuleParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var name = input.remove();
    return this.availableModules
      .findByName(name)
      .orElseThrow(
        () -> new ArgumentNotAvailableException(I18n.trans("command-modules-no-such-installable-module", name)));
  }

  @Suggestions("availableModules")
  public @NonNull List<String> suggestAvailableModules(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.availableModules.entries().stream().map(ModuleEntry::name).toList();
  }

  @CommandMethod("modules|module info <module>")
  public void moduleInfo(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "existingModule") ModuleWrapper module
  ) {
    this.printBasicModuleInfos(source, module);
    source.sendMessage(" - Dependencies: ");
    for (var dependingModule : module.dependingModules()) {
      source.sendMessage("  - Name: " + dependingModule.name());
      source.sendMessage("  - Version: " + dependingModule.version());
    }
  }

  @CommandMethod("modules|module list")
  public void listModules(@NonNull CommandSource source) {
    source.sendMessage(MODULES_FORMATTER.format(this.provider.modules()));
  }

  @CommandMethod("modules|module available")
  public void listAvailableModules(@NonNull CommandSource source) {
    source.sendMessage(MODULE_ENTRY_FORMATTER.format(this.availableModules.entries()));
  }

  @CommandMethod("modules|module load <module>")
  public void loadModule(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "modulePath") @Quoted Path path
  ) {
    // try to load the module
    var wrapper = this.provider.loadModule(path);
    // if the wrapper is null, the module is already loaded
    if (wrapper == null) {
      source.sendMessage(I18n.trans("command-modules-module-already-loaded", path));
    }
  }

  @CommandMethod("modules|module install <module>")
  public void installModule(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "availableModule") @Greedy ModuleEntry entry,
    @Flag(value = "noChecksumValidation") boolean noChecksumValidation
  ) {
    // check if all modules the module is depending on are present
    var missingModules = entry.dependingModules().stream()
      .filter(depend -> this.provider.module(depend) == null)
      .collect(Collectors.toSet());
    if (!missingModules.isEmpty()) {
      source.sendMessage(I18n.trans("command-modules-install-missing-depend",
        entry.name(),
        String.join(", ", missingModules)));
      return;
    }

    // download the module
    var target = this.provider.moduleDirectoryPath().resolve(entry.name() + ".jar");
    ConsoleProgressWrappers.wrapDownload(entry.url(), stream -> FileUtil.copy(stream, target));

    // validate the downloaded file
    var checksum = ChecksumUtil.fileShaSum(target);
    if (!Node.instance().dev() && !checksum.equals(entry.sha3256())) {
      // the checksum validation skip is only available for official modules
      if (entry.official() && noChecksumValidation) {
        source.sendMessage(I18n.trans("command-module-skipping-checksum-fail", entry.name()));
      } else {
        // remove the suspicious file
        FileUtil.delete(target);
        // send a message that the validation can be skipped if the module is official
        if (entry.official()) {
          source.sendMessage(I18n.trans("command-modules-checksum-validation-skippable", entry.name()));
        } else {
          source.sendMessage(I18n.trans("cloudnet-install-modules-invalid-checksum", entry.name()));
        }
        return;
      }
    }

    // load the module
    var wrapper = this.provider.loadModule(target);
    if (wrapper == null) {
      source.sendMessage(I18n.trans("command-modules-module-already-loaded", target));
      return;
    }

    // start the module
    wrapper.startModule();
    source.sendMessage(I18n.trans("command-modules-module-installed", wrapper.moduleConfiguration().name()));
  }

  @CommandMethod("modules|module start <module>")
  public void startModule(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "toStartModule") ModuleWrapper wrapper
  ) {
    wrapper.startModule();
  }

  @CommandMethod("modules|module reload [module]")
  public void reloadModule(
    @NonNull CommandSource source,
    @Nullable @Argument(value = "module", parserName = "toReloadModule") ModuleWrapper wrapper
  ) {
    if (wrapper != null) {
      wrapper.reloadModule();
    } else {
      this.provider.reloadAll();
    }
  }

  @CommandMethod("modules|module stop <module>")
  public void stopModule(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "toStopModule") ModuleWrapper wrapper
  ) {
    wrapper.stopModule();
  }

  @CommandMethod("modules|module unload <module>")
  public void unloadModule(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "toUnloadModule") ModuleWrapper wrapper
  ) {
    wrapper.unloadModule();
  }

  @CommandMethod("modules|module uninstall <module>")
  public void uninstallModule(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "existingModule") ModuleWrapper wrapper
  ) {
    wrapper.stopModule();
    wrapper.unloadModule();
    try {
      Files.delete(Path.of(wrapper.uri()));
      source.sendMessage(I18n.trans("command-modules-module-uninstall", wrapper.module().name()));
    } catch (IOException exception) {
      source.sendMessage(I18n.trans("command-modules-module-uninstall-failed", wrapper.module().name()));
      LOGGER.severe("Exception while uninstalling module %s", exception, wrapper.module().name());
    }
  }

  private boolean canLoadModule(@NonNull Path path) {
    return this.provider.modules().stream().noneMatch(module -> {
      try {
        return module.url().equals(path.toUri().toURL());
      } catch (MalformedURLException e) {
        return false;
      }
    });
  }

  private void printBasicModuleInfos(@NonNull CommandSource source, @NonNull ModuleWrapper module) {
    source.sendMessage("Module: " + module.module().name());
    source.sendMessage(" - Lifecycle: " + module.moduleLifeCycle());
    source.sendMessage(" - Version: " + module.module().version());
    source.sendMessage(" - Author: " + module.moduleConfiguration().author());
    source.sendMessage(" - Description: " + module.moduleConfiguration().description());
  }
}
