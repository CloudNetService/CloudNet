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

package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;

public final class ExampleHttpHandler {

  public void registerHttpHandlerExample() {
    // register a default http handler, which receives all http message on the following path
    CloudNet.getInstance().getHttpServer()
      .registerHandler(
        "/helloworld",
        (path, context) -> {
          context
            .response()
            .statusCode(HttpResponseCode.HTTP_OK) //sets the response status code
            .header("Content-Type", "text/plain") //Sets the header
            .body("Hello, world!") //Sets the response http body
            .context() //switch to IHttpContext
            .closeAfter(true) //is not required. It closes automatic by default
            .cancelNext() //cancelled that no http handler will invoked after this on this path
          ;
        })
    ;

    // Register for the http get specific an http handler
    CloudNet.getInstance().getHttpServer().registerHandler(
      "/your_name/{name}", //Http
      new MethodHttpHandlerAdapter() {

        @Override
        public void handleGet(String path, IHttpContext context) {
          context
            .response()
            .statusCode(HttpResponseCode.HTTP_OK)
            .header("Content-Type", "text/plain")
            .body("Your name is " + context.request().pathParameters()
              .containsKey("name")) // get the following path parameter "name"
            .context()
            .closeAfter(true)
            .cancelNext()
          ;
        }
      }
    );

    //Removes all Http handler from this class loader
    CloudNet.getInstance().getHttpServer().removeHandler(ExampleHttpHandler.class.getClassLoader());
  }
}
