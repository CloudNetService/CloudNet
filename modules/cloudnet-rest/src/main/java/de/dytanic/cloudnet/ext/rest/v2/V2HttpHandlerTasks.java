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

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
import java.util.function.Consumer;

public class V2HttpHandlerTasks extends V2HttpHandler {

  public V2HttpHandlerTasks(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.endsWith("/task")) {
        this.handleTaskListRequest(context);
      } else if (path.endsWith("/exists")) {
        this.handleTaskExistsRequest(context);
      } else {
        this.handleTaskRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      this.handleTaskCreateRequest(context);
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      this.handleTaskDeleteRequest(context);
    }
  }

  protected void handleTaskListRequest(IHttpContext context) {
    this.ok(context)
      .body(this.success().append("tasks", this.getTaskProvider().getPermanentServiceTasks()).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleTaskExistsRequest(IHttpContext context) {
    this.handleWithTaskContext(context, task -> this.ok(context)
      .body(this.success().append("result", this.getTaskProvider().isServiceTaskPresent(task)).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleTaskRequest(IHttpContext context) {
    this.handleWithTaskContext(context, task -> {
      ServiceTask serviceTask = this.getTaskProvider().getServiceTask(task);
      if (serviceTask == null) {
        this.ok(context)
          .body(this.failure().append("reason", "Unknown service task").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("task", serviceTask).toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleTaskCreateRequest(IHttpContext context) {
    ServiceTask serviceTask = this.body(context.request()).toInstanceOf(ServiceTask.class);
    if (serviceTask == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing service task").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    if (this.getTaskProvider().addPermanentServiceTask(serviceTask)) {
      this.response(context, HttpResponseCode.HTTP_CREATED).body(this.success().toByteArray()).context()
        .closeAfter(true).cancelNext();
    } else {
      this.ok(context).body(this.failure().toByteArray()).context().closeAfter(true).cancelNext();
    }
  }

  protected void handleTaskDeleteRequest(IHttpContext context) {
    this.handleWithTaskContext(context, task -> {
      if (this.getTaskProvider().isServiceTaskPresent(task)) {
        this.getTaskProvider().removePermanentServiceTask(task);
        this.ok(context)
          .body(this.success().toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.response(context, HttpResponseCode.HTTP_GONE)
          .body(this.failure().append("reason", "No such service task").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleWithTaskContext(IHttpContext context, Consumer<String> handler) {
    String taskName = context.request().pathParameters().get("task");
    if (taskName == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing task paramter").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      handler.accept(taskName);
    }
  }

  protected ServiceTaskProvider getTaskProvider() {
    return this.getCloudNet().getServiceTaskProvider();
  }
}
