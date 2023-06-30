/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.common.log.AbstractHandler;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpHandler;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketChannel;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketListener;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.JsonConfiguration;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import eu.cloudnetservice.node.permission.command.PermissionUserCommandSource;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.node")
@ApplyHeaders
public final class V2HttpHandlerNode extends V2HttpHandler {

  private final Configuration configuration;
  private final NetworkClient networkClient;
  private final ModuleProvider moduleProvider;
  private final CloudNetVersion cloudNetVersion;
  private final CommandProvider commandProvider;
  private final NodeServerProvider nodeServerProvider;
  private final CloudServiceManager cloudServiceManager;
  private final ServiceTaskProvider serviceTaskProvider;
  private final PermissionManagement permissionManagement;
  private final GroupConfigurationProvider groupConfigurationProvider;

  @Inject
  public V2HttpHandlerNode(
    @NonNull Configuration configuration,
    @NonNull NetworkClient networkClient,
    @NonNull ModuleProvider moduleProvider,
    @NonNull CloudNetVersion cloudNetVersion,
    @NonNull CommandProvider commandProvider,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CloudServiceManager cloudServiceManager,
    @NonNull ServiceTaskProvider serviceTaskProvider,
    @NonNull PermissionManagement permissionManagement,
    @NonNull GroupConfigurationProvider groupConfigurationProvider
  ) {
    this.configuration = configuration;
    this.networkClient = networkClient;
    this.moduleProvider = moduleProvider;
    this.cloudNetVersion = cloudNetVersion;
    this.commandProvider = commandProvider;
    this.nodeServerProvider = nodeServerProvider;
    this.cloudServiceManager = cloudServiceManager;
    this.serviceTaskProvider = serviceTaskProvider;
    this.permissionManagement = permissionManagement;
    this.groupConfigurationProvider = groupConfigurationProvider;
  }

  @HttpRequestHandler(paths = "/api/v2/node", priority = HttpHandler.PRIORITY_LOW)
  private void handleNodePing(@NonNull HttpContext context) {
    this.response(context, HttpResponseCode.NO_CONTENT).context().closeAfter(true).cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/node")
  private void sendNodeInformation(@NonNull HttpContext context) {
    var nodeServer = this.nodeServerProvider.localNode();

    var information = this.success()
      .append("version", this.cloudNetVersion)
      .append("nodeInfoSnapshot", nodeServer.nodeInfoSnapshot())
      .append("lastNodeInfoSnapshot", nodeServer.lastNodeInfoSnapshot())
      .append("serviceCount", this.cloudServiceManager.serviceCount())
      .append("clientConnections", this.networkClient.channels().stream()
        .map(NetworkChannel::serverAddress)
        .toList());
    this.ok(context).body(information.toString()).context().closeAfter(true).cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/node/config")
  private void handleNodeConfigRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("config", this.configuration).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/node/config", methods = "PUT")
  private void handleNodeConfigUpdateRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    var configuration = body.toInstanceOf(JsonConfiguration.class);
    if (configuration == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing configuration in body").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    // a little workaround here, write the configuration to the file and load the used from there
    configuration.save();
    this.configuration.load();

    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/node/reload")
  private void handleReloadRequest(
    @NonNull HttpContext context,
    @NonNull @Optional @FirstRequestQueryParam(value = "type", def = "all") String type
  ) {
    switch (StringUtil.toLower(type)) {
      case "all" -> {
        this.reloadConfig();
        this.moduleProvider.reloadAll();
      }
      case "config" -> this.reloadConfig();
      default -> {
        this.badRequest(context)
          .body(this.failure().append("reason", "Invalid reload type").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
        return;
      }
    }

    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @HttpRequestHandler(paths = "/api/v2/node/liveConsole")
  private void handleLiveConsoleRequest(@NonNull HttpContext context, @NonNull @BearerAuth HttpSession session) {
    context.upgrade().thenAccept(channel -> {
      var handler = new WebSocketLogHandler(session, channel, DefaultLogFormatter.END_LINE_SEPARATOR);
      channel.addListener(handler);
      LogManager.rootLogger().addHandler(handler);
    });
  }

  private void reloadConfig() {
    this.configuration.reloadFrom(JsonConfiguration.loadFromFile());
    this.serviceTaskProvider.reload();
    this.groupConfigurationProvider.reload();
    this.permissionManagement.reload();
  }

  protected class WebSocketLogHandler extends AbstractHandler implements WebSocketListener {

    protected final HttpSession httpSession;
    protected final WebSocketChannel channel;

    public WebSocketLogHandler(
      @NonNull HttpSession session,
      @NonNull WebSocketChannel channel,
      @NonNull Formatter formatter
    ) {
      super.setFormatter(formatter);
      this.httpSession = session;
      this.channel = channel;
    }

    @Override
    public void handle(@NonNull WebSocketChannel channel, @NonNull WebSocketFrameType type, byte[] bytes) {
      var user = this.httpSession.user();
      if (type == WebSocketFrameType.TEXT && user != null) {
        var commandLine = new String(bytes, StandardCharsets.UTF_8);

        var commandSource = new PermissionUserCommandSource(user, V2HttpHandlerNode.this.permissionManagement);
        V2HttpHandlerNode.this.commandProvider.execute(commandSource, commandLine).getOrNull();

        for (var message : commandSource.messages()) {
          this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, message);
        }
      }
    }

    @Override
    public void handleClose(
      @NonNull WebSocketChannel channel,
      @NonNull AtomicInteger statusCode,
      @NonNull AtomicReference<String> statusText
    ) {
      LogManager.rootLogger().removeHandler(this);
    }

    @Override
    public void publish(@NonNull LogRecord record) {
      this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, super.getFormatter().format(record));
    }
  }
}
