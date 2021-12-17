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
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.V2HttpHandler;
import java.util.function.Consumer;

public class V2HttpHandlerGroups extends V2HttpHandler {

  public V2HttpHandlerGroups(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/group")) {
        this.handleGroupListRequest(context);
      } else if (path.endsWith("/exists")) {
        this.handleGroupExistsRequest(context);
      } else {
        this.handleGroupRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      this.handleCreateGroupRequest(context);
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      this.handleDeleteGroupRequest(context);
    }
  }

  protected void handleGroupListRequest(IHttpContext context) {
    this.ok(context)
      .body(this.success().append("groups", this.getGroupProvider().groupConfigurations()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleGroupExistsRequest(IHttpContext context) {
    this.handleWithGroupContext(context, name -> this.ok(context)
      .body(this.success().append("result", this.getGroupProvider().isGroupConfigurationPresent(name)).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleGroupRequest(IHttpContext context) {
    this.handleWithGroupContext(context, name -> {
      var configuration = this.getGroupProvider().groupConfiguration(name);
      if (configuration == null) {
        this.ok(context)
          .body(this.failure().append("reason", "Unknown configuration").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("group", configuration).toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleCreateGroupRequest(IHttpContext context) {
    var configuration = this.body(context.request()).toInstanceOf(GroupConfiguration.class);
    if (configuration == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing configuration").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.getGroupProvider().addGroupConfiguration(configuration);
    this.response(context, HttpResponseCode.HTTP_CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeleteGroupRequest(IHttpContext context) {
    this.handleWithGroupContext(context, name -> {
      if (this.getGroupProvider().isGroupConfigurationPresent(name)) {
        this.getGroupProvider().removeGroupConfigurationByName(name);
        this.ok(context)
          .body(this.success().toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.response(context, HttpResponseCode.HTTP_GONE)
          .body(this.failure().append("reason", "No such group").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleWithGroupContext(IHttpContext context, Consumer<String> handler) {
    var groupName = context.request().pathParameters().get("group");
    if (groupName == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing group parameter").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      handler.accept(groupName);
    }
  }

  protected GroupConfigurationProvider getGroupProvider() {
    return this.getCloudNet().groupConfigurationProvider();
  }
}
