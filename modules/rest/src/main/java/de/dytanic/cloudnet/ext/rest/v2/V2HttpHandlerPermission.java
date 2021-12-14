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

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.V2HttpHandler;
import java.util.UUID;
import java.util.function.Consumer;

public class V2HttpHandlerPermission extends V2HttpHandler {

  public V2HttpHandlerPermission(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("exists")) {
        if (path.contains("user")) {
          this.handlePermissionUserExistsRequest(context);
        } else {
          this.handlePermissionGroupExistsRequest(context);
        }
      } else if (path.endsWith("group")) {
        this.handlePermissionGroupList(context);
      } else {
        if (path.contains("user")) {
          this.handlePermissionUserRequest(context);
        } else {
          this.handlePermissionGroupRequest(context);
        }
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      if (path.endsWith("user")) {
        this.handleCreatePermissionUserRequest(context);
      } else {
        this.handleCreatePermissionGroupRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      if (path.contains("user")) {
        this.handleDeletePermissionUserRequest(context);
      } else {
        this.handleDeletePermissionGroupRequest(context);
      }
    }
  }

  protected void handlePermissionGroupList(IHttpContext context) {
    this.ok(context)
      .body(this.success().append("groups", this.getPermissionManagement().getGroups()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handlePermissionGroupExistsRequest(IHttpContext context) {
    this.handleWithPermissionGroupContext(context, true, group -> this.ok(context)
      .body(this.success().append("result", group != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handlePermissionGroupRequest(IHttpContext context) {
    this.handleWithPermissionGroupContext(context, false, group -> this.ok(context)
      .body(this.success().append("group", group).toString())
      .context()
      .closeAfter(true)
      .cancelNext());
  }

  protected void handleCreatePermissionGroupRequest(IHttpContext context) {
    var permissionGroup = this.body(context.request()).toInstanceOf(PermissionGroup.class);
    if (permissionGroup == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing permission group in body").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.getPermissionManagement().addPermissionGroup(permissionGroup);
    this.response(context, HttpResponseCode.HTTP_CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeletePermissionGroupRequest(IHttpContext context) {
    this.handleWithPermissionGroupContext(context, false, group -> {
      this.getPermissionManagement().deletePermissionGroup(group);
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handlePermissionUserExistsRequest(IHttpContext context) {
    this.handleWithPermissionUserContext(context, true, user -> this.ok(context)
      .body(this.success().append("result", user != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handlePermissionUserRequest(IHttpContext context) {
    this.handleWithPermissionUserContext(context, false, user -> this.ok(context)
      .body(this.success().append("user", user).toString())
      .context()
      .closeAfter(true)
      .cancelNext());
  }

  protected void handleCreatePermissionUserRequest(IHttpContext context) {
    var permissionUser = this.body(context.request()).toInstanceOf(PermissionUser.class);
    if (permissionUser == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing permission user").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.getPermissionManagement().addPermissionUser(permissionUser);
    this.response(context, HttpResponseCode.HTTP_CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeletePermissionUserRequest(IHttpContext context) {
    this.handleWithPermissionUserContext(context, false, user -> {
      this.getPermissionManagement().deletePermissionUser(user);
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleWithPermissionGroupContext(
    IHttpContext context,
    boolean mayBeNull,
    Consumer<PermissionGroup> handler
  ) {
    var groupName = context.request().pathParameters().get("group");
    if (groupName == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing permission group name parameter").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // try to get the group
    var group = this.getPermissionManagement().getGroup(groupName);
    if (group == null && !mayBeNull) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown permission group").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // post to the handler
    handler.accept(group);
  }

  protected void handleWithPermissionUserContext(
    IHttpContext context,
    boolean mayBeNull,
    Consumer<PermissionUser> handler
  ) {
    var identifier = context.request().pathParameters().get("user");
    if (identifier == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing identifier parameter").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // try to find a matching player
    PermissionUser user;
    try {
      // try to parse a player unique id from the string
      var uniqueId = UUID.fromString(identifier);
      user = this.getPermissionManagement().getUser(uniqueId);
    } catch (Exception exception) {
      user = this.getPermissionManagement().getFirstUser(identifier);
    }
    // check if the user is present before applying to the handler
    if (user == null && !mayBeNull) {
      this.ok(context)
        .body(this.failure().append("reason", "No permission user with provided uniqueId/name").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // post to handler
    handler.accept(user);
  }

  protected IPermissionManagement getPermissionManagement() {
    return this.getCloudNet().getPermissionManagement();
  }

}
