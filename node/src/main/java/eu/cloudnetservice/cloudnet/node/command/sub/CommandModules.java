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
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.cloudnet.common.column.ColumnFormatter;
import eu.cloudnetservice.cloudnet.common.column.RowBasedFormatter;
import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleProvider;
import eu.cloudnetservice.cloudnet.driver.module.ModuleWrapper;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.annotation.CommandAlias;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.console.animation.progressbar.ConsoleProgressWrappers;
import eu.cloudnetservice.cloudnet.node.module.ModuleEntry;
import eu.cloudnetservice.cloudnet.node.module.ModulesHolder;
import eu.cloudnetservice.ext.updater.util.ChecksumUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import lombok.NonNull;

@CommandAlias("module")
@CommandPermission("cloudnet.commands.modules")
@Description("Manages all available modules and loading new modules after the start")
public final class CommandModules {

  private static final RowBasedFormatter<ModuleWrapper> MODULES_FORMATTER = RowBasedFormatter.<ModuleWrapper>builder()
    .defaultFormatter(ColumnFormatter.builder()
      .columnTitles("Name", "Version", "Author", "Lifecycle", "Description")
      .build())
    .column(wrapper -> wrapper.module().name())
    .column(wrapper -> wrapper.module().version())
    .column(wrapper -> wrapper.moduleConfiguration().author())
    .column(ModuleWrapper::moduleLifeCycle)
    .column(wrapper -> wrapper.moduleConfiguration().description())
    .build();
  private static final RowBasedFormatter<ModuleEntry> MODULE_ENTRY_FORMATTER = RowBasedFormatter.<ModuleEntry>builder()
    .defaultFormatter(ColumnFormatter.builder()
      .columnTitles("Name", "Version", "Author", "Description", "Official")
      .build())
    .column(ModuleEntry::name)
    .column(ModuleEntry::version)
    .column(entry -> String.join(", ", entry.maintainers()))
    .column(ModuleEntry::description)
    .column(entry -> entry.maintainers().contains("CloudNetService"))
    .build();

  private final ModuleProvider provider = CloudNet.instance().moduleProvider();
  private final ModulesHolder availableModules = CloudNet.instance().modulesHolder();

  @Parser(name = "modulePath", suggestions = "modulePath")
  public Path modulePathParser(CommandContext<?> $, Queue<String> input) {
    var fileName = input.remove();
    // resolve the path to the module
    var path = this.provider.moduleDirectoryPath().resolve(fileName);
    // check if the file exists
    if (Files.notExists(path)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-file-not-found", fileName));
    }
    return path;
  }

  @Suggestions("modulePath")
  public List<String> suggestModulePath(CommandContext<?> $, String input) {
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
  public ModuleWrapper existingModuleParser(CommandContext<?> $, Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-loaded", moduleName));
    }
    return wrapper;
  }

  @Suggestions("existingModule")
  public List<String> suggestExistingModule(CommandContext<?> $, String input) {
    return this.provider.modules()
      .stream()
      .map(module -> module.module().name())
      .toList();
  }

