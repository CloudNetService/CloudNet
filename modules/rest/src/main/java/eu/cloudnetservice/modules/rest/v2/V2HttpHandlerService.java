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

import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketChannel;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketListener;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.modules.rest.RestUtil;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.WebSocketAbleV2HttpHandler;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.ServiceConsoleLineHandler;
import eu.cloudnetservice.node.service.ServiceConsoleLogCache;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerService extends WebSocketAbleV2HttpHandler {

  public V2HttpHandlerService(@Nullable String requiredPermission) {
    super(
      requiredPermission,
      (context, path) -> context.request().method().equalsIgnoreCase("GET") && path.endsWith("/livelog"),
      "GET", "POST", "DELETE", "PATCH"
    );
  }

  @Override
  protected void handleBearerAuthorized(
    @NonNull String path,
    @NonNull HttpContext context,
    @NonNull HttpSession session
  ) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/service")) {
        this.handleListServicesRequest(context);
      } else if (path.contains("/include")) {
        this.handleIncludeRequest(context);
      } else if (path.endsWith("/deployresources")) {
        this.handleDeployResourcesRequest(context);
      } else if (path.endsWith("/loglines")) {
        this.handleLogLinesRequest(context);
      } else if (path.endsWith("/livelog")) {
        this.handleLiveLogRequest(context);
      } else {
        this.handleServiceRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      if (path.endsWith("/create")) {
        this.handleCreateRequest(context);
      } else if (path.contains("/add")) {
        this.handleAddRequest(context);
      } else if (path.endsWith("/command")) {
        this.handleServiceCommandRequest(context);
      }
    } else if (context.request().method().equals("PATCH")) {
      this.handleServiceStateUpdateRequest(context);
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      this.handleServiceDeleteRequest(context);
    }
  }

  @Override
  protected void handleTicketAuthorizedRequest(
    @NonNull String path,
    @NonNull HttpContext context,
    @NonNull HttpSession session
  ) {
    this.handleLiveLogRequest(context);
  }

  protected void handleListServicesRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("services", this.generalServiceProvider().services()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleServiceRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> this.ok(context)
      .body(this.success().append("snapshot", service).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleServiceStateUpdateRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> {
      var targetState = RestUtil.first(context.request().queryParameters().get("target"));
      if (targetState == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing target state in query").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
        return;
      }

      if (targetState.equalsIgnoreCase("start")) {
        service.provider().start();
      } else if (targetState.equalsIgnoreCase("stop")) {
        service.provider().stop();
      } else if (targetState.equalsIgnoreCase("restart")) {
        service.provider().restart();
      } else {
        this.badRequest(context)
          .body(this.failure().append("reason", "Invalid target state").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
        return;
      }

      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
    });
  }

  protected void handleServiceCommandRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> {
      var commandLine = this.body(context.request()).getString("command");
      if (commandLine == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing command line").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        service.provider().runCommand(commandLine);
        this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
      }
    });
  }

  protected void handleIncludeRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> {
      var type = RestUtil.first(context.request().queryParameters().get("type"));
      if (type != null) {
        if (type.equalsIgnoreCase("templates")) {
          service.provider().includeWaitingServiceTemplates();
        } else if (type.equalsIgnoreCase("inclusions")) {
          service.provider().includeWaitingServiceInclusions();
        } else {
          this.badRequest(context)
            .body(this.failure().append("reason", "Invalid include type").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }

        this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
      } else {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing inclusion types in query params").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleDeployResourcesRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> {
      var removeDeployments = Boolean
        .getBoolean(RestUtil.first(context.request().queryParameters().get("remove"), "true"));
      service.provider().deployResources(removeDeployments);

      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
    });
  }

  protected void handleLogLinesRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> this.ok(context)
      .body(this.success().append("lines", service.provider().cachedLogMessages()).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleLiveLogRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> {
      var cloudService = this.node().cloudServiceProvider().localCloudService(service.serviceId().uniqueId());
      if (cloudService != null) {
        context.upgrade().thenAccept(channel -> {
          ServiceConsoleLineHandler handler = (console, line, stderr) -> channel.sendWebSocketFrame(
            WebSocketFrameType.TEXT,
            line);
          cloudService.serviceConsoleLogCache().addHandler(handler);

          channel.addListener(new ConsoleHandlerWebSocketListener(
            cloudService,
            cloudService.serviceConsoleLogCache(),
            handler));
        });
      } else {
        this.badRequest(context)
          .body(this.failure().append("reason", "Service is unknown or not running on this node").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleCreateRequest(@NonNull HttpContext context) {
    var body = this.body(context.request());
    // check for a provided service configuration
    var configuration = body.get("serviceConfiguration", ServiceConfiguration.class);
    if (configuration == null) {
      // check for a provided service task
      var serviceTask = body.get("task", ServiceTask.class);
      if (serviceTask != null) {
        configuration = ServiceConfiguration.builder(serviceTask).build();
      } else {
        // fallback to a service task name which has to exist
        var serviceTaskName = body.getString("serviceTaskName");
        if (serviceTaskName != null) {
          var task = this.node().serviceTaskProvider().serviceTask(serviceTaskName);
          if (task != null) {
            configuration = ServiceConfiguration.builder(task).build();
          } else {
            // we got a task but it does not exist
            this.badRequest(context)
              .body(this.failure().append("reason", "Provided task is unknown").toString())
              .context()
              .closeAfter(true)
              .cancelNext();
            return;
          }
        } else {
          this.sendInvalidServiceConfigurationResponse(context);
          return;
        }
      }
    }

    var snapshot = this.serviceFactory().createCloudService(configuration);
    if (snapshot != null) {
      var start = body.getBoolean("start", false);
      if (start) {
        snapshot.provider().start();
      }

      this.ok(context)
        .body(this.success().append("snapshot", snapshot).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.ok(context)
        .body(this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected void handleAddRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> {
      var type = RestUtil.first(context.request().queryParameters().get("type"), null);
      if (type == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing type in query params").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        var body = this.body(context.request());
        var flushAfter = Boolean
          .getBoolean(RestUtil.first(context.request().queryParameters().get("flush"), "false"));

        if (type.equalsIgnoreCase("template")) {
          var template = body.get("template", ServiceTemplate.class);
          if (template == null) {
            this.badRequest(context)
              .body(this.failure().append("reason", "Missing template in body").toString())
              .context()
              .closeAfter(true)
              .cancelNext();
            return;
          } else {
            service.provider().addServiceTemplate(template);
            if (flushAfter) {
              service.provider().includeWaitingServiceTemplates();
            }
          }
        } else if (type.equalsIgnoreCase("deployment")) {
          var deployment = body.get("deployment", ServiceDeployment.class);
          if (deployment == null) {
            this.badRequest(context)
              .body(this.failure().append("reason", "Missing deployment in body").toString())
              .context()
              .closeAfter(true)
              .cancelNext();
            return;
          } else {
            service.provider().addServiceDeployment(deployment);
            if (flushAfter) {
              service.provider().deployResources(body.getBoolean("removeDeployments", true));
            }
          }
        } else if (type.equalsIgnoreCase("inclusion")) {
          var inclusion = body.get("inclusion", ServiceRemoteInclusion.class);
          if (inclusion == null) {
            this.badRequest(context)
              .body(this.failure().append("reason", "Missing inclusion in body").toString())
              .context()
              .closeAfter(true)
              .cancelNext();
            return;
          } else {
            service.provider().addServiceRemoteInclusion(inclusion);
            if (flushAfter) {
              service.provider().includeWaitingServiceInclusions();
            }
          }
        } else {
          this.badRequest(context)
            .body(this.failure().append("reason", "Invalid add type").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }

        this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
      }
    });
  }

  protected void handleServiceDeleteRequest(@NonNull HttpContext context) {
    this.handleWithServiceContext(context, service -> {
      service.provider().delete();
      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
    });
  }

  protected void handleWithServiceContext(
    @NonNull HttpContext context,
    @NonNull Consumer<ServiceInfoSnapshot> handler
  ) {
    var identifier = context.request().pathParameters().get("identifier");
    if (identifier == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing service identifier").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // try to find a matching service
    ServiceInfoSnapshot serviceInfoSnapshot;
    try {
      // try to parse a unique id from that
      var serviceId = UUID.fromString(identifier);
      serviceInfoSnapshot = this.serviceById(serviceId);
    } catch (Exception exception) {
      serviceInfoSnapshot = this.serviceByName(identifier);
    }
    // check if the snapshot is present before applying to the handler
    if (serviceInfoSnapshot == null) {
      this.ok(context)
        .body(this.failure().append("reason", "No service with provided uniqueId/name").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }
    // post to handler
    handler.accept(serviceInfoSnapshot);
  }

  protected void sendInvalidServiceConfigurationResponse(@NonNull HttpContext context) {
    this.badRequest(context)
      .body(this.failure().append("reason", "Missing parameters for service creation").toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected @NonNull CloudServiceProvider generalServiceProvider() {
    return this.node().cloudServiceProvider();
  }

  protected @NonNull CloudServiceFactory serviceFactory() {
    return this.node().cloudServiceFactory();
  }

  protected @Nullable ServiceInfoSnapshot serviceByName(String name) {
    return this.generalServiceProvider().serviceByName(name);
  }

  protected @Nullable ServiceInfoSnapshot serviceById(UUID uniqueID) {
    return this.generalServiceProvider().service(uniqueID);
  }

  protected static class ConsoleHandlerWebSocketListener implements WebSocketListener {

    protected final CloudService service;
    protected final ServiceConsoleLogCache logCache;
    protected final ServiceConsoleLineHandler watchingHandler;

    public ConsoleHandlerWebSocketListener(
      @NonNull CloudService service,
      @NonNull ServiceConsoleLogCache logCache,
      @NonNull ServiceConsoleLineHandler watchingHandler
    ) {
      this.service = service;
      this.logCache = logCache;
      this.watchingHandler = watchingHandler;
    }

    @Override
    public void handle(
      @NonNull WebSocketChannel channel,
      @NonNull WebSocketFrameType type,
      byte[] bytes
    ) {
      if (type == WebSocketFrameType.TEXT) {
        var commandLine = new String(bytes, StandardCharsets.UTF_8);
        this.service.runCommand(commandLine);
      }
    }

    @Override
    public void handleClose(
      @NonNull WebSocketChannel channel,
      @NonNull AtomicInteger statusCode,
      @NonNull AtomicReference<String> reasonText
    ) {
      this.logCache.removeHandler(this.watchingHandler);
    }
  }
}
