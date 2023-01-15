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

package eu.cloudnetservice.launcher.java17.updater.updaters;

import eu.cloudnetservice.ext.updater.Updater;
import eu.cloudnetservice.ext.updater.util.GitHubUtil;
import eu.cloudnetservice.launcher.java17.updater.LauncherUpdaterContext;
import eu.cloudnetservice.launcher.java17.updater.util.FileDownloadUpdateHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;

public final class LauncherUpdater implements Updater<LauncherUpdaterContext> {

  @Override
  public void executeUpdates(@NonNull LauncherUpdaterContext context, boolean onlyIfRequired) throws Exception {
    // get the new checksum of the file
    var checksum = context.checksums().getProperty("launcher");
    var launcherPath = context.launcher().workingDirectory().resolve("launcher-update.jar");
    var downloadUri = GitHubUtil.buildUri(context.repo(), context.branch(), "launcher.jar");
    // download the updated launcher
    if (FileDownloadUpdateHelper.updateFile(downloadUri, launcherPath, checksum, "launcher", onlyIfRequired)) {
      // install the updater
      var launcherPatcherPath = context.launcher().workingDirectory().resolve("launcher-patcher.jar");
      var currentJar = Path.of(LauncherUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          // run the patcher we've downloaded by the other updater (hopefully). Command line:
          // <current java> -jar <pid> <old launcher> <new launcher>
          var command = List.of(
            Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString(),
            "-jar",
            launcherPatcherPath.toAbsolutePath().toString(),
            Long.toString(ProcessHandle.current().pid()),
            currentJar.toAbsolutePath().toString(),
            launcherPath.toAbsolutePath().toString());
          var updaterLogFile = context.launcher().workingDirectory().resolve("launcher_updater.log").toFile();
          // start the process
          new ProcessBuilder(command)
            .redirectError(updaterLogFile)
            .redirectOutput(updaterLogFile)
            .start();
        } catch (IOException exception) {
          throw new IllegalStateException("Unable to start updater:", exception);
        }
      }));
    }
  }
}
