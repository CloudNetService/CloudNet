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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.cloudnet.driver.service.GroupConfiguration;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerGroups extends V2HttpHandler {

  public V2HttpHandlerGroups(@Nullable String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context, @NonNull HttpSession ses) {
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

  protected void handleGroupListRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("groups", this.groupProvider().groupConfigurations()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleGroupExistsRequest(@NonNull HttpContext context) {
    this.handleWithGroupContext(context, name -> this.ok(context)
      .body(this.success().append("result", this.groupProvider().groupConfiguration(name) != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleGroupRequest(@NonNull HttpContext context) {
    this.handleWithGroupContext(context, name -> {
      var configuration = this.groupProvider().groupConfiguration(name);
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

  protected void handleCreateGroupRequest(@NonNull HttpContext context) {
    var configuration = this.body(context.request()).toInstanceOf(GroupConfiguration.class);
    if (configuration == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing configuration").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.groupProvider().addGroupConfiguration(configuration);
    this.response(context, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleDeleteGroupRequest(@NonNull HttpContext context) {
    this.handleWithGroupContext(context, name -> {
      if (this.groupProvider().groupConfiguration(name) != null) {
        this.groupProvider().removeGroupConfigurationByName(name);
        this.ok(context)
          .body(this.success().toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.response(context, HttpResponseCode.GONE)
          .body(this.failure().append("reason", "No such group").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleWithGroupContext(@NonNull HttpContext context, @NonNull Consumer<String> handler) {
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

  protected @NonNull GroupConfigurationProvider groupProvider() {
    return this.node().groupConfigurationProvider();
  }
}
