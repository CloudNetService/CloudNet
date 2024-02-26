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

package eu.cloudnetservice.launcher.java17.updater.updaters;

import eu.cloudnetservice.ext.updater.Updater;
import eu.cloudnetservice.launcher.java17.updater.LauncherUpdaterContext;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public final class LauncherChecksumsFileUpdater implements Updater<LauncherUpdaterContext> {

  @Override
  public void executeUpdates(@NonNull LauncherUpdaterContext context, boolean onlyIfRequired) throws Exception {
    try (var stream = Files.newOutputStream(Path.of("launcher", "checksums.properties"))) {
      context.checksums().store(
        stream,
        String.format("Latest loaded checksums (%s@%s)", context.repo(), context.branch()));
    }
  }
}
