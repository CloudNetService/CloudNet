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

package eu.cloudnetservice.cloudnet.node.module.util;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.node.module.ModulesHolder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.NonNull;

public final class ModuleJsonReader {

  private ModuleJsonReader() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull ModulesHolder read(@NonNull Path launcherDirPath) {
    var jsonFile = launcherDirPath.resolve("modules.json");
    // check if the file exists, if the file does not exist (probably when running in development mode) then we silently
    // ignore that and return an empty module holder
    if (Files.exists(jsonFile)) {
      return JsonDocument.newDocument(jsonFile).toInstanceOf(ModulesHolder.class);
    } else {
      return new ModulesHolder(Set.of());
    }
  }
}
