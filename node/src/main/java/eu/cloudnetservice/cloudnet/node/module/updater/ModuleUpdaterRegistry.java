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

package eu.cloudnetservice.cloudnet.node.module.updater;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.module.DefaultModuleProvider;
import eu.cloudnetservice.cloudnet.node.module.ModulesHolder;
import eu.cloudnetservice.ext.updater.defaults.DefaultUpdaterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

public final class ModuleUpdaterRegistry extends DefaultUpdaterRegistry<ModuleUpdaterContext, ModulesHolder> {

  @Override
  protected @NonNull ModuleUpdaterContext provideContext(@NonNull ModulesHolder provisionContext) throws Exception {
    // read the module names from all existing modules
    Map<Path, String> moduleNames = new HashMap<>();
    FileUtil.walkFileTree(DefaultModuleProvider.DEFAULT_MODULE_DIR, ($, file) -> {
      // open the jar file
      FileUtil.openZipFile(file, fs -> {
        var moduleJson = fs.getPath("module.json");
        if (Files.exists(moduleJson)) {
          // read the file
          var document = JsonDocument.newDocument(moduleJson);
          if (document.contains("name")) {
            moduleNames.put(file.toAbsolutePath(), document.getString("name"));
          }
        }
      });
    }, false, "*.{jar,war,zip}");
    // create a module updating context from the information
    return new ModuleUpdaterContext(
      provisionContext,
      moduleNames,
      System.getProperty("cloudnet.updateRepo", "CloudNetService/launchermeta"),
      System.getProperty("cloudnet.updateBranch", "release"));
  }
}
