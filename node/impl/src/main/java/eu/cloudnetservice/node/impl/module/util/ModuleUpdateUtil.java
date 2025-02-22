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

package eu.cloudnetservice.node.impl.module.util;

import dev.derklaro.aerogel.auto.Factory;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.node.impl.module.ModulesHolder;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class ModuleUpdateUtil {

  private ModuleUpdateUtil() {
    throw new UnsupportedOperationException();
  }

  @Factory
  private static @NonNull ModulesHolder readModuleJson(@NonNull @Named("launcherDir") Path launcherDirPath) {
    var jsonFile = launcherDirPath.resolve("modules.json");
    // check if the file exists, if the file does not exist (probably when running in development mode) then we silently
    // ignore that and return an empty module holder
    if (Files.exists(jsonFile)) {
      return DocumentFactory.json().parse(jsonFile).toInstanceOf(ModulesHolder.class);
    } else {
      return new ModulesHolder(Set.of());
    }
  }

  public static @Nullable Path findPathOfModule(@NonNull Path moduleDirectory, @NonNull String moduleName) {
    try (var stream = Files.newDirectoryStream(moduleDirectory, "*.{jar,war}")) {
      for (var candidate : stream) {
        // skip directories
        if (Files.isDirectory(candidate)) {
          continue;
        }

        // open the zip file and read the properties of it
        var expectedModule = FileUtil.mapZipFile(candidate, fs -> {
          var moduleJson = fs.getPath("module.json");
          if (Files.exists(moduleJson)) {
            // read the module json and check if the name matches the expected one
            var name = DocumentFactory.json().parse(moduleJson).getString("name");
            return name != null && name.equals(moduleName);
          }
          // not a module file
          return false;
        }, false);

        // check if the candidate file was the expected module
        if (expectedModule) {
          return candidate;
        }
      }
    } catch (IOException ignored) {
    }

    // unable to find a matching module (or an error occurred)
    return null;
  }
}
