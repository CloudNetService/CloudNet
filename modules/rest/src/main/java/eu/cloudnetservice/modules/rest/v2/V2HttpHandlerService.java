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

import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketChannel;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketListener;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.ServiceConsoleLineHandler;
import eu.cloudnetservice.node.service.ServiceConsoleLogCache;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.services")
@ApplyHeaders
public final class V2HttpHandlerService extends V2HttpHandler {

  private final CloudServiceFactory serviceFactory;
  private final CloudServiceManager serviceManager;
  private final ServiceTaskProvider serviceTaskProvider;

  @Inject
  public V2HttpHandlerService(
    @NonNull CloudServiceFactory serviceFactory,
    @NonNull CloudServiceManager serviceManager,
    @NonNull ServiceTaskProvider serviceTaskProvider
  ) {
    this.serviceFactory = serviceFactory;
    this.serviceManager = serviceManager;
    this.serviceTaskProvider = serviceTaskProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service")
  private void handleListServicesRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("services", this.serviceManager.services()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}")
  private void handleServiceRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("id") String id) {
    this.handleWithServiceContext(context, id, service -> this.ok(context)
      .body(this.success().append("snapshot", service).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}/lifecycle", methods = "PATCH")
  private void handleServiceStateUpdateRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("target") String targetState
  ) {
    this.handleWithServiceContext(context, id, service -> {
      switch (StringUtil.toLower(targetState)) {
        case "start" -> service.provider().start();
        case "stop" -> service.provider().stop();
        case "restart" -> service.provider().restart();
        default -> {
          this.badRequest(context)
            .body(this.failure().append("reason", "Invalid target state").toString())
            .context()
            .closeAfter(true)
            .cancelNext(true);
          return;
        }
      }

      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}/command", methods = "POST")
  private void handleServiceCommandRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("id") String id,
    @NonNull @RequestBody Document body
  ) {
    this.handleWithServiceContext(context, id, service -> {
      var commandLine = body.getString("command");
      if (commandLine == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing command line").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        service.provider().runCommand(commandLine);
        this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}/include")
  private void handleIncludeRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("type") String includeType
  ) {
    this.handleWithServiceContext(context, id, service -> {
      switch (StringUtil.toLower(includeType)) {
        case "templates" -> service.provider().includeWaitingServiceTemplates();
        case "inclusions" -> service.provider().includeWaitingServiceInclusions();
        default -> {
          this.badRequest(context)
            .body(this.failure().append("reason", "Invalid include type").toString())
            .context()
            .closeAfter(true)
            .cancelNext(true);
          return;
        }
      }

      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}/deployResources")
  private void handleDeployResourcesRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("id") String id,
    @NonNull @Optional @FirstRequestQueryParam(value = "remove", def = "true") String removeDeployments
  ) {
    this.handleWithServiceContext(context, id, service -> {
      var removeAfterDeploy = Boolean.parseBoolean(removeDeployments);
      service.provider().deployResources(removeAfterDeploy);

      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}/logLines")
  private void handleLogLinesRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("id") String id) {
    this.handleWithServiceContext(context, id, service -> this.ok(context)
      .body(this.success().append("lines", service.provider().cachedLogMessages()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true));
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}/liveLog")
  private void handleLiveLogRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("id") String id) {
    this.handleWithServiceContext(context, id, service -> {
      var cloudService = this.serviceManager.localCloudService(service.serviceId().uniqueId());
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
          .cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/create", methods = "POST")
  private void handleCreateRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    // check for a provided service configuration
    var configuration = body.readObject("serviceConfiguration", ServiceConfiguration.class);
    if (configuration == null) {
      // check for a provided service task
      var serviceTask = body.readObject("task", ServiceTask.class);
      if (serviceTask != null) {
        configuration = ServiceConfiguration.builder(serviceTask).build();
      } else {
        // fallback to a service task name which has to exist
        var serviceTaskName = body.getString("serviceTaskName");
        if (serviceTaskName != null) {
          var task = this.serviceTaskProvider.serviceTask(serviceTaskName);
          if (task != null) {
            configuration = ServiceConfiguration.builder(task).build();
          } else {
            // we got a task but it does not exist
            this.badRequest(context)
              .body(this.failure().append("reason", "Provided task is unknown").toString())
              .context()
              .closeAfter(true)
              .cancelNext(true);
            return;
          }
        } else {
          this.sendInvalidServiceConfigurationResponse(context);
          return;
        }
      }
    }

    var createResult = this.serviceFactory.createCloudService(configuration);
    var start = body.getBoolean("start", false);
    if (start && createResult.state() == ServiceCreateResult.State.CREATED) {
      createResult.serviceInfo().provider().start();
    }

    this.ok(context)
      .body(this.success().append("result", createResult).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}/add", methods = "POST")
  private void handleAddRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("type") String type,
    @NonNull @Optional @FirstRequestQueryParam(value = "flush", def = "false") String flush,
    @NonNull @RequestBody Document body
  ) {
    this.handleWithServiceContext(context, id, service -> {
      var flushAfter = Boolean.parseBoolean(flush);
      switch (StringUtil.toLower(type)) {
        case "template" -> {
          var template = body.readObject("template", ServiceTemplate.class);
          if (template == null) {
            this.badRequest(context)
              .body(this.failure().append("reason", "Missing template in body").toString())
              .context()
              .closeAfter(true)
              .cancelNext(true);
            return;
          } else {
            service.provider().addServiceTemplate(template);
            if (flushAfter) {
              service.provider().includeWaitingServiceTemplates();
            }
          }
        }

        case "deployment" -> {
          var deployment = body.readObject("deployment", ServiceDeployment.class);
          if (deployment == null) {
            this.badRequest(context)
              .body(this.failure().append("reason", "Missing deployment in body").toString())
              .context()
              .closeAfter(true)
              .cancelNext(true);
            return;
          } else {
            service.provider().addServiceDeployment(deployment);
            if (flushAfter) {
              service.provider().deployResources(body.getBoolean("removeDeployments", true));
            }
          }
        }

        case "inclusion" -> {
          var inclusion = body.readObject("inclusion", ServiceRemoteInclusion.class);
          if (inclusion == null) {
            this.badRequest(context)
              .body(this.failure().append("reason", "Missing inclusion in body").toString())
              .context()
              .closeAfter(true)
              .cancelNext(true);
            return;
          } else {
            service.provider().addServiceRemoteInclusion(inclusion);
            if (flushAfter) {
              service.provider().includeWaitingServiceInclusions();
            }
          }
        }

        default -> {
          this.badRequest(context)
            .body(this.failure().append("reason", "Invalid add type").toString())
            .context()
            .closeAfter(true)
            .cancelNext(true);
          return;
        }
      }

      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/service/{id}", methods = "DELETE")
  private void handleServiceDeleteRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("id") String id) {
    this.handleWithServiceContext(context, id, service -> {
      service.provider().delete();
      this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
    });
  }

  private void handleWithServiceContext(
    @NonNull HttpContext context,
    @NonNull String identifier,
    @NonNull Consumer<ServiceInfoSnapshot> handler
  ) {
    // try to find a matching service
    ServiceInfoSnapshot serviceInfoSnapshot;
    try {
      // try to parse a unique id from that
      var serviceId = UUID.fromString(identifier);
      serviceInfoSnapshot = this.serviceManager.service(serviceId);
    } catch (Exception exception) {
      serviceInfoSnapshot = this.serviceManager.serviceByName(identifier);
    }

    // check if the snapshot is present before applying to the handler
    if (serviceInfoSnapshot == null) {
      this.ok(context)
        .body(this.failure().append("reason", "No service with provided uniqueId/name").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    // post to handler
    handler.accept(serviceInfoSnapshot);
  }

  private void sendInvalidServiceConfigurationResponse(@NonNull HttpContext context) {
    this.badRequest(context)
      .body(this.failure().append("reason", "Missing parameters for service creation").toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  protected record ConsoleHandlerWebSocketListener(
    @NonNull CloudService service,
    @NonNull ServiceConsoleLogCache logCache,
    @NonNull ServiceConsoleLineHandler watchingHandler
  ) implements WebSocketListener {

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
