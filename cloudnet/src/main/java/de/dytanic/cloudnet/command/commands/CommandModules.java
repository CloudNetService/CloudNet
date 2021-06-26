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

package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandModules extends CommandDefault implements ITabCompleter {

  public CommandModules() {
    super("modules", "module");
  }

  @Deprecated
  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
    IModuleProvider moduleProvider = CloudNetDriver.getInstance().getModuleProvider();
    Collection<IModuleWrapper> moduleWrappers = moduleProvider.getModules();

    if (args.length == 0) {
      sender.sendMessage(
        "Modules(" + moduleWrappers.size() + "): " + Arrays.toString(moduleWrappers.stream()
          .map(moduleWrapper -> moduleWrapper.getModule().getName()).toArray(String[]::new)),
        " ",
        "modules list | group=<name> name=<name> version=<version>"
      );
      return;
    }

    if (args[0].equalsIgnoreCase("list")) {
      for (IModuleWrapper wrapper : moduleWrappers) {
        if (properties.containsKey("group") && !wrapper.getModuleConfiguration().getGroup()
          .contains(properties.get("group"))) {
          continue;
        }
        if (properties.containsKey("name") && !wrapper.getModuleConfiguration().getName()
          .contains(properties.get("name"))) {
          continue;
        }
        if (properties.containsKey("version") && !wrapper.getModuleConfiguration().getVersion()
          .contains(properties.get("version"))) {
          continue;
        }

        this.displayModuleInfo(sender, wrapper);
      }
    }

    IModuleWrapper wrapper = CloudNetDriver.getInstance().getModuleProvider().getModule(args[0]);
    if (wrapper != null) {
      this.displayModuleInfo(sender, wrapper);
    }
  }

  private void displayModuleInfo(ICommandSender sender, IModuleWrapper moduleWrapper) {
    List<String> list = new ArrayList<>();

    list.add("* Module: " +
      moduleWrapper.getModuleConfiguration().getGroup() + ":" +
      moduleWrapper.getModuleConfiguration().getName() + ":" +
      moduleWrapper.getModuleConfiguration().getVersion()
    );

    list.add("* Health status: " + moduleWrapper.getModuleLifeCycle().name());

    if (moduleWrapper.getModuleConfiguration().getAuthor() != null) {
      list.add("* Author: " + moduleWrapper.getModuleConfiguration().getAuthor());
    }

    if (moduleWrapper.getModuleConfiguration().getWebsite() != null) {
      list.add("* Website: " + moduleWrapper.getModuleConfiguration().getWebsite());
    }

    if (moduleWrapper.getModuleConfiguration().getDescription() != null) {
      list.add("* Description: " + moduleWrapper.getModuleConfiguration().getDescription());
    }

    if (moduleWrapper.getModuleConfiguration().getDependencies() != null) {
      list.add(" ");
      list.add("* Dependencies: ");
      for (ModuleDependency moduleDependency : moduleWrapper.getModuleConfiguration().getDependencies()) {
        list.addAll(Arrays.asList(
          "- ",
          "Dependency: " + moduleDependency.getGroup() + ":" + moduleDependency.getName() + ":" + moduleDependency
            .getVersion(),
          (
            moduleDependency.getUrl() != null ?
              "Url: " + moduleDependency.getUrl()
              :
                "Repository: " + moduleDependency.getRepo()
          )
        ));
      }
    }

    if (moduleWrapper.getModuleConfiguration().getProperties() != null) {
      list.add(" ");
      list.add("* Properties: ");
      list.addAll(Arrays.asList(moduleWrapper.getModuleConfiguration().getProperties().toPrettyJson().split("\n")));
    }

    list.add(" ");

    sender.sendMessage(
      "Module: " +
        moduleWrapper.getModuleConfiguration().getGroup() + ":" +
        moduleWrapper.getModuleConfiguration().getName() + ":" +
        moduleWrapper.getModuleConfiguration().getVersion(),
      "Author: " + moduleWrapper.getModuleConfiguration().getAuthor()
    );
    sender.sendMessage(list.toArray(new String[0]));
  }

  @Override
  public Collection<String> complete(String commandLine, String[] args, Properties properties) {
    if (args.length > 1) {
      return null;
    }
    Collection<String> response = CloudNetDriver.getInstance().getModuleProvider().getModules()
      .stream().map(moduleWrapper -> moduleWrapper.getModule().getName()).collect(Collectors.toList());
    response.add("list");
    return response;
  }
}
