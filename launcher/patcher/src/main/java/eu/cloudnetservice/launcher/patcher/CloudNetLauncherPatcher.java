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

package eu.cloudnetservice.launcher.patcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.NonNull;

public final class CloudNetLauncherPatcher {

  public static void main(@NonNull String[] args) {
    // validate that we got all required args to run (<pid> <old launcher> <new launcher>)
    if (args.length == 3) {
      var launcherPid = Long.parseLong(args[0]);
      var oldLauncherPath = Path.of(args[1]);
      var newLauncherPath = Path.of(args[2]);
      // just for debug reasons
      // CHECKSTYLE.OFF: Launcher has no proper logger
      System.out.printf("Picked up options: %s -> %s (pid: %d)%n", oldLauncherPath, newLauncherPath, launcherPid);
      // CHECKSTYLE.ON
      // wait for the process to terminate by joining it (to block the current thread)
      ProcessHandle.of(launcherPid).ifPresent(handle -> {
        // CHECKSTYLE.OFF: Launcher has no proper logger
        System.out.println("Found process handle, joining now");
        System.out.println("Info: " + handle.info().command().get());
        System.out.println("Alive:" + handle.isAlive());
        handle.onExit().join();
        System.out.println("Post join");
        // CHECKSTYLE.ON
      });
      // the process doesn't exist or terminated - run the updater now
      // CHECKSTYLE.OFF: Launcher has no proper logger
      System.out.printf("Running patcher on file %s%n", oldLauncherPath);
      // CHECKSTYLE.ON
      replaceOldLauncher(oldLauncherPath, newLauncherPath);
    }
  }

  private static void replaceOldLauncher(@NonNull Path oldPath, @NonNull Path newPath) {
    try {
      // move the new file to the location of the old file
      Files.copy(newPath, oldPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
