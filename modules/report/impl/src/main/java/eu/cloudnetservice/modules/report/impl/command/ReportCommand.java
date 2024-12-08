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

package eu.cloudnetservice.modules.report.impl.command;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.report.config.PasteServer;
import eu.cloudnetservice.modules.report.impl.CloudNetReportModule;
import eu.cloudnetservice.modules.report.impl.emitter.EmitterService;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@CommandAlias("paste")
@Permission("cloudnet.command.paste")
@Description("module-report-command-description")
public final class ReportCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportCommand.class);

  private final I18n i18n;
  private final EmitterService emitterService;
  private final CloudNetReportModule reportModule;
  private final CloudServiceProvider serviceProvider;

  @Inject
  public ReportCommand(
    @NonNull @Service I18n i18n,
    @NonNull EmitterService emitterService,
    @NonNull CloudNetReportModule reportModule,
    @NonNull CloudServiceProvider serviceProvider
  ) {
    this.i18n = i18n;
    this.emitterService = emitterService;
    this.reportModule = reportModule;
    this.serviceProvider = serviceProvider;
  }

  @Parser(suggestions = "pasteServer")
  public @NonNull PasteServer defaultPasteServerParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var name = input.readString();
    return this.reportModule.configuration().pasteServers()
      .stream()
      .filter(server -> server.name().equalsIgnoreCase(name))
      .findFirst()
      .orElseThrow(
        () -> new ArgumentNotAvailableException(i18n.translate("module-report-command-paste-server-not-found", name)));
  }

  @Suggestions("pasteServer")
  public @NonNull Stream<String> suggestPasteServers() {
    return this.reportModule.configuration().pasteServers()
      .stream()
      .map(Named::name);
  }

  @Parser(suggestions = "service")
  public @NonNull ServiceInfoSnapshot serviceSnapshotParser(@NonNull CommandInput input) {
    var name = input.readString();
    return this.serviceProvider.services().stream()
      .filter(service -> service.name().equals(name))
      .findFirst()
      .orElseThrow(
        () -> new ArgumentNotAvailableException(this.i18n.translate("module-report-command-service-not-found", name)));
  }

  @Command("report|paste all [pasteServer]")
  public void pasteAll(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(this.emitterService.emitters());
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste general [pasteServer]")
  public void pasteGeneral(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(Object.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste nodes [pasteServer]")
  public void pasteNodes(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(NodeServer.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste node <node> [pasteServer]")
  public void pasteNode(
    @NonNull CommandSource source,
    @NonNull @Argument("node") NodeServer node,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(NodeServer.class, node);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste services [pasteServer]")
  public void pasteServices(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(ServiceInfoSnapshot.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste service <service> [pasteServer]")
  public void pasteService(
    @NonNull CommandSource source,
    @NonNull @Argument("service") ServiceInfoSnapshot service,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(ServiceInfoSnapshot.class, service);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste tasks [pasteServer]")
  public void pasteTasks(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(ServiceTask.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste task <task> [pasteServer]")
  public void pasteTask(
    @NonNull CommandSource source,
    @NonNull @Argument("task") ServiceTask task,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(ServiceTask.class, task);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste groups [pasteServer]")
  public void pasteGroups(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(GroupConfiguration.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste group <group> [pasteServer]")
  public void pasteGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("group") GroupConfiguration configuration,
    @Nullable @Argument("pasteServer") PasteServer pasteServer
  ) {
    var pasteContent = this.emitFullSpecificData(GroupConfiguration.class, configuration);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste modules [pasteServer]")
  public void pasteModules(@NonNull CommandSource source, @Nullable @Argument("pasteServer") PasteServer pasteServer) {
    var pasteContent = this.emitFullData(ModuleWrapper.class);
    this.pasteDataToPasteServer(source, pasteContent.toString(), pasteServer);
  }

  @Command("report|paste module <module> [pasteServer]")
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
    var emitters = this.emitterService.emitters(emitterDataClass);
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
    var emitters = this.emitterService.specificEmitters(emitterType);

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
    this.postData(pasteServer, content).whenComplete((pasteKey, exception) -> {
      if (exception != null) {
        // failed to upload data
        LOGGER.error("Unable to post paste data to {} ({})", pasteServer.baseUrl(), pasteServer.name(), exception);
        source.sendMessage(this.i18n.translate("module-report-command-paste-failed", pasteServer.baseUrl()));
      } else {
        // successfully uploaded data
        source.sendMessage(this.i18n.translate(
          "module-report-command-paste-success",
          pasteServer.baseUrl() + pasteKey));
      }
    });
  }

  private @NonNull CompletableFuture<String> postData(@NonNull PasteServer server, @NonNull String content) {
    return Unirest.request(server.requestMethod(), String.format("%s%s", server.baseUrl(), server.apiDataEndpoint()))
      .body(content)
      .headers(server.headers())
      .requestTimeout(10_000)
      .contentType("text/plain")
      .charset(StandardCharsets.UTF_8)
      .accept("application/json")
      .responseEncoding(StandardCharsets.UTF_8.name())
      .asJsonAsync()
      .thenApply(object -> object.getBody().getObject().getString(server.responsePasteIdKey()));
  }
}
