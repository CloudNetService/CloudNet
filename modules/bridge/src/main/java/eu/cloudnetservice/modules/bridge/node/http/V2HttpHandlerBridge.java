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

package eu.cloudnetservice.modules.bridge.node.http;

import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;

public class V2HttpHandlerBridge extends V2HttpHandler {

  public V2HttpHandlerBridge(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context, @NonNull HttpSession ses) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/exists")) {
        this.handleCloudPlayerExistsRequest(context);
      } else if (path.endsWith("/onlinecount")) {
        this.handleOnlinePlayerCountRequest(context);
      } else if (path.endsWith("/registeredcount")) {
        this.handleRegisteredPlayerCountRequest(context);
      } else {
        this.handleCloudPlayerRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      this.handleCreateCloudPlayerRequest(context);
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      this.handleDeleteCloudPlayerRequest(context);
    }
  }

  protected void handleOnlinePlayerCountRequest(HttpContext context) {
    this.ok(context)
      .body(this.success().append("onlineCount", this.playerManager().onlineCount()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleRegisteredPlayerCountRequest(HttpContext context) {
    this.ok(context)
      .body(this.success().append("registeredCount", this.playerManager().registeredCount()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleCloudPlayerExistsRequest(HttpContext context) {
    this.handleWithCloudPlayerContext(context, true, player -> this.ok(context)
      .body(this.success().append("result", player != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleCloudPlayerRequest(HttpContext context) {
    this.handleWithCloudPlayerContext(context, false, player -> this.ok(context)
      .body(this.success().append("player", player).toString())
      .context()
      .closeAfter(true)
      .cancelNext());
  }

  protected void handleCreateCloudPlayerRequest(HttpContext context) {
    var cloudOfflinePlayer = this.body(context.request()).toInstanceOf(CloudOfflinePlayer.class);
    if (cloudOfflinePlayer == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing player configuration").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.playerManager().updateOfflinePlayer(cloudOfflinePlayer);
    this.response(context, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeleteCloudPlayerRequest(HttpContext context) {
    this.handleWithCloudPlayerContext(context, false, player -> {
      this.playerManager().deleteCloudOfflinePlayer(player);
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleWithCloudPlayerContext(
    HttpContext context,
    boolean mayBeNull,
    Consumer<CloudOfflinePlayer> handler
  ) {
    var identifier = context.request().pathParameters().get("identifier");
    if (identifier == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing player identifier").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // try to find a matching player
    CloudOfflinePlayer player;
    try {
      // try to parse a player unique id from the string
      var uniqueId = UUID.fromString(identifier);
      player = this.playerManager().offlinePlayer(uniqueId);
    } catch (Exception exception) {
      player = this.playerManager().firstOfflinePlayer(identifier);
    }
    // check if a player is present before applying to the handler
    if (player == null && !mayBeNull) {
      this.ok(context)
        .body(this.failure().append("reason", "No player with provided uniqueId/name").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // post to handler
    handler.accept(player);
  }

  protected PlayerManager playerManager() {
    return this.node().servicesRegistry().firstService(PlayerManager.class);
  }

}