  @Parser(name = "toStartModule", suggestions = "toStartModule")
  public ModuleWrapper loadedModuleParser(CommandContext<?> $, Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null || !wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STARTED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-loaded", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toStartModule")
  public List<String> suggestStartModule(CommandContext<?> $, String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STARTED))
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "toReloadModule", suggestions = "toReloadModule")
  public ModuleWrapper reloadedModuleParser(CommandContext<?> $, Queue<String> input) {
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
  public List<String> suggestReloadModule(CommandContext<?> $, String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.RELOADING))
      .filter(wrapper -> !wrapper.moduleConfiguration().runtimeModule())
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "toStopModule", suggestions = "toStopModule")
  public ModuleWrapper stoppedModuleParser(CommandContext<?> $, Queue<String> input) {
    var moduleName = input.remove();
    var wrapper = this.provider.module(moduleName);
    if (wrapper == null || !wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STOPPED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-started", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toStopModule")
  public List<String> suggestStopModule(CommandContext<?> $, String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STOPPED))
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "toUnloadModule", suggestions = "toUnloadModule")
  public ModuleWrapper unloadedModuleParser(CommandContext<?> $, Queue<String> input) {
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
  public List<String> suggestUnloadModule(CommandContext<?> $, String input) {
    return this.provider.modules().stream()
      .filter(wrapper -> wrapper.moduleLifeCycle().canChangeTo(ModuleLifeCycle.UNLOADED))
      .filter(wrapper -> !wrapper.moduleConfiguration().runtimeModule())
      .map(wrapper -> wrapper.module().name())
      .toList();
  }

  @Parser(name = "availableModule", suggestions = "availableModules")
  public ModuleEntry availableModuleParser(CommandContext<?> $, Queue<String> input) {
    var name = input.remove();
    return this.availableModules
      .findByName(name)
      .orElseThrow(
        () -> new ArgumentNotAvailableException(I18n.trans("command-modules-no-such-installable-module", name)));
  }

  @Suggestions("availableModules")
  public List<String> suggestAvailableModules(CommandContext<?> $, String input) {
    return this.availableModules.entries().stream().map(ModuleEntry::name).toList();
  }

  @CommandMethod("modules|module info <module>")
  public void moduleInfo(
    CommandSource source,
    @Argument(value = "module", parserName = "existingModule") ModuleWrapper module
  ) {
    this.printBasicModuleInfos(source, module);
    source.sendMessage(" - Dependencies: ");
    for (var dependingModule : module.dependingModules()) {
      source.sendMessage("  - Name: " + dependingModule.name());
      source.sendMessage("  - Version: " + dependingModule.version());
    }
  }

  @CommandMethod("modules|module list")
  public void listModules(CommandSource source) {
    source.sendMessage(MODULES_FORMATTER.format(this.provider.modules()));
  }

  @CommandMethod("modules|module available")
  public void listAvailableModules(CommandSource source) {
    source.sendMessage(MODULE_ENTRY_FORMATTER.format(this.availableModules.entries()));
  }

  @CommandMethod("modules|module load <module>")
  public void loadModule(
    CommandSource source,
    @Argument(value = "module", parserName = "modulePath") @Quoted Path path
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
    CommandSource source,
    @Argument(value = "module", parserName = "availableModule") @Greedy ModuleEntry entry
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
    ConsoleProgressWrappers.wrapDownload(entry.url(), stream -> FileUtils.copy(stream, target));

    // validate the downloaded file
    var checksum = ChecksumUtils.fileShaSum(target);
    if (!checksum.equals(entry.sha3256())) {
      FileUtils.delete(target);
      source.sendMessage(I18n.trans("cloudnet-install-modules-invalid-checksum", entry.name()));
      return;
    }

    // load the module
    var wrapper = this.provider.loadModule(target);
    if (wrapper == null) {
      source.sendMessage(I18n.trans("command-modules-module-already-loaded"));
      return;
    }

    // start the module
    wrapper.startModule();
    source.sendMessage(I18n.trans("command-modules-module-installed", wrapper.moduleConfiguration().name()));
  }

  @CommandMethod("modules|module start <module>")
  public void startModule(
    CommandSource source,
    @Argument(value = "module", parserName = "toStartModule") ModuleWrapper wrapper
  ) {
    wrapper.startModule();
  }

  @CommandMethod("modules|module reload [module]")
  public void reloadModule(
    CommandSource source,
    @Argument(value = "module", parserName = "toReloadModule") ModuleWrapper wrapper
  ) {
    if (wrapper != null) {
      wrapper.reloadModule();
    } else {
      this.provider.reloadAll();
    }
  }

  @CommandMethod("modules|module stop <module>")
  public void stopModule(
    CommandSource source,
    @Argument(value = "module", parserName = "toStopModule") ModuleWrapper wrapper
  ) {
    wrapper.stopModule();
  }

  @CommandMethod("modules|module unload <module>")
  public void unloadModule(
    CommandSource source,
    @Argument(value = "module", parserName = "toUnloadModule") ModuleWrapper wrapper
  ) {
    wrapper.unloadModule();
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
