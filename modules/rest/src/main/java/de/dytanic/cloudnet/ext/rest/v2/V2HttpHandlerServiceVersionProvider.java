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

import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.rest.RestUtils;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.V2HttpHandler;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;

public class V2HttpHandlerServiceVersionProvider extends V2HttpHandler {

  public V2HttpHandlerServiceVersionProvider(String requiredPermission) {
    super(requiredPermission, "GET", "POST");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/serviceversion")) {
        this.handleVersionListRequest(context);
      } else if (path.contains("/load")) {
        this.handleVersionLoadRequest(context);
      } else {
        this.handleVersionRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      this.handleVersionAddRequest(context);
    }
  }

  protected void handleVersionListRequest(IHttpContext context) {
    this.ok(context)
      .body(this.success().append("versions", this.getVersionProvider().getServiceVersionTypes()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleVersionRequest(IHttpContext context) {
    var version = context.request().pathParameters().get("version");
    if (version == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing version identifier").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var serviceVersion = this.getVersionProvider().getServiceVersionType(version).orElse(null);
    if (serviceVersion == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Unknown service version").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.ok(context)
      .body(this.success().append("version", serviceVersion).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleVersionLoadRequest(IHttpContext context) {
    var url = RestUtils.getFirst(context.request().queryParameters().get("url"), null);
    if (url == null) {
      this.getVersionProvider().loadDefaultVersionTypes();
    } else {
      try {
        if (!this.getVersionProvider().loadServiceVersionTypes(url)) {
          this.ok(context).body(this.failure().toString()).context().closeAfter(true).cancelNext();
          return;
        }
      } catch (IOException exception) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Unable to load versions from provided url").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
        return;
      }
    }

    this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
  }

  protected void handleVersionAddRequest(IHttpContext context) {
    var type = this.body(context.request()).toInstanceOf(ServiceVersionType.class);
    if (type == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing specific data").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.getVersionProvider().registerServiceVersionType(type);
    this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
  }

  protected ServiceVersionProvider getVersionProvider() {
    return this.getCloudNet().getServiceVersionProvider();
  }
}
