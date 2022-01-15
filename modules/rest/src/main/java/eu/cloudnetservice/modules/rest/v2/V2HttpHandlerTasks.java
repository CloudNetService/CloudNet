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
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import java.util.function.Consumer;

public class V2HttpHandlerTasks extends V2HttpHandler {

  public V2HttpHandlerTasks(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context, @NonNull HttpSession ses) {
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

  protected void handleTaskListRequest(HttpContext context) {
    this.ok(context)
      .body(this.success().append("tasks", this.taskProvider().permanentServiceTasks()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleTaskExistsRequest(HttpContext context) {
    this.handleWithTaskContext(context, task -> this.ok(context)
      .body(this.success().append("result", this.taskProvider().serviceTaskPresent(task)).toString())
      .context()
      .closeAfter(true)
      .cancelNext()
    );
  }

  protected void handleTaskRequest(HttpContext context) {
    this.handleWithTaskContext(context, task -> {
      var serviceTask = this.taskProvider().serviceTask(task);
      if (serviceTask == null) {
        this.ok(context)
          .body(this.failure().append("reason", "Unknown service task").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("task", serviceTask).toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleTaskCreateRequest(HttpContext context) {
    var serviceTask = this.body(context.request()).toInstanceOf(ServiceTask.class);
    if (serviceTask == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing service task").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    if (this.taskProvider().addPermanentServiceTask(serviceTask)) {
      this.response(context, HttpResponseCode.CREATED).body(this.success().toString()).context()
        .closeAfter(true).cancelNext();
    } else {
      this.ok(context).body(this.failure().toString()).context().closeAfter(true).cancelNext();
    }
  }

  protected void handleTaskDeleteRequest(HttpContext context) {
    this.handleWithTaskContext(context, task -> {
      var serviceTask = this.taskProvider().serviceTask(task);
      if (serviceTask != null) {
        this.taskProvider().removePermanentServiceTask(serviceTask);
        this.ok(context)
          .body(this.success().toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.response(context, HttpResponseCode.GONE)
          .body(this.failure().append("reason", "No such service task").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleWithTaskContext(HttpContext context, Consumer<String> handler) {
    var taskName = context.request().pathParameters().get("task");
    if (taskName == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing task paramter").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      handler.accept(taskName);
    }
  }

  protected ServiceTaskProvider taskProvider() {
    return this.node().serviceTaskProvider();
  }
}
