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

package eu.cloudnetservice.launcher.dependency;

import eu.cloudnetservice.launcher.utils.HttpUtils;
import java.net.URI;
import java.nio.file.Path;
import lombok.NonNull;

public record Repository(@NonNull String name, @NonNull URI url) {

  public void loadDependency(@NonNull Path targetPath, @NonNull Dependency dependency) throws Exception {
    // for example: io.netty netty-all 4.1.70.Final netty-all-4.1.70.Final-linux_x64_86
    System.out.printf("Downloading dependency %s to %s... %n", dependency, targetPath);
    HttpUtils.get(
      URI.create(String.format(
        "%s/%s/%s/%s/%s-%s.jar",
        this.url,
        dependency.normalizedGroup(),
        dependency.name(),
        dependency.originalVersion(),
        dependency.name(),
        dependency.fullVersion())),
      HttpUtils.handlerForFile(targetPath)
    ).body();
  }
}
