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
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import eu.cloudnetservice.cloudnet.node.template.install.ServiceVersionProvider;
import eu.cloudnetservice.cloudnet.node.template.install.ServiceVersionType;
import eu.cloudnetservice.modules.rest.RestUtil;
import java.io.IOException;
import lombok.NonNull;

public class V2HttpHandlerServiceVersionProvider extends V2HttpHandler {

  public V2HttpHandlerServiceVersionProvider(String requiredPermission) {
    super(requiredPermission, "GET", "POST");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context,
    @NonNull HttpSession session) {
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

  protected void handleVersionListRequest(HttpContext context) {
    this.ok(context)
      .body(this.success().append("versions", this.versionProvider().serviceVersionTypes()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleVersionRequest(HttpContext context) {
    var version = context.request().pathParameters().get("version");
    if (version == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing version identifier").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var serviceVersion = this.versionProvider().getServiceVersionType(version).orElse(null);
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

  protected void handleVersionLoadRequest(HttpContext context) {
    var url = RestUtil.first(context.request().queryParameters().get("url"), null);
    if (url == null) {
      this.versionProvider().loadDefaultVersionTypes();
    } else {
      try {
        if (!this.versionProvider().loadServiceVersionTypes(url)) {
          this.ok(context)
            .body(this.failure().toString())
            .context()
            .closeAfter(true)
            .cancelNext();
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

  protected void handleVersionAddRequest(HttpContext context) {
    var type = this.body(context.request()).toInstanceOf(ServiceVersionType.class);
    if (type == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing specific data").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.versionProvider().registerServiceVersionType(type);
    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected ServiceVersionProvider versionProvider() {
    return this.node().serviceVersionProvider();
  }
}
