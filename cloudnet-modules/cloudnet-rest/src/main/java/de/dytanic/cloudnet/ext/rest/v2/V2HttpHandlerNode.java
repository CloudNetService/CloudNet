package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.DefaultLogFormatter;
import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.conf.JsonConfiguration;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.rest.RestUtils;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
import de.dytanic.cloudnet.permission.command.DefaultPermissionUserCommandSender;
import de.dytanic.cloudnet.permission.command.IPermissionUserCommandSender;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class V2HttpHandlerNode extends V2HttpHandler {

    protected static final IFormatter LOG_FORMATTER = new DefaultLogFormatter();

    public V2HttpHandlerNode(String requiredPermission) {
        super(requiredPermission, "GET", "PUT");
    }

    @Override
    protected void handleUnauthorized(String path, IHttpContext context) {
        this.response(context, HttpResponseCode.HTTP_NO_CONTENT).context().closeAfter(true).cancelNext();
    }

    @Override
    protected void handleBasicAuthorized(String path, IHttpContext context, IPermissionUser user) {
        this.sendNodeInformation(context);
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
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

    protected void sendNodeInformation(IHttpContext context) {
        JsonDocument information = this.success()
                .append("title", CloudNet.class.getPackage().getImplementationTitle())
                .append("version", CloudNet.class.getPackage().getImplementationVersion())
                .append("nodeInfoSnapshot", this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot())
                .append("lastNodeInfoSnapshot", this.getCloudNet().getLastNetworkClusterNodeInfoSnapshot())
                .append("serviceCount", this.getCloudNet().getCloudServiceProvider().getServicesCount())
                .append("clientConnections", super.getCloudNet().getNetworkClient().getChannels().stream()
                        .map(INetworkChannel::getServerAddress)
                        .collect(Collectors.toList()));
        this.ok(context).body(information.toByteArray()).context().closeAfter(true).cancelNext();
    }

    protected void handleNodeConfigRequest(IHttpContext context) {
        this.ok(context)
                .body(this.success().append("config", this.getConfiguration()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleNodeConfigUpdateRequest(IHttpContext context) {
        JsonConfiguration configuration = this.body(context.request()).toInstanceOf(JsonConfiguration.class);
        if (configuration == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing configuration in body").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        // a little workaround here, write the configuration to the file and load the used from there
        configuration.save();
        this.getConfiguration().load();

        this.ok(context)
                .body(this.success().toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleReloadRequest(IHttpContext context) {
        String type = RestUtils.getFirst(context.request().queryParameters().get("type"), "all").toLowerCase();
        switch (type) {
            case "all":
                this.getCloudNet().reload();
                break;
            case "config":
                this.getCloudNet().getConfig().load();
                this.getCloudNet().getConfigurationRegistry().load();
                this.getCloudNet().getServiceTaskProvider().reload();
                this.getCloudNet().getGroupConfigurationProvider().reload();
                this.getCloudNet().getPermissionManagement().reload();
                break;
            default:
                this.badRequest(context)
                        .body(this.failure().append("reason", "Invalid reload type").toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
                return;
        }

        this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
    }

    protected void handleLiveConsoleRequest(IHttpContext context, HttpSession session) {
        IWebSocketChannel channel = context.upgrade();
        if (channel != null) {
            WebSocketLogHandler handler = new WebSocketLogHandler(session, channel, LOG_FORMATTER);

            channel.addListener(handler);
            CloudNetDriver.getInstance().getLogger().addLogHandler(handler);
        }
    }

    protected class WebSocketLogHandler extends AbstractLogHandler implements IWebSocketListener {

        protected final HttpSession httpSession;
        protected final IWebSocketChannel channel;

        public WebSocketLogHandler(HttpSession session, IWebSocketChannel channel, IFormatter formatter) {
            super(formatter);
            this.httpSession = session;
            this.channel = channel;
        }

        @Override
        public void handle(@NotNull LogEntry logEntry) throws Exception {
            this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, super.formatter.format(logEntry));
        }

        @Override
        public void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) throws Exception {
            IPermissionUser user = this.httpSession.getUser();
            if (type == WebSocketFrameType.TEXT && user != null) {
                String commandLine = new String(bytes, StandardCharsets.UTF_8);

                IPermissionUserCommandSender sender = new DefaultPermissionUserCommandSender(user,
                        V2HttpHandlerNode.this.getCloudNet().getPermissionManagement());
                V2HttpHandlerNode.this.getCloudNet().getCommandMap().dispatchCommand(sender, commandLine);

                for (String message : sender.getWrittenMessages()) {
                    this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, message);
                }
            }
        }

        @Override
        public void handleClose(IWebSocketChannel channel, AtomicInteger statusCode, AtomicReference<String> reasonText) {
            CloudNetDriver.getInstance().getLogger().removeLogHandler(this);
        }
    }
}
