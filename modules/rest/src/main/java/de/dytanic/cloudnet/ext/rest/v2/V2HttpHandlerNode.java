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

package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.log.AbstractHandler;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.defaults.DefaultLogFormatter;
import de.dytanic.cloudnet.config.JsonConfiguration;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.http.HttpContext;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketListener;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.rest.RestUtils;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.WebSocketAbleV2HttpHandler;
import de.dytanic.cloudnet.permission.command.PermissionUserCommandSource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import lombok.NonNull;

public class V2HttpHandlerNode extends WebSocketAbleV2HttpHandler {

  public V2HttpHandlerNode(String requiredPermission) {
    super(
      requiredPermission,
      (context, path) -> context.request().method().equalsIgnoreCase("GET") && path.endsWith("/liveconsole"),
      "GET", "PUT");
  }

  @Override
  protected void handleUnauthorizedRequest(String path, HttpContext context) {
    this.response(context, HttpResponseCode.HTTP_NO_CONTENT).context().closeAfter(true).cancelNext();
  }

  @Override
  protected void handleBasicAuthorized(String path, HttpContext context, PermissionUser user) {
    this.sendNodeInformation(context);
  }

  @Override
  protected void handleBearerAuthorized(String path, HttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/liveconsole")) {
        this.handleLiveConsoleRequest(context, session);
      } else if (path.endsWith("/config")) {
        this.handleNodeConfigRequest(context);
      } else if (path.endsWith("/reload")) {
        this.handleReloadRequest(context);
      } else {
        this.sendNodeInformation(context);
      }
    } else if (context.request().method().equalsIgnoreCase("PUT")) {
      if (path.endsWith("/config")) {
        this.handleNodeConfigUpdateRequest(context);
      }
    }
  }

  @Override
  protected void handleTicketAuthorizedRequest(String path, HttpContext context, HttpSession session) {
    this.handleLiveConsoleRequest(context, session);
  }

  protected void sendNodeInformation(HttpContext context) {
    var nodeServer = this.node().nodeServerProvider().selfNode();

    var information = this.success()
      .append("title", CloudNet.class.getPackage().getImplementationTitle())
      .append("version", CloudNet.class.getPackage().getImplementationVersion())
      .append("nodeInfoSnapshot", nodeServer.nodeInfoSnapshot())
      .append("lastNodeInfoSnapshot", nodeServer.lastNodeInfoSnapshot())
      .append("serviceCount", this.node().cloudServiceProvider().serviceCount())
      .append("clientConnections", super.node().networkClient().channels().stream()
        .map(NetworkChannel::serverAddress)
        .collect(Collectors.toList()));
    this.ok(context).body(information.toString()).context().closeAfter(true).cancelNext();
  }

  protected void handleNodeConfigRequest(HttpContext context) {
    this.ok(context)
      .body(this.success().append("config", this.configuration()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleNodeConfigUpdateRequest(HttpContext context) {
    var configuration = this.body(context.request()).toInstanceOf(JsonConfiguration.class);
    if (configuration == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing configuration in body").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    // a little workaround here, write the configuration to the file and load the used from there
    configuration.save();
    this.configuration().load();

    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleReloadRequest(HttpContext context) {
    var type = RestUtils.first(context.request().queryParameters().get("type"), "all").toLowerCase();
    switch (type) {
      case "all":
        //TODO what to reload
        break;
      case "config":
        this.node().config().load();
        this.node().serviceTaskProvider().reload();
        this.node().groupConfigurationProvider().reload();
        this.node().permissionManagement().reload();
        break;
      default:
        this.badRequest(context)
          .body(this.failure().append("reason", "Invalid reload type").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
        return;
    }

    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleLiveConsoleRequest(HttpContext context, HttpSession session) {
    var channel = context.upgrade();
    if (channel != null) {
      var handler = new WebSocketLogHandler(session, channel, DefaultLogFormatter.END_LINE_SEPARATOR);

      channel.addListener(handler);
      LogManager.rootLogger().addHandler(handler);
    }
  }

  protected class WebSocketLogHandler extends AbstractHandler implements WebSocketListener {

    protected final HttpSession httpSession;
    protected final WebSocketChannel channel;

    public WebSocketLogHandler(HttpSession session, WebSocketChannel channel, Formatter formatter) {
      super.setFormatter(formatter);
      this.httpSession = session;
      this.channel = channel;
    }

    @Override
    public void handle(@NonNull WebSocketChannel channel, @NonNull WebSocketFrameType type, byte[] bytes)
      throws Exception {
      var user = this.httpSession.user();
      if (type == WebSocketFrameType.TEXT && user != null) {
        var commandLine = new String(bytes, StandardCharsets.UTF_8);

        var commandSource = new PermissionUserCommandSource(user,
          V2HttpHandlerNode.this.node().permissionManagement());
        V2HttpHandlerNode.this.node().commandProvider().execute(commandSource, commandLine).join();

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
    public void publish(LogRecord record) {
      this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, super.getFormatter().format(record));
    }
  }
}
