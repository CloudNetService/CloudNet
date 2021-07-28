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

package de.dytanic.cloudnet.ext.bridge.node.http.v2;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
import java.util.UUID;
import java.util.function.Consumer;

public class V2HttpHandlerBridge extends V2HttpHandler {

  public V2HttpHandlerBridge(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/exists")) {
        this.handleCloudPlayerExistsRequest(context);
      } else {
        this.handleCloudPlayerRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      this.handleCreateCloudPlayerRequest(context);
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      this.handleDeleteCloudPlayerRequest(context);
    }
  }

  protected void handleCloudPlayerExistsRequest(IHttpContext context) {
    this.handleWithCloudPlayerContext(context, true, player -> this.ok(context)
      .body(this.success().append("result", player != null).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleCloudPlayerRequest(IHttpContext context) {
    this.handleWithCloudPlayerContext(context, false, player -> this.ok(context)
      .body(this.success().append("player", player).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext());
  }

  protected void handleCreateCloudPlayerRequest(IHttpContext context) {
    ICloudOfflinePlayer cloudOfflinePlayer = this.body(context.request()).toInstanceOf(CloudOfflinePlayer.class);
    if (cloudOfflinePlayer == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing player configuration").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.getPlayerManager().updateOfflinePlayer(cloudOfflinePlayer);
    this.response(context, HttpResponseCode.HTTP_CREATED)
      .body(this.success().toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeleteCloudPlayerRequest(IHttpContext context) {
    this.handleWithCloudPlayerContext(context, false, player -> {
      this.getPlayerManager().deleteCloudOfflinePlayer(player);
      this.ok(context)
        .body(this.success().toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleWithCloudPlayerContext(
    IHttpContext context,
    boolean mayBeNull,
    Consumer<ICloudOfflinePlayer> handler
  ) {
    String identifier = context.request().pathParameters().get("player");
    if (identifier == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing player identifier").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // try to find a matching player
    ICloudOfflinePlayer player;
    try {
      // try to parse a player unique id from the string
      UUID uniqueId = UUID.fromString(identifier);
      player = this.getPlayerManager().getOfflinePlayer(uniqueId);
    } catch (Exception exception) {
      player = this.getPlayerManager().getFirstOfflinePlayer(identifier);
    }
    // check if a player is present before applying to the handler
    if (player == null && !mayBeNull) {
      this.ok(context)
        .body(this.failure().append("reason", "No player with provided uniqueId/name").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // post to handler
    handler.accept(player);
  }

  protected IPlayerManager getPlayerManager() {
    return this.getCloudNet().getServicesRegistry().getFirstService(IPlayerManager.class);
  }

}
