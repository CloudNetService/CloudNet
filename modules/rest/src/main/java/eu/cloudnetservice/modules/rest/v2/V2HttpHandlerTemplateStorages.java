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

import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class V2HttpHandlerTemplateStorages extends V2HttpHandler {

  public V2HttpHandlerTemplateStorages(String requiredPermission) {
    super(requiredPermission, "GET");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context,
    @NonNull HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/templatestorage")) {
        this.handleStorageListRequest(context);
      } else if (path.endsWith("/templates")) {
        this.handleTemplateListRequest(context);
      }
    }
  }

  protected void handleStorageListRequest(HttpContext context) {
    this.ok(context)
      .body(this.success().append("storages", this.node().availableTemplateStorages().stream()
        .map(Nameable::name).collect(Collectors.toList())).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleTemplateListRequest(HttpContext context) {
    this.handleWithStorageContext(context, templateStorage -> this.ok(context)
      .body(this.success().append("templates", templateStorage.templates()).toString())
      .context()
      .closeAfter(true)
      .cancelNext());
  }

  protected void handleWithStorageContext(HttpContext context, Consumer<TemplateStorage> handler) {
    var storage = context.request().pathParameters().get("storage");
    if (storage == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing template storage in path params").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var templateStorage = this.node().templateStorage(storage);
    if (templateStorage == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Unknown template storage").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    handler.accept(templateStorage);
  }
}
