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

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.driver.module.ModuleProvider;
import eu.cloudnetservice.cloudnet.driver.module.ModuleWrapper;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerModule extends V2HttpHandler {

  public V2HttpHandlerModule(String requiredPermission) {
    super(requiredPermission, "GET", "POST");
  }

  @Override
  protected void handleBearerAuthorized(String path, HttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/module")) {
        this.handleModuleListRequest(context);
      } else if (path.endsWith("/module/reload")) {
        this.handleReloadRequest(context);
      } else if (path.endsWith("/reload")) {
        this.handleModuleReloadRequest(context);
      } else if (path.endsWith("/unload")) {
        this.handleModuleUnloadRequest(context);
      } else if (path.endsWith("/config")) {
        this.handleModuleConfigRequest(context);
      } else {
        this.handleModuleRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      if (path.endsWith("/load")) {
        this.handleModuleLoadRequest(context);
      } else if (path.endsWith("/config")) {
        this.handleConfigUpdateRequest(context);
      }
    }
  }

  protected void handleReloadRequest(HttpContext context) {
    this.node().moduleProvider().reloadAll();

    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleModuleListRequest(HttpContext context) {
    this.ok(context).body(this.success().append("modules", this.moduleProvider().modules().stream()
        .map(module -> JsonDocument.newDocument("lifecycle", module.moduleLifeCycle())
          .append("configuration", module.moduleConfiguration()))
        .collect(Collectors.toList()))
      .toString()
    ).context().closeAfter(true).cancelNext();
  }

  protected void handleModuleRequest(HttpContext context) {
    this.handleWithModuleContext(context, module -> this.showModule(context, module));
  }

  protected void handleModuleReloadRequest(HttpContext context) {
    this.handleWithModuleContext(context, module -> {
      module.reloadModule();

      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleModuleUnloadRequest(HttpContext context) {
    this.handleWithModuleContext(context, module -> {
      module.unloadModule();
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleModuleLoadRequest(HttpContext context) {
    var moduleStream = context.request().bodyStream();
    if (moduleStream == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing module data in body").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var name = context.request().pathParameters().get("name");
    if (name == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing module name in path").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var moduleTarget = this.moduleProvider().moduleDirectoryPath().resolve(name);
    FileUtils.ensureChild(this.moduleProvider().moduleDirectoryPath(), moduleTarget);

    try (var outputStream = Files.newOutputStream(moduleTarget)) {
      FileUtils.copy(moduleStream, outputStream);
    } catch (IOException exception) {
      this.response(context, HttpResponseCode.HTTP_INTERNAL_ERROR)
        .body(this.failure().append("reason", "Unable to copy module file").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      FileUtils.delete(moduleTarget);
      return;
    }

    this.showModule(context, this.moduleProvider().loadModule(moduleTarget));
  }

  protected void handleModuleConfigRequest(HttpContext context) {
    this.handleWithModuleContext(context, module -> {
      if (module.module() instanceof DriverModule) {
        var config = ((DriverModule) module.module()).readConfig();
        this.ok(context)
          .body(this.success().append("config", config).toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.failure().append("reason", "Module was not loaded by CloudNet").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleConfigUpdateRequest(HttpContext context) {
    this.handleWithModuleContext(context, module -> {
      if (module.module() instanceof DriverModule) {
        var stream = context.request().bodyStream();
        if (stream == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "Missing data in body").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
        } else {
          var driverModule = (DriverModule) module.module();
          driverModule.writeConfig(JsonDocument.newDocument(stream));

          this.ok(context)
            .body(this.success().toString())
            .context()
            .closeAfter(true)
            .cancelNext();
        }
      } else {
        this.ok(context)
          .body(this.failure().append("reason", "Module was not loaded by CloudNet").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void showModule(HttpContext context, @Nullable ModuleWrapper wrapper) {
    if (wrapper == null) {
      this.ok(context)
        .body(this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.ok(context)
        .body(this.success()
        .append("lifecycle", wrapper.moduleLifeCycle())
        .append("configuration", wrapper.moduleConfiguration())
        .toString()
      ).context().closeAfter(true).cancelNext();
    }
  }

  protected void handleWithModuleContext(HttpContext context, Consumer<ModuleWrapper> handler) {
    var name = context.request().pathParameters().get("name");
    if (name == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing module name in path").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var wrapper = this.moduleProvider().module(name);
    if (wrapper == null) {
      this.notFound(context)
        .body(this.failure().append("reason", "No such module").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    handler.accept(wrapper);
  }

  protected ModuleProvider moduleProvider() {
    return this.node().moduleProvider();
  }
}
