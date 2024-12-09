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

package eu.cloudnetservice.node.impl.version.execute.defaults;

import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.version.execute.InstallStepExecutor;
import eu.cloudnetservice.node.impl.version.information.VersionInstaller;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BuildStepExecutor implements InstallStepExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(BuildStepExecutor.class);
  private static final ExecutorService OUTPUT_READER_EXECUTOR = Executors.newCachedThreadPool();
  private static final Type STRING_LIST_TYPE = TypeFactory.parameterizedClass(List.class, String.class);

  private final Configuration configuration;
  private final Collection<Process> runningBuildProcesses = new ConcurrentLinkedQueue<>();

  @Inject
  public BuildStepExecutor(@NonNull Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public @NonNull Set<Path> execute(
    @NonNull VersionInstaller installer,
    @NonNull Path workDir,
    @NonNull Set<Path> paths
  ) throws IOException {
    var version = installer.serviceVersion();

    Collection<String> jvmOptions = version.properties().readObject("jvmOptions", STRING_LIST_TYPE);
    List<String> parameters = version.properties().readObject("parameters", STRING_LIST_TYPE, new ArrayList<>());

    for (var path : paths) {
      List<String> arguments = new ArrayList<>();

      arguments.add(Objects.requireNonNullElse(
        installer.installerExecutable(),
        this.configuration.javaCommand()));
      if (jvmOptions != null) {
        arguments.addAll(jvmOptions);
      }

      arguments.add("-jar");
      arguments.add(path.getFileName().toString());
      arguments.addAll(parameters.stream().map(parameter -> parameter.replace("%version%", version.name())).toList());

      var expectedExitCode = version.properties().getInt("exitCode", 0);
      var exitCode = this.buildProcessAndWait(arguments, workDir);

      if (!version.properties().getBoolean("disableExitCodeChecking") && exitCode != expectedExitCode) {
        throw new IllegalStateException(String.format(
          "Process returned unexpected exit code! Got %d, expected %d",
          exitCode,
          expectedExitCode));
      }
    }

    return Files.walk(workDir).collect(Collectors.toSet());
  }

  @Override
  public void interrupt() {
    for (var runningBuildProcess : List.copyOf(this.runningBuildProcesses)) {
      runningBuildProcess.destroyForcibly();
    }
  }

  protected int buildProcessAndWait(@NonNull List<String> arguments, @NonNull Path workingDir) {
    return this.buildProcessAndWait(
      arguments,
      workingDir,
      (line, $) -> LOGGER.info("[Template Installer]: {}", line),
      (line, $) -> LOGGER.warn("[Template Installer]: {}", line));
  }

  protected int buildProcessAndWait(
    @NonNull List<String> arguments,
    @NonNull Path workingDir,
    @NonNull BiConsumer<String, Process> systemOutRedirector,
    @NonNull BiConsumer<String, Process> systemErrRedirector
  ) {
    try {
      var process = new ProcessBuilder()
        .command(arguments)
        .directory(workingDir.toFile())
        .start();

      this.runningBuildProcesses.add(process);

      OUTPUT_READER_EXECUTOR.execute(new BuildOutputRedirector(process, process.getInputStream(), systemOutRedirector));
      OUTPUT_READER_EXECUTOR.execute(new BuildOutputRedirector(process, process.getErrorStream(), systemErrRedirector));

      var exitCode = process.waitFor();
      this.runningBuildProcesses.remove(process);

      return exitCode;
    } catch (IOException | InterruptedException exception) {
      LOGGER.error("Exception while awaiting build process", exception);
    }
    return -1;
  }

  private record BuildOutputRedirector(
    @NonNull Process process,
    @NonNull InputStream source,
    @NonNull BiConsumer<String, Process> handler
  ) implements Runnable {

    @Override
    public void run() {
      try (var in = new BufferedReader(new InputStreamReader(this.source, StandardCharsets.UTF_8))) {
        String line;
        while ((line = in.readLine()) != null) {
          this.handler.accept(line, this.process);
        }
      } catch (IOException exception) {
        LOGGER.error("Exception while reading output", exception);
      }
    }
  }
}
