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

package eu.cloudnetservice.launcher.java17.dependency;

import eu.cloudnetservice.ext.updater.util.ChecksumUtil;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;

public final class DependencyHelper {

  public static final Path LIB_PATH = Path.of("launcher", "libs");

  private DependencyHelper() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Set<Path> loadFromLibrariesFile(@NonNull Path jarPath) throws IOException {
    // load the cloudnet.cnl file from the jar
    Collection<Dependency> dependencies = new HashSet<>();
    Map<String, Repository> repositories = new HashMap<>();
    try (var fileSystem = FileSystems.newFileSystem(jarPath)) {
      // get the library version file and validate that it is actually there
      var libPath = fileSystem.getPath("cloudnet.cnl");
      if (Files.notExists(libPath)) {
        throw new IllegalArgumentException("Unable to find cloudnet.cnl in " + jarPath);
      }
      // open the file and resolve all repos and dependencies from it
      var lines = Files.readAllLines(libPath, StandardCharsets.UTF_8);
      for (var line : lines) {
        // check if we should read the line
        if (!line.isEmpty() && !line.startsWith("#")) {
          var parts = line.split(" ");
          if (parts.length > 0) {
            // check if it is a repository or dependency
            if (parts[0].equalsIgnoreCase("repo")) {
              // check that it can actually be a valid repo definition
              Objects.checkIndex(2, parts.length);
              repositories.put(parts[1], new Repository(parts[1], URI.create(parts[2])));
            } else if (parts[0].equalsIgnoreCase("include")) {
              // check that it can actually be a valid dependency definition
              Objects.checkIndex(6, parts.length);
              dependencies.add(new Dependency(
                parts[1],
                parts[2],
                parts[3],
                parts[4],
                parts[5],
                parts[6],
                parts.length == 8 ? parts[7] : null));
            } else {
              // well somebody tried to troll us, huh?
              throw new IllegalArgumentException("Unsure how to handle line \"" + line + "\" in cloudnet.cnl");
            }
          }
        }
      }
    }
    // load all dependencies
    return load(repositories, dependencies);
  }

  public static @NonNull Set<Path> load(
    @NonNull Map<String, Repository> repositories,
    @NonNull Collection<Dependency> dependencies
  ) throws IOException {
    Set<Path> loadedDependencyPaths = new HashSet<>();
    for (var dependency : dependencies) {
      // get the associated repository
      var repo = repositories.get(dependency.repo());
      Objects.requireNonNull(repo, "Dependency " + dependency + " is in unknown repository " + dependency.repo());
      // get the target file path
      var targetFile = LIB_PATH
        .resolve(dependency.normalizedGroup())
        .resolve(dependency.name())
        .resolve(dependency.originalVersion())
        .resolve(String.format("%s-%s%s.jar", dependency.name(), dependency.fullVersion(), dependency.classifier()));
      // if the file already exists, ensure that the checksum still matches
      if (Files.exists(targetFile)) {
        var checksum = ChecksumUtil.fileShaSum(targetFile);
        if (!checksum.equals(dependency.checksum())) {
          // remove the file here, this will trigger a re-download
          Files.deleteIfExists(targetFile);
        }
      }
      // we don't need to load the dependency if we already loaded it
      if (Files.notExists(targetFile)) {
        try {
          // load the dependency
          repo.loadDependency(targetFile, dependency);
        } catch (Exception exception) {
          throw new IllegalStateException("Unable to load dependency " + dependency + " from " + repo, exception);
        }
      }
      // the dependency is available for loading now
      loadedDependencyPaths.add(targetFile);
    }
    return loadedDependencyPaths;
  }
}
