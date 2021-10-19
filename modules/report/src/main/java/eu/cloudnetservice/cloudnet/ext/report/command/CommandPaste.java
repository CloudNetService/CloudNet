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

package eu.cloudnetservice.cloudnet.ext.report.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.report.CloudNetReportModule;
import eu.cloudnetservice.cloudnet.ext.report.config.PasteService;
import eu.cloudnetservice.cloudnet.ext.report.paste.PasteCreator;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@Description("Upload cloud specific data to a paste service")
@CommandPermission("cloudnet.command.paste")
public final class CommandPaste {

  private final CloudNetReportModule reportModule;

  public CommandPaste(CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
  }

  @Parser(suggestions = "pasteService")
  public PasteService defaultPasteServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();
    return this.reportModule.getReportConfiguration().getPasteServers()
      .stream()
      .filter(service -> service.getName().equalsIgnoreCase(name))
      .findFirst()
      .orElseThrow(
        () -> new ArgumentNotAvailableException(LanguageManager.getMessage("command-paste-paste-service-not-found")));
  }

  @Suggestions("pasteService")
  public List<String> suggestPasteService(CommandContext<CommandSource> $, String input) {
    return this.reportModule.getReportConfiguration().getPasteServers()
      .stream()
      .map(INameable::getName)
      .collect(Collectors.toList());
  }

  @Parser(suggestions = "cloudService")
  public ICloudService singleServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();
    ICloudService cloudService = CloudNet.getInstance().getCloudServiceProvider().getLocalCloudService(name);
    if (cloudService == null) {
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-service-service-not-found"));
    }
    return cloudService;
  }

  @Suggestions("cloudService")
  public List<String> suggestService(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getCloudServiceProvider().getLocalCloudServices()
      .stream()
      .map(service -> service.getServiceId().getName())
      .collect(Collectors.toList());
  }

  @CommandMethod("paste <pasteService> node")
  public void pasteNode(CommandSource source, @Argument("pasteService") PasteService pasteService) {
    PasteCreator pasteCreator = new PasteCreator(pasteService, this.reportModule.getEmitterRegistry());
    NetworkClusterNodeInfoSnapshot selfNode = CloudNet.getInstance().getClusterNodeServerProvider().getSelfNode()
      .getNodeInfoSnapshot();

    String response = pasteCreator.createNodePaste(selfNode);
    if (response == null) {
      source.sendMessage(LanguageManager.getMessage("module-report-command-paste-failed")
        .replace("%url%", pasteService.getServiceUrl()));
    } else {
      source.sendMessage(LanguageManager.getMessage("module-report-command-paste-success")
        .replace("%url%", response));
    }
  }

  @CommandMethod("paste <pasteService> <service>")
  public void pasteServices(
    CommandSource source,
    @Argument("pasteService") PasteService pasteService,
    @Argument("service") ICloudService service
  ) {
    PasteCreator pasteCreator = new PasteCreator(pasteService, this.reportModule.getEmitterRegistry());

    String response = pasteCreator.createServicePaste(service);
    if (response == null) {
      source.sendMessage(LanguageManager.getMessage("module-report-command-paste-failed")
        .replace("%url%", pasteService.getServiceUrl()));
    } else {
      source.sendMessage(LanguageManager.getMessage("module-report-command-paste-success")
        .replace("%url%", response));
    }
  }

}
