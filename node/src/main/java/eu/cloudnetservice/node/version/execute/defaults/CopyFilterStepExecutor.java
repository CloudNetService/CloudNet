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

package eu.cloudnetservice.node.version.execute.defaults;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.node.version.execute.InstallStepExecutor;
import eu.cloudnetservice.node.version.information.VersionInstaller;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.NonNull;

public class CopyFilterStepExecutor implements InstallStepExecutor {

  private static final Type STRING_MAP = TypeToken.getParameterized(Map.class, String.class, String.class).getType();

  @Override
  public @NonNull Set<Path> execute(
    @NonNull VersionInstaller installer,
    @NonNull Path workingDirectory,
    @NonNull Set<Path> inputPaths
  ) throws IOException {
    Map<String, String> copy = installer.serviceVersion().properties().get("copy", STRING_MAP);
    if (copy == null) {
      throw new IllegalStateException(String.format(
        "Missing copy property on service version %s!",
        installer.serviceVersion().name()));
    }

    var patterns = copy.entrySet().stream()
      .map(entry -> new Pair<>(Pattern.compile(entry.getKey()), entry.getValue()))
      .toList();

    Set<Path> resultPaths = new HashSet<>();
    for (var path : inputPaths) {
      if (Files.isDirectory(path)) {
        continue;
      }

      var relativePath = StringUtil.toLower(workingDirectory.relativize(path).toString().replace("\\", "/"));
      for (var patternEntry : patterns) {
        var pattern = patternEntry.first();
        var target = patternEntry.second();

        if (pattern.matcher(relativePath).matches()) {
          var targetPath = workingDirectory.resolve(target
            .replace("%path%", relativePath)
            .replace("%fileName%", path.getFileName().toString()));

          if (Files.isDirectory(targetPath)) {
            targetPath = path;
          }

          // copying might not even be necessary, maybe only filtering was wanted?
          if (!path.equals(targetPath)) {
            Files.createDirectories(targetPath.getParent());
            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }

          resultPaths.add(targetPath);
        }
      }
    }

    return resultPaths;
  }
}
