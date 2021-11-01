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

package de.dytanic.cloudnet.ext.cloudperms.node.config;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public final class CloudPermissionConfigHelper {

  public static CloudPermissionConfig read(@NotNull Path location) {
    JsonDocument document = JsonDocument.newDocument(location);
    if (document.isEmpty()) {
      write(CloudPermissionConfig.DEFAULT, location);
      return CloudPermissionConfig.DEFAULT;
    }

    return document.toInstanceOf(CloudPermissionConfig.class);
  }

  public static void write(@NotNull CloudPermissionConfig config, @NotNull Path location) {
    JsonDocument.newDocument(config).write(location);
  }

}
