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

import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@ApplyHeaders
public final class V2HttpHandlerPreflight extends V2HttpHandler {

  @HttpRequestHandler(paths = "*", methods = "OPTIONS")
  private void handlePreflight(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().toString())
      .context()
      .closeAfter(true);
  }

}
