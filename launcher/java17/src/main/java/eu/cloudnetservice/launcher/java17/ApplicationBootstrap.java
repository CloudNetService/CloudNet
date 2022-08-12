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

package eu.cloudnetservice.launcher.java17;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import lombok.NonNull;

final class ApplicationBootstrap {

  private static final List<String> DEFAULT_PROCESS_ARGUMENTS = Arrays.asList(
    // We currently require access to some jvm internals to allow us to build some pretty nice stuff
    // which is easier accessible for us than needing each data object which (for example) wants to use rpc
    // to give a method handle with private class access into the generator. While this breaks up the jvm
    // encapsulation partly, it is way better than trying even more hacky stuff like guice or graal do to
    // gain access into the internals.
    "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED");
  private static final RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();

  private ApplicationBootstrap() {
    throw new UnsupportedOperationException();
  }

  public static void bootstrap(
    @NonNull Path applicationPath,
    @NonNull Set<Path> dependencies,
    @NonNull String[] processArguments,
    int debuggerPort
  ) throws Exception {
    // resolve the current jar, we only append the launcher jar to it in case someone needs access to it
    var currentJar = Path.of(ApplicationBootstrap.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toURI())
      .toAbsolutePath()
      .toString();

    // begin the process building
    Set<String> arguments = new LinkedHashSet<>();
    arguments.add(resolveJavaExecutable());

    // add the arguments supplied to the current process
    arguments.addAll(RUNTIME_MX_BEAN.getInputArguments());

    // add our default arguments & all system properties we might have set
    arguments.addAll(DEFAULT_PROCESS_ARGUMENTS);
    RUNTIME_MX_BEAN.getSystemProperties().forEach((key, value) -> arguments.add("-D" + key + "=" + value));

    // enabled the debugger if requested
    if (debuggerPort >= 0 && debuggerPort <= 0xFFFF) {
      arguments.add(String.format("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:%d", debuggerPort));
    }

    // class path
    arguments.add("-cp");
    arguments.add(buildClassPath(applicationPath, currentJar, dependencies));

    // add the main class we want to invoke
    arguments.add(resolveMainClass(applicationPath));

    // add the default arguments in a parsable way so that the node can pass it to the wrapper
    arguments.add(String.join(";;", DEFAULT_PROCESS_ARGUMENTS));

    // and the given process arguments
    arguments.addAll(Arrays.asList(processArguments));

    // start the process in a non-demon thread
    var thread = new Thread(() -> {
      try {
        // start the process
        var process = new ProcessBuilder(arguments.toArray(String[]::new)).inheritIO().start();

        // terminate the process if needed on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          var processHandle = process.toHandle();
          // destroy the process if it is still alive and wait for it to happen
          if (processHandle.isAlive() && processHandle.destroy()) {
            try {
              // we don't want to wait forever, but give it some time
              processHandle.onExit().get(30, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException exception) {
              // well looks like it didn't terminate - try to force it and don't wait
              processHandle.destroyForcibly();
            } catch (InterruptedException exception) {
              Thread.currentThread().interrupt(); // reset the interrupted state of the thread
            }
          }
        }));

        // wait for the process to exit normally (for example when using the stop command)
        process.waitFor();
      } catch (IOException exception) {
        // hm, something went wrong
        // CHECKSTYLE.OFF: Launcher has no proper logger
        System.err.println("Unable to start the node process with arguments: " + String.join(" ", arguments));
        System.err.println("The exception is:");
        exception.printStackTrace();
        // CHECKSTYLE.ON
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt(); // reset the interrupted state of the thread
      }
    }, "Application-Thread");
    thread.setDaemon(false);
    thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }

  private static @NonNull String buildClassPath(
    @NonNull Path appPath,
    @NonNull String currentJarPath,
    @NonNull Set<Path> dependencies
  ) {
    // build the full class path
    var paths = dependencies.stream()
      .map(path -> path.toAbsolutePath().toString())
      .collect(Collectors.collectingAndThen(
        Collectors.toSet(),
        set -> {
          set.add(currentJarPath);
          set.add(appPath.toAbsolutePath().toString());
          set.addAll(Arrays.asList(RUNTIME_MX_BEAN.getClassPath().split(File.pathSeparator)));

          return set;
        }));

    // join the entries correctly together
    return String.join(File.pathSeparator, paths);
  }

  private static @NonNull String resolveJavaExecutable() {
    // java.home points to the root java installation directory
    return Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
  }

  private static @NonNull String resolveMainClass(@NonNull Path applicationFile) throws IOException {
    try (var jarFile = new JarFile(applicationFile.toFile())) {
      return jarFile.getManifest().getMainAttributes().getValue("Main-Class");
    }
  }
}
