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
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.sun.management.HotSpotDiagnosticMXBean;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.report.CloudNetReportModule;
import eu.cloudnetservice.cloudnet.ext.report.config.PasteService;
import eu.cloudnetservice.cloudnet.ext.report.paste.PasteCreator;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

@CommandAlias("paste")
@CommandPermission("cloudnet.command.paste")
@Description("Upload cloud specific data to a paste service")
public final class CommandReport {

  private static final Logger LOGGER = LogManager.getLogger(CommandReport.class);

  private final CloudNetReportModule reportModule;

  public CommandReport(CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
  }

  @Parser(suggestions = "pasteService")
  public PasteService defaultPasteServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();
    return this.reportModule.getReportConfiguration().pasteServers()
      .stream()
      .filter(service -> service.name().equalsIgnoreCase(name))
      .findFirst()
      .orElseThrow(
        () -> new ArgumentNotAvailableException(I18n.trans("command-paste-paste-service-not-found")));
  }

  @Suggestions("pasteService")
  public List<String> suggestPasteService(CommandContext<CommandSource> $, String input) {
    return this.reportModule.getReportConfiguration().pasteServers()
      .stream()
      .map(INameable::name)
      .collect(Collectors.toList());
  }

  @Parser(suggestions = "cloudService")
  public ICloudService singleServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();
    var cloudService = CloudNet.getInstance().cloudServiceProvider().getLocalCloudService(name);
    if (cloudService == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-service-not-found"));
    }
    return cloudService;
  }

  @Suggestions("cloudService")
  public List<String> suggestService(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().cloudServiceProvider().getLocalCloudServices()
      .stream()
      .map(service -> service.getServiceId().name())
      .collect(Collectors.toList());
  }

  @CommandMethod("report|paste node [pasteService]")
  public void pasteNode(CommandSource source, @Argument("pasteService") PasteService pasteService) {
    var pasteCreator = new PasteCreator(this.fallbackPasteService(pasteService),
      this.reportModule.getEmitterRegistry());
    var selfNode = CloudNet.getInstance().getClusterNodeServerProvider().getSelfNode()
      .getNodeInfoSnapshot();

    var response = pasteCreator.createNodePaste(selfNode);
    if (response == null) {
      source.sendMessage(I18n.trans("module-report-command-paste-failed")
        .replace("%url%", pasteService.serviceUrl()));
    } else {
      source.sendMessage(I18n.trans("module-report-command-paste-success")
        .replace("%url%", response));
    }
  }

  @CommandMethod("report|paste service <service> [pasteService]")
  public void pasteServices(
    CommandSource source,
    @Argument("pasteService") PasteService pasteService,
    @Argument("service") ICloudService service
  ) {
    var pasteCreator = new PasteCreator(this.fallbackPasteService(pasteService),
      this.reportModule.getEmitterRegistry());

    var response = pasteCreator.createServicePaste(service);
    if (response == null) {
      source.sendMessage(I18n.trans("module-report-command-paste-failed")
        .replace("%url%", pasteService.serviceUrl()));
    } else {
      source.sendMessage(I18n.trans("module-report-command-paste-success")
        .replace("%url%", response));
    }
  }

  @CommandMethod("report|paste thread-dump")
  public void reportThreadDump(CommandSource source) {
    var file = this.reportModule.getCurrentRecordDirectory().resolve(System.currentTimeMillis() + "-threaddump.txt");

    if (this.createThreadDump(file)) {
      source.sendMessage(
        I18n.trans("module-report-thread-dump-success").replace("%file%", file.toString()));
    } else {
      source.sendMessage(I18n.trans("module-report-thread-dump-failed"));
    }
  }

  @CommandMethod("report|paste heap-dump")
  public void reportHeapDump(CommandSource source, @Flag("live") boolean live) {
    var file = this.reportModule.getCurrentRecordDirectory().resolve(System.currentTimeMillis() + "-heapdump.hprof");

    if (this.createHeapDump(file, live)) {
      source.sendMessage(
        I18n.trans("module-report-heap-dump-success").replace("%file%", file.toString()));
    } else {
      source.sendMessage(I18n.trans("module-report-heap-dump-failed"));
    }
  }

  private boolean createThreadDump(Path path) {
    var builder = new StringBuilder();
    var threadBean = ManagementFactory.getThreadMXBean();
    for (var threadInfo : threadBean.dumpAllThreads(threadBean.isObjectMonitorUsageSupported(),
      threadBean.isSynchronizerUsageSupported())) {
      builder.append(threadInfo.toString());
    }

    try {
      Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
      return true;
    } catch (IOException exception) {
      LOGGER.severe("Unable to create thread dump", exception);
      return false;
    }
  }

  private boolean createHeapDump(Path path, boolean live) {
    try {
      var server = ManagementFactory.getPlatformMBeanServer();
      var mxBean = ManagementFactory.newPlatformMXBeanProxy(
        server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
      mxBean.dumpHeap(path.toString(), live);
      return true;
    } catch (IOException exception) {
      LOGGER.severe("Unable to create heap dump", exception);
      return false;
    }
  }

  private PasteService fallbackPasteService(@Nullable PasteService service) {
    // paste services are optional, but the user entered one just return it
    if (service != null) {
      return service;
    }

    var pasteServices = this.reportModule.getReportConfiguration().pasteServers();
    // there are no paste services, use fallback
    if (pasteServices.isEmpty()) {
      return PasteService.FALLBACK;
    }
    // use the first in the configuration
    return this.reportModule.getReportConfiguration().pasteServers().get(0);
  }

}
