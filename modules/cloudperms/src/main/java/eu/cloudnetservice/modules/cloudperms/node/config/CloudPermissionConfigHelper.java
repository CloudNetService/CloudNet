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

package eu.cloudnetservice.modules.cloudperms.node.config;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import java.nio.file.Path;
import lombok.NonNull;

public final class CloudPermissionConfigHelper {

  /**
   * Reads a {@link CloudPermissionConfig} from the file at the given location. If the file is empty, the default {@link
   * CloudPermissionConfig#DEFAULT} configuration is written to the file.
   *
   * @param location the location of the file to read from
   * @return the read {@link CloudPermissionConfig} configuration
   */
  public static CloudPermissionConfig read(@NonNull Path location) {
    var document = JsonDocument.newDocument(location);
    if (document.empty()) {
      write(CloudPermissionConfig.DEFAULT, location);
      return CloudPermissionConfig.DEFAULT;
    }

    return document.toInstanceOf(CloudPermissionConfig.class);
  }

  /**
   * Wraps the given config into a {@link JsonDocument} and writes it to the given location.
   *
   * @param config   the config to write
   * @param location the location to save the config to
   */
  public static void write(@NonNull CloudPermissionConfig config, @NonNull Path location) {
    JsonDocument.newDocument(config).write(location);
  }
}
