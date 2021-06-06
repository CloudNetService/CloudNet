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

package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
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
      IPermissionUserCommandSender commandSender = new DefaultPermissionUserCommandSender(permissionUser,
        this.getCloudNet().getPermissionManagement());

      if (this.getCloudNet().getCommandMap().dispatchCommand(commandSender, commandLine)) {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_OK)
          .body(new JsonDocument("receivedMessages", commandSender.getWrittenMessages().toArray(new String[0]))
            .toByteArray())
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
