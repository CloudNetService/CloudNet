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
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public final class CloudNetLauncherPatcher {

  public static void main(@NonNull String[] args) {
    try {
      // the jvm has a signal handler for SIGHUP, which exists the jvm when received
      // tmux and screen use SIGHUP to signal the process that the session closed; however,
      // this shouldn't prevent the patching process from running anyway
      Signal.handle(new Signal("HUP"), SignalHandler.SIG_IGN);
    } catch (Throwable throwable) {
      printf(System.err, "Unable to register signal handler: %s", throwable.getMessage());
    }

    // validate that we got all required args to run (<pid> <launcher path> <new launcher path>)
    if (args.length == 3) {
      var launcherPid = Long.parseLong(args[0]);
      var launcherPath = Path.of(args[1]);
      var newLauncherPath = Path.of(args[2]);
      printf(System.out, "Picked up options:");
      printf(System.out, " - Launcher PID: %d", launcherPid);
      printf(System.out, " - Launcher Path: %s", launcherPath);
      printf(System.out, " - New Launcher Path: %s", newLauncherPath);

      // wait for the process to terminate by joining it (to block the current thread)
      ProcessHandle.of(launcherPid).ifPresent(handle -> handle.onExit()
        .orTimeout(5, TimeUnit.SECONDS)
        .exceptionally(__ -> {
          printf(System.err, "Launcher process did not terminate in time, running patcher anyway");
          return null;
        })
        .join());

      printf(System.out, "Copying new launcher file...");
      replaceOldLauncher(launcherPath, newLauncherPath);
    }
  }

  private static void replaceOldLauncher(@NonNull Path oldPath, @NonNull Path newPath) {
    try {
      Files.copy(newPath, oldPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static void printf(@NonNull PrintStream target, @NonNull String format, @NonNull Object... args) {
    target.printf(format + "%n", args);
  }
}
