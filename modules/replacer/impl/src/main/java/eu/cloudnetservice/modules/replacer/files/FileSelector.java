/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.replacer.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

public final class FileSelector {

  public Stream<Path> findFiles(Path root, List<PathMatcher> matchers) throws IOException {
    return Files.walk(root)
      .filter(Files::isRegularFile)
      .filter(path -> this.matchesAny(path, matchers));
  }

  private boolean matchesAny(Path path, List<PathMatcher> matchers) {
    for (var matcher : matchers) {
      if (matcher.matches(path)) {
        return true;
      }
    }
    return false;
  }
}
