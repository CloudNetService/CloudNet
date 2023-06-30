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

import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.Consumer;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.template.storage")
@ApplyHeaders
public final class V2HttpHandlerTemplateStorage extends V2HttpHandler {

  private final TemplateStorageProvider templateStorageProvider;

  @Inject
  public V2HttpHandlerTemplateStorage(@NonNull TemplateStorageProvider storageProvider) {
    this.templateStorageProvider = storageProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/templateStorage")
  private void handleStorageListRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success()
        .append("storages", this.templateStorageProvider.availableTemplateStorages())
        .toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/templateStorage/{storage}/templates")
  private void handleTemplateListRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storage
  ) {
    this.handleWithStorageContext(context, storage, templateStorage -> this.ok(context)
      .body(this.success().append("templates", templateStorage.templates()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  private void handleWithStorageContext(
    @NonNull HttpContext context,
    @NonNull String storage,
    @NonNull Consumer<TemplateStorage> handler
  ) {
    var templateStorage = this.templateStorageProvider.templateStorage(storage);
    if (templateStorage == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Unknown template storage").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    handler.accept(templateStorage);
  }
}
