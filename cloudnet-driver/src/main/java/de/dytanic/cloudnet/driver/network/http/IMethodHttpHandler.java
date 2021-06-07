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

package de.dytanic.cloudnet.driver.network.http;

public interface IMethodHttpHandler extends IHttpHandler {

  @Override
  default void handle(String path, IHttpContext context) throws Exception {
    switch (context.request().method().toUpperCase()) {
      case "GET":
        this.handleGet(path, context);
        break;
      case "POST":
        this.handlePost(path, context);
        break;
      case "PATCH":
        this.handlePatch(path, context);
        break;
      case "PUT":
        this.handlePut(path, context);
        break;
      case "DELETE":
        this.handleDelete(path, context);
        break;
      case "HEAD":
        this.handleHead(path, context);
        break;
      case "TRACE":
        this.handleTrace(path, context);
        break;
      case "OPTIONS":
        this.handleOptions(path, context);
        break;
      case "CONNECT":
        this.handleConnect(path, context);
        break;
      default:
        break;
    }
  }

  void handlePost(String path, IHttpContext context) throws Exception;

  void handleGet(String path, IHttpContext context) throws Exception;

  void handlePut(String path, IHttpContext context) throws Exception;

  void handleHead(String path, IHttpContext context) throws Exception;

  void handleDelete(String path, IHttpContext context) throws Exception;

  void handlePatch(String path, IHttpContext context) throws Exception;

  void handleTrace(String path, IHttpContext context) throws Exception;

  void handleOptions(String path, IHttpContext context) throws Exception;

  void handleConnect(String path, IHttpContext context) throws Exception;

}
