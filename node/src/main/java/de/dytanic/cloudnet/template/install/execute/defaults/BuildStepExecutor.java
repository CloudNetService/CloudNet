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

package de.dytanic.cloudnet.template.install.execute.defaults;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.execute.InstallStepExecutor;
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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class BuildStepExecutor implements InstallStepExecutor {

  private static final Logger LOGGER = LogManager.getLogger(BuildStepExecutor.class);
  private static final ExecutorService OUTPUT_READER_EXECUTOR = Executors.newCachedThreadPool();
  private static final Type STRING_LIST_TYPE = TypeToken.getParameterized(List.class, String.class).getType();

  private final Collection<Process> runningBuildProcesses = new CopyOnWriteArrayList<>();

  @Override
  public @NotNull Set<Path> execute(
    @NotNull InstallInformation information,
    @NotNull Path workDir,
    @NotNull Set<Path> paths
  ) throws IOException {
    var version = information.getServiceVersion();

    Collection<String> jvmOptions = version.getProperties().get("jvmOptions", STRING_LIST_TYPE);
    List<String> parameters = version.getProperties().get("parameters", STRING_LIST_TYPE, new ArrayList<>());

    for (var path : paths) {
      List<String> arguments = new ArrayList<>();

      arguments.add(information.getInstallerExecutable().orElse(CloudNet.getInstance().getConfig().getJVMCommand()));
      if (jvmOptions != null) {
        arguments.addAll(jvmOptions);
      }

      arguments.add("-jar");
      arguments.add(path.getFileName().toString());
      arguments.addAll(
        parameters.stream()
          .map(parameter -> parameter.replace("%version%", version.getName()))
          .collect(Collectors.toList()));

      var expectedExitCode = version.getProperties().getInt("exitCode", 0);
      var exitCode = this.buildProcessAndWait(arguments, workDir);

      if (!version.getProperties().getBoolean("disableExitCodeChecking") && exitCode != expectedExitCode) {
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
    for (var runningBuildProcess : this.runningBuildProcesses) {
      runningBuildProcess.destroyForcibly();
    }
  }

  protected int buildProcessAndWait(@NotNull List<String> arguments, @NotNull Path workingDir) {
    return this.buildProcessAndWait(
      arguments,
      workingDir,
      (line, $) -> LOGGER.info(String.format("[Template Installer]: %s", line)),
      (line, $) -> LOGGER.warning(String.format("[Template Installer]: %s", line)));
  }

  protected int buildProcessAndWait(
    @NotNull List<String> arguments,
    @NotNull Path workingDir,
    @NotNull BiConsumer<String, Process> systemOutRedirector,
    @NotNull BiConsumer<String, Process> systemErrRedirector
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
      LOGGER.severe("Exception while awaiting build process", exception);
    }
    return -1;
  }

  private static class BuildOutputRedirector implements Runnable {

    private final Process process;
    private final InputStream source;
    private final BiConsumer<String, Process> handler;

    public BuildOutputRedirector(Process process, InputStream source, BiConsumer<String, Process> handler) {
      this.process = process;
      this.source = source;
      this.handler = handler;
    }

    @Override
    public void run() {
      try (var in = new BufferedReader(new InputStreamReader(this.source, StandardCharsets.UTF_8))) {
        String line;
        while ((line = in.readLine()) != null) {
          this.handler.accept(line, this.process);
        }
      } catch (IOException exception) {
        LOGGER.severe("Exception while reading output", exception);
      }
    }
  }
}
