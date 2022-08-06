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
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.collect.Iterables;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.report.CloudNetReportModule;
import eu.cloudnetservice.modules.report.config.PasteServer;
import eu.cloudnetservice.modules.report.emitter.EmitterRegistry;
import eu.cloudnetservice.modules.report.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.emitter.ReportDataWriter;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("paste")
@CommandPermission("cloudnet.command.paste")
@Description("module-report-command-description")
public final class ReportCommand {

  private static final Logger LOGGER = LogManager.logger(ReportCommand.class);

  private final EmitterRegistry emitterRegistry;
  private final CloudNetReportModule reportModule;

  public ReportCommand(@NonNull CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
    this.emitterRegistry = ServiceRegistry.first(EmitterRegistry.class);
  }

  @Parser(suggestions = "pasteServer")
  public @NonNull PasteServer defaultPasteServerParser(
    @NonNull CommandContext<CommandSource> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    return this.reportModule.configuration().pasteServers()
      .stream()
      .filter(server -> server.name().equalsIgnoreCase(name))
      .findFirst()
      .orElseThrow(
        () -> new ArgumentNotAvailableException(I18n.trans("module-report-command-paste-server-not-found", name)));
  }

  @Suggestions("pasteServer")
  public @NonNull List<String> suggestPasteServers(@NonNull CommandContext<CommandSource> $, @NonNull String input) {
    return this.reportModule.configuration().pasteServers()
      .stream()
      .map(Nameable::name)
      .toList();
  }

  @Parser(suggestions = "service")
  public @NonNull ServiceInfoSnapshot serviceSnapshotParser(
    @NonNull CommandContext<CommandSource> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    return CloudNetDriver.instance().cloudServiceProvider().services().stream()
      .filter(service -> service.name().equals(name))
      .findFirst()
      .orElseThrow(
        () -> new ArgumentNotAvailableException(I18n.trans("module-report-command-service-not-found", name)));
  }

  @CommandMethod("report|paste all [pasteServer]")
  public void pasteAll(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(this.emitterRegistry.emitters());
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste general [pasteServer]")
  public void pasteGeneral(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(Object.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste nodes [pasteServer]")
  public void pasteNodes(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(NodeServer.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste node <node> [pasteServer]")
  public void pasteNode(
    @NonNull CommandSource source,
    @NonNull @Argument("node") NodeServer node,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(NodeServer.class, node);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste services [pasteServer]")
  public void pasteServices(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(ServiceInfoSnapshot.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste service <service> [pasteServer]")
  public void pasteService(
    @NonNull CommandSource source,
    @NonNull @Argument("service") ServiceInfoSnapshot service,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(ServiceInfoSnapshot.class, service);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste tasks [pasteServer]")
  public void pasteTasks(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(ServiceTask.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste task <task> [pasteServer]")
  public void pasteTask(
    @NonNull CommandSource source,
    @NonNull @Argument("task") ServiceTask task,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(ServiceTask.class, task);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste groups [pasteServer]")
  public void pasteGroups(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(GroupConfiguration.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste group <group> [pasteServer]")
  public void pasteGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("group") GroupConfiguration configuration,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(GroupConfiguration.class, configuration);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste modules [pasteServer]")
  public void pasteModules(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(ModuleWrapper.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @CommandMethod("report|paste module <module> [pasteServer]")
  public void pasteModule(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "module", parserName = "existingModule") ModuleWrapper module,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(ModuleWrapper.class, module);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  private @NonNull PasteServer givenOrDefaultPasteServer(@Nullable PasteServer server) {
    // paste services are optional, but the user entered one just return it
    return Objects.requireNonNullElseGet(
      server,
      () -> Iterables.getFirst(this.reportModule.configuration().pasteServers(), PasteServer.DEFAULT_PASTER_SERVER));
  }

  private @NonNull ReportDataWriter emitFullData(@NonNull Class<?> emitterDataClass) {
    var emitters = this.emitterRegistry.emitters(emitterDataClass);
    return this.emitFullData(emitters);
  }

  private @NonNull ReportDataWriter emitFullData(@NonNull Collection<ReportDataEmitter> emitters) {
    var writer = ReportDataWriter.newEmptyWriter();
    for (var emitter : emitters) {
      writer = emitter.emitData(writer);
    }

    return writer;
  }

  private @NonNull <T> ReportDataWriter emitFullSpecificData(@NonNull Class<T> emitterType, @NonNull T value) {
    var writer = ReportDataWriter.newEmptyWriter();
    var emitters = this.emitterRegistry.specificEmitters(emitterType);

    // emit the data
    for (var emitter : emitters) {
      writer = emitter.emitData(writer, value);
    }

    return writer;
  }

  private void pasteDataToPasteServer(
    @NonNull CommandSource source,
    @NonNull String content,
    @Nullable PasteServer target
  ) {
    var pasteServer = this.givenOrDefaultPasteServer(target);
    pasteServer.postData(content).whenComplete((pasteKey, exception) -> {
      if (exception != null) {
        // failed to upload data
        LOGGER.severe("Unable to post paste data to %s (%s)", exception, pasteServer.baseUrl(), pasteServer.name());
        source.sendMessage(I18n.trans("module-report-command-paste-failed", pasteServer.baseUrl()));
      } else {
        // successfully uploaded data
        source.sendMessage(I18n.trans("module-report-command-paste-success", pasteServer.baseUrl() + pasteKey));
      }
    });
  }
}
