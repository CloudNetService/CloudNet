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

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.Collection;
import java.util.stream.Collectors;

public class CommandScreen extends SubCommandHandler {

  public CommandScreen() {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            Collection<String> services = CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()
              .stream()
              .filter(cloudService -> cloudService.getServiceConsoleLogCache().isScreenEnabled())
              .map(cloudService -> cloudService.getServiceId().getName())
              .collect(Collectors.toSet());

            if (services.isEmpty()) {
              sender.sendMessage(LanguageManager.getMessage("command-screen-list-no-screen"));
              return;
            }

            sender.sendMessage(
              LanguageManager.getMessage("command-screen-list").replace("%screens%", String.join(", ", services)));
          },
          anyStringIgnoreCase("list", "l")
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            Collection<String> services = CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()
              .stream()
              .filter(cloudService -> {
                if (cloudService.getServiceConsoleLogCache().isScreenEnabled()) {
                  cloudService.getServiceConsoleLogCache().setScreenEnabled(false);
                  return true;
                }
                return false;
              })
              .map(cloudService -> cloudService.getServiceId().getName())
              .collect(Collectors.toSet());

            if (services.isEmpty()) {
              sender.sendMessage(LanguageManager.getMessage("command-screen-list-no-screen"));
              return;
            }

            sender.sendMessage(
              LanguageManager.getMessage("command-screen-disabled").replace("%screens%", String.join(", ", services)));
          },
          anyStringIgnoreCase("disableAll", "d")
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            String name = (String) args.argument("local service name").get();

            ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceByNameOrUniqueId(name);

            if (serviceInfoSnapshot == null) {
              return;
            }

            ICloudService cloudService = CloudNet.getInstance().getCloudServiceManager()
              .getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

            if (cloudService == null) {
              return;
            }

            if (sender instanceof ConsoleCommandSender) {
              boolean enabled = !cloudService.getServiceConsoleLogCache().isScreenEnabled();
              cloudService.getServiceConsoleLogCache().setScreenEnabled(enabled);

              if (enabled) {
                for (String input : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
                  CloudNet.getInstance().getLogger()
                    .log(LogLevel.INFO, "[" + cloudService.getServiceId().getName() + "] " + input);
                }

                sender.sendMessage(LanguageManager.getMessage("command-screen-enable-for-service")
                  .replace("%name%", cloudService.getServiceId().getName())
                  .replace("%uniqueId%", cloudService.getServiceId().getUniqueId().toString().split("-")[0])
                );
              } else {
                sender.sendMessage(LanguageManager.getMessage("command-screen-disable-for-service")
                  .replace("%name%", cloudService.getServiceId().getName())
                  .replace("%uniqueId%", cloudService.getServiceId().getUniqueId().toString().split("-")[0])
                );
              }
              return;
            }

            for (String input : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
              sender.sendMessage("[" + cloudService.getServiceId().getName() + "] " + input);
            }
          },
          anyStringIgnoreCase("toggle", "t"),
          dynamicString(
            "local service name",
            () -> CloudNet.getInstance().getCloudServiceManager().getLocalCloudServices()
              .stream()
              .map(ICloudService::getServiceId)
              .map(ServiceId::getName)
              .collect(Collectors.toList())
          )
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            String line = (String) args.argument("command").get();

            Collection<String> targetServiceNames = CloudNet.getInstance().getCloudServiceManager().getCloudServices()
              .values().stream()
              .filter(cloudService -> {
                if (cloudService.getServiceConsoleLogCache().isScreenEnabled()) {
                  cloudService.runCommand(line);
                  return true;
                }
                return false;
              })
              .map(cloudService -> cloudService.getServiceId().getName())
              .collect(Collectors.toSet());

            if (targetServiceNames.isEmpty()) {
              sender.sendMessage(LanguageManager.getMessage("command-screen-write-no-screen"));
            } else {
              sender.sendMessage(LanguageManager.getMessage("command-screen-write-success")
                .replace("%command%", line)
                .replace("%targets%", String.join(", ", targetServiceNames)));
            }
          },
          subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length).setMaxArgs(Integer.MAX_VALUE),
          anyStringIgnoreCase("write", "send"),
          dynamicString("command")
        )

        .getSubCommands(),
      "screen", "scr", "console"
    );

    super.permission = "cloudnet.command.screen";
    super.description = LanguageManager.getMessage("command-description-screen");
  }
}
