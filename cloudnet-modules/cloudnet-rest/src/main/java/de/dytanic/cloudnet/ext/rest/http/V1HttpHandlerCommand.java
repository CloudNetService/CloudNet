package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.http.V1HttpHandler;
import de.dytanic.cloudnet.permission.command.DefaultPermissionUserCommandSender;
import de.dytanic.cloudnet.permission.command.IPermissionUserCommandSender;

public final class V1HttpHandlerCommand extends V1HttpHandler {

    public V1HttpHandlerCommand(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, POST");
    }

    @Override
    public void handlePost(String path, IHttpContext context) {
        if (context.request().body().length == 0) {
            this.send400Response(context, "Empty http body");
            return;
        }

        String commandLine = context.request().bodyAsString();
        IPermissionUser permissionUser = HTTP_SESSION.getUser(context);

        if (permissionUser != null) {
            IPermissionUserCommandSender commandSender = new DefaultPermissionUserCommandSender(permissionUser, getCloudNet().getPermissionManagement());

            if (getCloudNet().getCommandMap().dispatchCommand(commandSender, commandLine)) {
                context
                        .response()
                        .body(new JsonDocument("receivedMessages", commandSender.getWrittenMessages().toArray(new String[0])).toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;
                return;
            }
        }

        this.send400Response(context, "userUniqueId not found or command not exists").closeAfter(true).cancelNext();
    }
}