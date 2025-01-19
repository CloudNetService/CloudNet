/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.report.config;

import eu.cloudnetservice.driver.base.Named;
import java.util.Map;
import lombok.NonNull;

public record PasteServer(
  @NonNull String name,
  @NonNull String baseUrl,
  @NonNull String apiDataEndpoint,
  @NonNull String requestMethod,
  @NonNull Map<String, String> headers,
  @NonNull String responsePasteIdKey
) implements Named {

  public static final PasteServer DEFAULT_PASTER_SERVER = new PasteServer(
    "cloudnet",
    "https://just-paste.it/",
    "documents",
    "POST",
    Map.of(),
    "key");

  @Override
  public @NonNull String baseUrl() {
    return this.baseUrl.endsWith("/") ? this.baseUrl : String.format("%s/", this.baseUrl);
  }
}
