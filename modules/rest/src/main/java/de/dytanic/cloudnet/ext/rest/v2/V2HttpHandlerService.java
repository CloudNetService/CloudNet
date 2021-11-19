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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.rest.RestUtils;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.WebSocketAbleV2HttpHandler;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.IServiceConsoleLogCache;
import de.dytanic.cloudnet.service.ServiceConsoleLineHandler;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class V2HttpHandlerService extends WebSocketAbleV2HttpHandler {

  public V2HttpHandlerService(String requiredPermission) {
    super(
      requiredPermission,
      (context, path) -> context.request().method().equalsIgnoreCase("GET") && path.endsWith("/livelog"),
      "GET", "POST", "DELETE", "PATCH"
    );
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
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
  protected void handleTicketAuthorizedRequest(String path, IHttpContext context, HttpSession session) {
    this.handleLiveLogRequest(context);
  }

  protected void handleListServicesRequest(IHttpContext context) {
    this.ok(context)
      .body(this.success().append("services", this.getGeneralServiceProvider().getCloudServices()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleServiceRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> this.ok(context)
      .body(this.success().append("snapshot", service).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleServiceStateUpdateRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> {
      String targetState = RestUtils.getFirst(context.request().queryParameters().get("target"));
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

  protected void handleServiceCommandRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> {
      String commandLine = this.body(context.request()).getString("command");
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

  protected void handleIncludeRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> {
      String type = RestUtils.getFirst(context.request().queryParameters().get("type"));
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

  protected void handleDeployResourcesRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> {
      boolean removeDeployments = Boolean
        .getBoolean(RestUtils.getFirst(context.request().queryParameters().get("remove"), "true"));
      service.provider().deployResources(removeDeployments);

      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
    });
  }

  protected void handleLogLinesRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> this.ok(context)
      .body(this.success().append("lines", service.provider().getCachedLogMessages()).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleLiveLogRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> {
      ICloudService cloudService = this.getCloudNet().getCloudServiceProvider()
        .getLocalCloudService(service.getServiceId().getUniqueId());
      if (cloudService != null) {
        IWebSocketChannel webSocketChannel = context.upgrade();
        if (webSocketChannel == null) {
          return;
        }

        ServiceConsoleLineHandler handler = (console, line) -> webSocketChannel
          .sendWebSocketFrame(WebSocketFrameType.TEXT, line);
        cloudService.getServiceConsoleLogCache().addHandler(handler);

        webSocketChannel
          .addListener(
            new ConsoleHandlerWebSocketListener(cloudService, cloudService.getServiceConsoleLogCache(), handler));
      } else {
        this.badRequest(context)
          .body(this.failure().append("reason", "Service is unknown or not running on this node").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleCreateRequest(@NotNull IHttpContext context) {
    JsonDocument body = this.body(context.request());
    // check for a provided service configuration
    ServiceConfiguration configuration = body.get("serviceConfiguration", ServiceConfiguration.class);
    if (configuration == null) {
      // check for a provided service task
      ServiceTask serviceTask = body.get("task", ServiceTask.class);
      if (serviceTask != null) {
        configuration = ServiceConfiguration.builder(serviceTask).build();
      } else {
        // fallback to a service task name which has to exist
        String serviceTaskName = body.getString("serviceTaskName");
        if (serviceTaskName != null) {
          ServiceTask task = this.getCloudNet().getServiceTaskProvider().getServiceTask(serviceTaskName);
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

    ServiceInfoSnapshot snapshot = this.getServiceFactory().createCloudService(configuration);
    if (snapshot != null) {
      boolean start = body.getBoolean("start", false);
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

  protected void handleAddRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> {
      String type = RestUtils.getFirst(context.request().queryParameters().get("type"), null);
      if (type == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing type in query params").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        JsonDocument body = this.body(context.request());
        boolean flushAfter = Boolean
          .getBoolean(RestUtils.getFirst(context.request().queryParameters().get("flush"), "false"));

        if (type.equalsIgnoreCase("template")) {
          ServiceTemplate template = body.get("template", ServiceTemplate.class);
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
          ServiceDeployment deployment = body.get("deployment", ServiceDeployment.class);
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
          ServiceRemoteInclusion inclusion = body.get("inclusion", ServiceRemoteInclusion.class);
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

  protected void handleServiceDeleteRequest(IHttpContext context) {
    this.handleWithServiceContext(context, service -> {
      service.provider().delete();
      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
    });
  }

  protected void handleWithServiceContext(IHttpContext context, Consumer<ServiceInfoSnapshot> handler) {
    String identifier = context.request().pathParameters().get("identifier");
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
      UUID serviceId = UUID.fromString(identifier);
      serviceInfoSnapshot = this.getServiceById(serviceId);
    } catch (Exception exception) {
      serviceInfoSnapshot = this.getServiceByName(identifier);
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

  protected void sendInvalidServiceConfigurationResponse(IHttpContext context) {
    this.badRequest(context)
      .body(this.failure().append("reason", "Missing parameters for service creation").toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected GeneralCloudServiceProvider getGeneralServiceProvider() {
    return this.getCloudNet().getCloudServiceProvider();
  }

  protected CloudServiceFactory getServiceFactory() {
    return this.getCloudNet().getCloudServiceFactory();
  }

  protected ServiceInfoSnapshot getServiceByName(String name) {
    return this.getGeneralServiceProvider().getCloudServiceByName(name);
  }

  protected ServiceInfoSnapshot getServiceById(UUID uniqueID) {
    return this.getGeneralServiceProvider().getCloudService(uniqueID);
  }

  protected static class ConsoleHandlerWebSocketListener implements IWebSocketListener {

    protected final ICloudService service;
    protected final IServiceConsoleLogCache logCache;
    protected final ServiceConsoleLineHandler watchingHandler;

    public ConsoleHandlerWebSocketListener(ICloudService service,
      IServiceConsoleLogCache logCache, ServiceConsoleLineHandler watchingHandler) {
      this.service = service;
      this.logCache = logCache;
      this.watchingHandler = watchingHandler;
    }

    @Override
    public void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) throws Exception {
      if (type == WebSocketFrameType.TEXT) {
        String commandLine = new String(bytes, StandardCharsets.UTF_8);
        this.service.runCommand(commandLine);
      }
    }

    @Override
    public void handleClose(IWebSocketChannel channel, AtomicInteger statusCode, AtomicReference<String> reasonText) {
      this.logCache.removeHandler(this.watchingHandler);
    }
  }
}
