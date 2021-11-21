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
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

@CommandAlias("module")
@CommandPermission("cloudnet.commands.modules")
@Description("Manages all available modules and loading new modules after the start")
public final class CommandModules {

  private final IModuleProvider provider = CloudNet.getInstance().getModuleProvider();

  @Parser(name = "modulePath", suggestions = "modulePath")
  public Path modulePathParser(CommandContext<?> $, Queue<String> input) {
    String fileName = input.remove();
    // resolve the path to the module
    Path path = this.provider.getModuleDirectoryPath().resolve(fileName);
    // check if the file exists
    if (Files.notExists(path)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-file-not-found").replace("%file%", fileName));
    }
    return path;
  }

  @Suggestions("modulePath")
  public List<String> suggestModulePath(CommandContext<?> $, String input) {
    Path moduleDirectory = this.provider.getModuleDirectoryPath();
    try {
      return Files.walk(moduleDirectory, 1)
        .filter(path -> !moduleDirectory.equals(path))
        .filter(path -> !Files.isDirectory(path))
        .filter(this::canLoadModule)
        .map(path -> path.getFileName().toString())
        .collect(Collectors.toList());
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }

  @Parser(name = "existingModule", suggestions = "existingModule")
  public IModuleWrapper existingModuleParser(CommandContext<?> $, Queue<String> input) {
    String moduleName = input.remove();
    IModuleWrapper wrapper = this.provider.getModule(moduleName);
    if (wrapper == null) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-loaded").replace("%name%", moduleName));
    }
    return wrapper;
  }

  @Suggestions("existingModule")
  public List<String> suggestExistingModule(CommandContext<?> $, String input) {
    return this.provider.getModules()
      .stream()
      .map(module -> module.getModule().getName())
      .collect(Collectors.toList());
  }

  @Parser(name = "toStartModule", suggestions = "toStartModule")
  public IModuleWrapper loadedModuleParser(CommandContext<?> $, Queue<String> input) {
    String moduleName = input.remove();
    IModuleWrapper wrapper = this.provider.getModule(moduleName);
    if (wrapper == null || !wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.STARTED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-loaded").replace("%name%", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toStartModule")
  public List<String> suggestStartModule(CommandContext<?> $, String input) {
    return this.provider.getModules().stream()
      .filter(wrapper -> wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.STARTED))
      .map(wrapper -> wrapper.getModule().getName())
      .collect(Collectors.toList());
  }

  @Parser(name = "toReloadModule", suggestions = "toReloadModule")
  public IModuleWrapper reloadedModuleParser(CommandContext<?> $, Queue<String> input) {
    String moduleName = input.remove();
    IModuleWrapper wrapper = this.provider.getModule(moduleName);
    if (wrapper == null || !wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.RELOADING)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-started").replace("%name%", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toReloadModule")
  public List<String> suggestReloadModule(CommandContext<?> $, String input) {
    return this.provider.getModules().stream()
      .filter(wrapper -> wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.RELOADING))
      .map(wrapper -> wrapper.getModule().getName())
      .collect(Collectors.toList());
  }

  @Parser(name = "toStopModule", suggestions = "toStopModule")
  public IModuleWrapper stoppedModuleParser(CommandContext<?> $, Queue<String> input) {
    String moduleName = input.remove();
    IModuleWrapper wrapper = this.provider.getModule(moduleName);
    if (wrapper == null || !wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.STOPPED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-started").replace("%name%", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toStopModule")
  public List<String> suggestStopModule(CommandContext<?> $, String input) {
    return this.provider.getModules().stream()
      .filter(wrapper -> wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.STOPPED))
      .map(wrapper -> wrapper.getModule().getName())
      .collect(Collectors.toList());
  }

  @Parser(name = "toUnloadModule", suggestions = "toUnloadModule")
  public IModuleWrapper unloadedModuleParser(CommandContext<?> $, Queue<String> input) {
    String moduleName = input.remove();
    IModuleWrapper wrapper = this.provider.getModule(moduleName);
    if (wrapper == null || !wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.UNLOADED)) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-modules-module-not-stopped").replace("%name%", moduleName));
    }
    return wrapper;
  }

  @Suggestions("toUnloadModule")
  public List<String> suggestUnloadModule(CommandContext<?> $, String input) {
    return this.provider.getModules().stream()
      .filter(wrapper -> wrapper.getModuleLifeCycle().canChangeTo(ModuleLifeCycle.UNLOADED))
      .map(wrapper -> wrapper.getModule().getName())
      .collect(Collectors.toList());
  }

  @CommandMethod("modules|module info <module>")
  public void moduleInfo(
    CommandSource source,
    @Argument(value = "module", parserName = "existingModule") IModuleWrapper module
  ) {
    this.printBasicModuleInfos(source, module);
    source.sendMessage(" - Dependencies: ");
    for (ModuleDependency dependingModule : module.getDependingModules()) {
      source.sendMessage("  - Name: " + dependingModule.getName());
      source.sendMessage("  - Version: " + dependingModule.getVersion());
    }
  }

  @CommandMethod("modules|module list")
  public void listModules(CommandSource source) {
    source.sendMessage("Loaded " + this.provider.getModules().size() + " modules");
    for (IModuleWrapper module : this.provider.getModules()) {
      this.printBasicModuleInfos(source, module);
    }
  }

  @CommandMethod("modules|module load <module>")
  public void loadModule(
    CommandSource source,
    @Argument(value = "module", parserName = "modulePath") @Quoted Path path
  ) {
    // try to load the module
    IModuleWrapper wrapper = this.provider.loadModule(path);
    // if the wrapper is null, the module is already loaded
    if (wrapper == null) {
      source.sendMessage(I18n.trans("command-modules-module-already-loaded"));
    }
  }

  @CommandMethod("modules|module start <module>")
  public void startModule(
    CommandSource source,
    @Argument(value = "module", parserName = "toStartModule") IModuleWrapper wrapper
  ) {
    wrapper.startModule();
  }

  @CommandMethod("modules|module reload [module]")
  public void reloadModule(
    CommandSource source,
    @Argument(value = "module", parserName = "toReloadModule") IModuleWrapper wrapper
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
    @Argument(value = "module", parserName = "toStopModule") IModuleWrapper wrapper
  ) {
    wrapper.stopModule();
  }

  @CommandMethod("modules|module unload <module>")
  public void unloadModule(
    CommandSource source,
    @Argument(value = "module", parserName = "toUnloadModule") IModuleWrapper wrapper
  ) {
    wrapper.unloadModule();
  }

  private boolean canLoadModule(@NotNull Path path) {
    return this.provider.getModules().stream().noneMatch(module -> {
      try {
        return module.getUrl().equals(path.toUri().toURL());
      } catch (MalformedURLException e) {
        return false;
      }
    });
  }

  private void printBasicModuleInfos(@NotNull CommandSource source, @NotNull IModuleWrapper module) {
    source.sendMessage("Module: " + module.getModule().getName());
    source.sendMessage(" - Lifecycle: " + module.getModuleLifeCycle());
    source.sendMessage(" - Version: " + module.getModule().getVersion());
    source.sendMessage(" - Author: " + module.getModuleConfiguration().getAuthor());
    source.sendMessage(" - Description: " + module.getModuleConfiguration().getDescription());
  }
}
