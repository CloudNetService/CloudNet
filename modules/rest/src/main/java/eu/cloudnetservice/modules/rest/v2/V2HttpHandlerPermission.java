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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
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
@HandlerPermission("http.v2.permission")
@ApplyHeaders
public final class V2HttpHandlerPermission extends V2HttpHandler {

  private final PermissionManagement permissionManagement;

  @Inject
  public V2HttpHandlerPermission(@NonNull PermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/group")
  private void handlePermissionGroupList(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("groups", this.permissionManagement.groups()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/group/{name}/exists")
  private void handlePermissionGroupExistsRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name
  ) {
    this.handleWithPermissionGroupContext(context, name, true, group -> this.ok(context)
      .body(this.success().append("result", group != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/group/{name}")
  private void handlePermissionGroupRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name
  ) {
    this.handleWithPermissionGroupContext(context, name, false, group -> this.ok(context)
      .body(this.success().append("group", group).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/group", methods = "POST")
  private void handleCreatePermissionGroupRequest(@NonNull HttpContext ctx, @NonNull @RequestBody Document body) {
    var permissionGroup = body.toInstanceOf(PermissionGroup.class);
    if (permissionGroup == null) {
      this.badRequest(ctx)
        .body(this.failure().append("reason", "Missing permission group in body").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    this.permissionManagement.addPermissionGroup(permissionGroup);
    this.response(ctx, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/group/{name}", methods = "DELETE")
  private void handleDeletePermissionGroupRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name
  ) {
    this.handleWithPermissionGroupContext(context, name, false, group -> {
      this.permissionManagement.deletePermissionGroup(group);
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/user/{id}/exists")
  private void handlePermissionUserExistsRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("id") String id
  ) {
    this.handleWithPermissionUserContext(context, id, true, user -> this.ok(context)
      .body(this.success().append("result", user != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/user/{id}")
  private void handlePermissionUserRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("id") String id) {
    this.handleWithPermissionUserContext(context, id, false, user -> this.ok(context)
      .body(this.success().append("user", user).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/user", methods = "POST")
  private void handleCreatePermissionUserRequest(@NonNull HttpContext context) {
    var permissionUser = this.body(context.request()).toInstanceOf(PermissionUser.class);
    if (permissionUser == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing permission user").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    this.permissionManagement.addPermissionUser(permissionUser);
    this.response(context, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/permission/user/{id}", methods = "DELETE")
  private void handleDeletePermissionUserRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("id") String id
  ) {
    this.handleWithPermissionUserContext(context, id, false, user -> {
      this.permissionManagement.deletePermissionUser(user);
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  private void handleWithPermissionGroupContext(
    @NonNull HttpContext context,
    @NonNull String name,
    boolean mayBeNull,
    @NonNull Consumer<PermissionGroup> handler
  ) {
    // try to get the group
    var group = this.permissionManagement.group(name);
    if (group == null && !mayBeNull) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown permission group").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    // post to the handler
    handler.accept(group);
  }

  private void handleWithPermissionUserContext(
    @NonNull HttpContext context,
    @NonNull String identifier,
    boolean mayBeNull,
    @NonNull Consumer<PermissionUser> handler
  ) {
    // try to find a matching player
    PermissionUser user;
    try {
      // try to parse a player unique id from the string
      var uniqueId = UUID.fromString(identifier);
      user = this.permissionManagement.user(uniqueId);
    } catch (Exception exception) {
      user = this.permissionManagement.firstUser(identifier);
    }

    // check if the user is present before applying to the handler
    if (user == null && !mayBeNull) {
      this.ok(context)
        .body(this.failure().append("reason", "No permission user with provided uniqueId/name").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    // post to handler
    handler.accept(user);
  }
}
