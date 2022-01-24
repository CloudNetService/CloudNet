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

package eu.cloudnetservice.launcher.java17.dependency;

import eu.cloudnetservice.ext.updater.util.ChecksumUtil;
import eu.cloudnetservice.launcher.java17.util.HttpUtil;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public record Repository(@NonNull String name, @NonNull URI url) {

  public void loadDependency(@NonNull Path targetPath, @NonNull Dependency dependency) throws Exception {
    // for example: io.netty netty-all 4.1.70.Final netty-all-4.1.70.Final-linux_x64_86
    // CHECKSTYLE.OFF: Launcher has no proper logger
    System.out.printf("Downloading dependency %s to %s... %n", dependency, targetPath);
    // CHECKSTYLE.ON
    for (int i = 0; i < 3; i++) {
      // try to download the dependency
      if (this.downloadDependency(targetPath, dependency)) {
        // successful, do not throw an exception
        return;
      }
    }
    // unsuccessful, abort
    throw new IllegalStateException("Tried and failed 3 times to download dependency " + dependency + ", aborting!");
  }

  private boolean downloadDependency(@NonNull Path target, @NonNull Dependency dependency) throws Exception {
    var actualPath = HttpUtil.get(
      URI.create(String.format(
        "%s/%s/%s/%s/%s-%s%s.jar",
        this.url,
        dependency.normalizedGroup(),
        dependency.name(),
        dependency.originalVersion(),
        dependency.name(),
        dependency.fullVersion(),
        dependency.classifier())),
      HttpUtil.handlerForFile(target)
    ).body();
    // validate the checksum of the file
    var checksum = ChecksumUtil.fileShaSum(actualPath);
    if (!checksum.equals(dependency.checksum())) {
      // remove the file
      Files.deleteIfExists(actualPath);
      return false;
    } else {
      // successful download
      return true;
    }
  }
}
