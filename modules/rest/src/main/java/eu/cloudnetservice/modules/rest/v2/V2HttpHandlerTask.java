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
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.tasks")
@ApplyHeaders
public final class V2HttpHandlerTask extends V2HttpHandler {

  private final ServiceTaskProvider taskProvider;

  @Inject
  public V2HttpHandlerTask(@NonNull ServiceTaskProvider taskProvider) {
    this.taskProvider = taskProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/task")
  private void handleTaskListRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("tasks", this.taskProvider.serviceTasks()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/task/{name}/exists")
  private void handleTaskExistsRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("name") String name) {
    var task = this.taskProvider.serviceTask(name);
    this.ok(context)
      .body(this.success().append("result", task != null).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/task/{name}")
  private void handleTaskRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("name") String name) {
    var serviceTask = this.taskProvider.serviceTask(name);
    if (serviceTask == null) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown service task").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.ok(context)
        .body(this.success().append("task", serviceTask).toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/task", methods = "POST")
  private void handleTaskCreateRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    var serviceTask = body.toInstanceOf(ServiceTask.class);
    if (serviceTask == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing service task").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    if (this.taskProvider.addServiceTask(serviceTask)) {
      this.response(context, HttpResponseCode.CREATED)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.ok(context).body(this.failure().toString()).context().closeAfter(true).cancelNext(true);
    }
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/task/{name}", methods = "DELETE")
  private void handleTaskDeleteRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("name") String name) {
    var serviceTask = this.taskProvider.serviceTask(name);
    if (serviceTask != null) {
      this.taskProvider.removeServiceTask(serviceTask);
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.response(context, HttpResponseCode.GONE)
        .body(this.failure().append("reason", "No such service task").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }
}
