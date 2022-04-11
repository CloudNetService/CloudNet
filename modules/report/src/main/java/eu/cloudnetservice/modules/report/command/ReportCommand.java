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

package eu.cloudnetservice.modules.report.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.collect.Iterables;
import com.sun.management.HotSpotDiagnosticMXBean;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.modules.report.CloudNetReportModule;
import eu.cloudnetservice.modules.report.config.PasteService;
import eu.cloudnetservice.modules.report.paste.PasteCreator;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.service.CloudService;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("paste")
@CommandPermission("cloudnet.command.paste")
@Description("Upload cloud specific data to a paste service")
public final class ReportCommand {

  private static final Logger LOGGER = LogManager.logger(ReportCommand.class);

  private final CloudNetReportModule reportModule;

  public ReportCommand(@NonNull CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
  }

  @Parser(suggestions = "pasteService")
  public @NonNull PasteService defaultPasteServiceParser(
    @NonNull CommandContext<CommandSource> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    return this.reportModule.reportConfiguration().pasteServers()
      .stream()
      .filter(service -> service.name().equalsIgnoreCase(name))
      .findFirst()
      .orElseThrow(
        () -> new ArgumentNotAvailableException(I18n.trans("command-paste-paste-service-not-found")));
  }

  @Suggestions("pasteService")
  public @NonNull List<String> suggestPasteService(@NonNull CommandContext<CommandSource> $, @NonNull String input) {
    return this.reportModule.reportConfiguration().pasteServers()
      .stream()
      .map(Nameable::name)
      .toList();
  }

  @Parser(suggestions = "cloudService")
  public @NonNull CloudService singleServiceParser(
    @NonNull CommandContext<CommandSource> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    var cloudService = Node.instance().cloudServiceProvider().localCloudService(name);
    if (cloudService == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-service-not-found"));
    }
    return cloudService;
  }

  @Suggestions("cloudService")
  public @NonNull List<String> suggestService(@NonNull CommandContext<CommandSource> $, @NonNull String input) {
    return Node.instance().cloudServiceProvider().localCloudServices()
      .stream()
      .map(service -> service.serviceId().name())
      .toList();
  }

  @CommandMethod("report|paste node [pasteService]")
  public void pasteNode(@NonNull CommandSource source, @Nullable @Argument("pasteService") PasteService pasteService) {
    var pasteCreator = new PasteCreator(this.fallbackPasteService(pasteService), this.reportModule.emitterRegistry());
    var selfNode = Node.instance().nodeServerProvider().localNode().nodeInfoSnapshot();

    var response = pasteCreator.createNodePaste(selfNode);
    if (response == null) {
      source.sendMessage(I18n.trans("module-report-command-paste-failed", pasteCreator.pasteService().serviceUrl()));
    } else {
      source.sendMessage(I18n.trans("module-report-command-paste-success", response));
    }
  }

  @CommandMethod("report|paste service <service> [pasteService]")
  public void pasteServices(
    @NonNull CommandSource source,
    @NonNull @Argument("service") CloudService service,
    @Nullable @Argument("pasteService") PasteService pasteService
  ) {
    var pasteCreator = new PasteCreator(this.fallbackPasteService(pasteService),
      this.reportModule.emitterRegistry());

    var response = pasteCreator.createServicePaste(service);
    if (response == null) {
      source.sendMessage(I18n.trans("module-report-command-paste-failed", pasteCreator.pasteService().serviceUrl()));
    } else {
      source.sendMessage(I18n.trans("module-report-command-paste-success", response));
    }
  }

  @CommandMethod("report|paste thread-dump")
  public void reportThreadDump(@NonNull CommandSource source) {
    var file = this.reportModule.currentRecordDirectory().resolve(System.currentTimeMillis() + "-threaddump.txt");

    if (this.createThreadDump(file)) {
      source.sendMessage(
        I18n.trans("module-report-thread-dump-success", file.toString()));
    } else {
      source.sendMessage(I18n.trans("module-report-thread-dump-failed"));
    }
  }

  @CommandMethod("report|paste heap-dump")
  public void reportHeapDump(@NonNull CommandSource source, @Flag("live") boolean live) {
    var file = this.reportModule.currentRecordDirectory().resolve(System.currentTimeMillis() + "-heapdump.hprof");

    if (this.createHeapDump(file, live)) {
      source.sendMessage(
        I18n.trans("module-report-heap-dump-success", file.toString()));
    } else {
      source.sendMessage(I18n.trans("module-report-heap-dump-failed"));
    }
  }

  private boolean createThreadDump(@NonNull Path path) {
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

  private boolean createHeapDump(@NonNull Path path, boolean live) {
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

  private @NonNull PasteService fallbackPasteService(@Nullable PasteService service) {
    // paste services are optional, but the user entered one just return it
    if (service != null) {
      return service;
    }
    return Iterables.getFirst(this.reportModule.reportConfiguration().pasteServers(), PasteService.FALLBACK);
  }
}
