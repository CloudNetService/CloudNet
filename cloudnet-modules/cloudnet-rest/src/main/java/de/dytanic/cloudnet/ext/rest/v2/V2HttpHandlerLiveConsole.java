package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.DefaultLogFormatter;
import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class V2HttpHandlerLiveConsole extends V2HttpHandler {

    protected static final IFormatter LOG_FORMATTER = new DefaultLogFormatter();

    public V2HttpHandlerLiveConsole(String requiredPermission) {
        super(requiredPermission, "GET");
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) throws Exception {
        IWebSocketChannel channel = context.upgrade();
        if (channel != null) {
            WebSocketLogHandler handler = new WebSocketLogHandler(channel, LOG_FORMATTER);

            channel.addListener(handler);
            CloudNetDriver.getInstance().getLogger().addLogHandler(handler);
        }
    }

    protected static class WebSocketLogHandler extends AbstractLogHandler implements IWebSocketListener {

        protected final IWebSocketChannel channel;

        public WebSocketLogHandler(IWebSocketChannel channel, IFormatter formatter) {
            super(formatter);
            this.channel = channel;
        }

        @Override
        public void handle(@NotNull LogEntry logEntry) throws Exception {
            this.channel.sendWebSocketFrame(WebSocketFrameType.TEXT, super.formatter.format(logEntry));
        }

        @Override
        public void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) throws Exception {
        }

        @Override
        public void handleClose(IWebSocketChannel channel, AtomicInteger statusCode, AtomicReference<String> reasonText) {
            CloudNetDriver.getInstance().getLogger().removeLogHandler(this);
        }
    }
}
