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
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
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
      .body(this.success().append("groups", this.getPermissionManagement().getGroups()).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handlePermissionGroupExistsRequest(IHttpContext context) {
    this.handleWithPermissionGroupContext(context, name -> this.ok(context)
      .body(this.success().append("result", this.getPermissionManagement().containsGroup(name)).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handlePermissionGroupRequest(IHttpContext context) {
    this.handleWithPermissionGroupContext(context, name -> {
      IPermissionGroup permissionGroup = this.getPermissionManagement().getGroup(name);
      if (permissionGroup == null) {
        this.ok(context)
          .body(this.failure().append("reason", "Unknown configuration").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("group", permissionGroup).toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleCreatePermissionGroupRequest(IHttpContext context) {
    IPermissionGroup permissionGroup = this.body(context.request()).toInstanceOf(IPermissionGroup.class);
    if (permissionGroup == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing configuration").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.getPermissionManagement().addGroup(permissionGroup);
    this.response(context, HttpResponseCode.HTTP_CREATED)
      .body(this.success().toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeletePermissionGroupRequest(IHttpContext context) {
    this.handleWithPermissionGroupContext(context, name -> {
      if (this.getPermissionManagement().containsGroup(name)) {
        this.getPermissionManagement().deleteGroup(name);
        this.ok(context)
          .body(this.success().toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.response(context, HttpResponseCode.HTTP_GONE)
          .body(this.failure().append("reason", "No such group").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleWithPermissionGroupContext(IHttpContext context, Consumer<String> handler) {
    String groupName = context.request().pathParameters().get("group");
    if (groupName == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing group parameter").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      handler.accept(groupName);
    }
  }

  protected void handlePermissionUserExistsRequest(IHttpContext context) {
    this.handleWithPermissionUserContext(context, name -> this.ok(context)
      .body(this.success().append("result", this.getPermissionManagement().containsUser(name)).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handlePermissionUserRequest(IHttpContext context) {
    this.handleWithPermissionUserContext(context, name -> {
      IPermissionUser permissionUser = this.getPermissionManagement().getFirstUser(name);
      if (permissionUser == null) {
        this.ok(context)
          .body(this.failure().append("reason", "Unknown user").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("user", permissionUser).toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleCreatePermissionUserRequest(IHttpContext context) {
    IPermissionUser permissionUser = this.body(context.request()).toInstanceOf(IPermissionUser.class);
    if (permissionUser == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing user configuration").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.getPermissionManagement().addUser(permissionUser);
    this.response(context, HttpResponseCode.HTTP_CREATED)
      .body(this.success().toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeletePermissionUserRequest(IHttpContext context) {
    this.handleWithPermissionUserContext(context, name -> {
      if (this.getPermissionManagement().containsUser(name)) {
        this.getPermissionManagement().deleteUser(name);
        this.ok(context)
          .body(this.success().toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.response(context, HttpResponseCode.HTTP_GONE)
          .body(this.failure().append("reason", "No such user").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });

  }

  protected void handleWithPermissionUserContext(IHttpContext context, Consumer<String> handler) {
    String permissionUserName = context.request().pathParameters().get("user");
    if (permissionUserName == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing user parameter").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      handler.accept(permissionUserName);
    }
  }

  protected IPermissionManagement getPermissionManagement() {
    return this.getCloudNet().getPermissionManagement();
  }

}
