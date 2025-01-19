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

package eu.cloudnetservice.node.impl.module.updater;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.impl.module.DefaultModuleProvider;
import eu.cloudnetservice.ext.updater.defaults.DefaultUpdaterRegistry;
import eu.cloudnetservice.node.impl.module.ModulesHolder;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

@Singleton
public final class ModuleUpdaterRegistry extends DefaultUpdaterRegistry<ModuleUpdaterContext, ModulesHolder> {

  @Override
  protected @NonNull ModuleUpdaterContext provideContext(@NonNull ModulesHolder provisionContext) {
    // read the module names from all existing modules
    Map<Path, String> moduleNames = new HashMap<>();
    FileUtil.walkFileTree(DefaultModuleProvider.DEFAULT_MODULE_DIR, ($, file) -> {
      // open the jar file
      FileUtil.openZipFile(file, fs -> {
        var moduleJson = fs.getPath("module.json");
        if (Files.exists(moduleJson)) {
          // read the file
          var document = DocumentFactory.json().parse(moduleJson);
          if (document.contains("name")) {
            moduleNames.put(file.toAbsolutePath(), document.getString("name"));
          }
        }
      });
    }, false, "*.{jar,war}");
    // create a module updating context from the information
    return new ModuleUpdaterContext(
      provisionContext,
      moduleNames,
      System.getProperty("cloudnet.updateRepo", "CloudNetService/launchermeta"),
      System.getProperty("cloudnet.updateBranch", "release"));
  }
}
