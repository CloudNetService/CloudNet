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
import eu.cloudnetservice.driver.network.http.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import eu.cloudnetservice.node.version.ServiceVersionType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@HandlerPermission("http.v2.service.provider")
@ApplyHeaders
public final class V2HttpHandlerServiceVersionProvider extends V2HttpHandler {

  private final ServiceVersionProvider versionProvider;

  @Inject
  public V2HttpHandlerServiceVersionProvider(
    @NonNull ServiceVersionProvider versionProvider
  ) {
    this.versionProvider = versionProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/serviceversion")
  private void handleVersionListRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("versions", this.versionProvider.serviceVersionTypes()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/serviceversion/{version}")
  private void handleVersionRequest(@NonNull HttpContext ctx, @NonNull @RequestPathParam("version") String version) {
    var serviceVersion = this.versionProvider.getServiceVersionType(version);
    if (serviceVersion == null) {
      this.badRequest(ctx)
        .body(this.failure().append("reason", "Unknown service version").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    this.ok(ctx)
      .body(this.success().append("version", serviceVersion).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/serviceversion/load")
  private void handleVersionLoadRequest(
    @NonNull HttpContext context,
    @Nullable @Optional @FirstRequestQueryParam("url") String url
  ) {
    if (url == null) {
      this.versionProvider.loadDefaultVersionTypes();
    } else {
      try {
        if (!this.versionProvider.loadServiceVersionTypes(url)) {
          this.ok(context)
            .body(this.failure().toString())
            .context()
            .closeAfter(true)
            .cancelNext(true);
          return;
        }
      } catch (IOException exception) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Unable to load versions from provided url").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
        return;
      }
    }

    this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/serviceversion/add", methods = "POST")
  private void handleVersionAddRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    var type = body.toInstanceOf(ServiceVersionType.class);
    if (type == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing specific data").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    this.versionProvider.registerServiceVersionType(type);
    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }
}
