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
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.groups")
@ApplyHeaders
public final class V2HttpHandlerGroup extends V2HttpHandler {

  private final GroupConfigurationProvider groupProvider;

  @Inject
  public V2HttpHandlerGroup(@NonNull GroupConfigurationProvider groupProvider) {
    this.groupProvider = groupProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/group")
  private void handleGroupListRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("groups", this.groupProvider.groupConfigurations()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/group/{name}/exists")
  private void handleGroupExistsRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name
  ) {
    this.ok(context)
      .body(this.success().append("result", this.groupProvider.groupConfiguration(name) != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/group/{name}")
  private void handleGroupRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("name") String name) {
    var configuration = this.groupProvider.groupConfiguration(name);
    if (configuration == null) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown configuration").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.ok(context)
        .body(this.success().append("group", configuration).toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/group", methods = "POST")
  private void handleCreateGroupRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    var configuration = body.toInstanceOf(GroupConfiguration.class);
    if (configuration == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing configuration").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    this.groupProvider.addGroupConfiguration(configuration);
    this.response(context, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/group/{name}", methods = "DELETE")
  private void handleDeleteGroupRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name
  ) {
    if (this.groupProvider.groupConfiguration(name) != null) {
      this.groupProvider.removeGroupConfigurationByName(name);
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.response(context, HttpResponseCode.GONE)
        .body(this.failure().append("reason", "No such group").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }
}
