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

package eu.cloudnetservice.modules.bridge.node.http;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpHandler;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.bridge")
@ApplyHeaders
public final class V2HttpHandlerBridge extends V2HttpHandler {

  private final PlayerManager playerManager;

  @Inject
  public V2HttpHandlerBridge(@NonNull PlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/player/onlineCount", priority = HttpHandler.PRIORITY_HIGH)
  private void handleOnlinePlayerCountRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("onlineCount", this.playerManager.onlineCount()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/player/registeredCount", priority = HttpHandler.PRIORITY_HIGH)
  private void handleRegisteredPlayerCountRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("registeredCount", this.playerManager.registeredCount()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/player/{id}/exists")
  private void handleCloudPlayerExistsRequest(@NonNull HttpContext ctx, @NonNull @RequestPathParam("id") String id) {
    this.handleWithCloudPlayerContext(ctx, id, true, player -> this.ok(ctx)
      .body(this.success().append("result", player != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/player/{id}")
  private void handleCloudPlayerRequest(@NonNull HttpContext ctx, @NonNull @RequestPathParam("id") String id) {
    this.handleWithCloudPlayerContext(ctx, id, false, player -> this.ok(ctx)
      .body(this.success().append("player", player).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/player/{id}", methods = "POST")
  private void handleCreateCloudPlayerRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    var cloudOfflinePlayer = body.toInstanceOf(CloudOfflinePlayer.class);
    if (cloudOfflinePlayer == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing player configuration").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    this.playerManager.updateOfflinePlayer(cloudOfflinePlayer);
    this.response(context, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/player/{id}", methods = "DELETE")
  private void handleCloudPlayerDeleteRequest(@NonNull HttpContext ctx, @NonNull @RequestPathParam("id") String id) {
    this.handleWithCloudPlayerContext(ctx, id, false, player -> {
      this.playerManager.deleteCloudOfflinePlayer(player);
      this.ok(ctx)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  private void handleWithCloudPlayerContext(
    @NonNull HttpContext context,
    @NonNull String identifier,
    boolean mayBeNull,
    @NonNull Consumer<CloudOfflinePlayer> handler
  ) {
    // try to find a matching player
    CloudOfflinePlayer player;
    try {
      // try to parse a player unique id from the string
      var uniqueId = UUID.fromString(identifier);
      player = this.playerManager.offlinePlayer(uniqueId);
    } catch (Exception exception) {
      player = this.playerManager.firstOfflinePlayer(identifier);
    }

    // check if a player is present before applying to the handler
    if (player == null && !mayBeNull) {
      this.ok(context)
        .body(this.failure().append("reason", "No player with provided uniqueId/name").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    // post to handler
    handler.accept(player);
  }
}
