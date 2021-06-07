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

package de.dytanic.cloudnet.template.install.run.step.executor;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.run.InstallInformation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class BuildStepExecutor implements InstallStepExecutor {

  private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
  }.getType();
  private static final ExecutorService OUTPUT_READER_EXECUTOR = Executors.newCachedThreadPool();

  private final Collection<Process> runningBuildProcesses = new CopyOnWriteArrayList<>();

  @Override
  public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException {
    ServiceVersion version = installInformation.getServiceVersion();

    Collection<String> jvmOptions = version.getProperties().get("jvmOptions", STRING_LIST_TYPE);
    List<String> parameters = version.getProperties().get("parameters", STRING_LIST_TYPE, new ArrayList<>());

    for (Path path : inputPaths) {
      List<String> processArguments = new ArrayList<>();
      processArguments.add(CloudNet.getInstance().getConfig().getJVMCommand());
      if (jvmOptions != null) {
        processArguments.addAll(jvmOptions);
      }
      processArguments.add("-jar");
      processArguments.add(path.getFileName().toString());
      processArguments.addAll(
        parameters.stream()
          .map(parameter -> parameter.replace("%version%", version.getName()))
          .collect(Collectors.toList())
      );

      int expectedExitCode = version.getProperties().getInt("exitCode", 0);
      int exitCode = this.buildProcessAndWait(processArguments, workingDirectory);

      if (exitCode != expectedExitCode) {
        throw new IllegalStateException(
          String.format("Process returned unexpected exit code! Got %d, expected %d", exitCode, expectedExitCode));
      }
    }

    return Files.walk(workingDirectory).collect(Collectors.toSet());
  }

  @Override
  public void interrupt() {
    for (Process runningBuildProcess : this.runningBuildProcesses) {
      runningBuildProcess.destroyForcibly();
    }
  }

  private int buildProcessAndWait(List<String> arguments, Path workingDir) {
    try {
      Process process = new ProcessBuilder()
        .command(arguments)
        .directory(workingDir.toFile())
        .start();

      this.runningBuildProcesses.add(process);

      OUTPUT_READER_EXECUTOR.execute(new BuildOutputRunnable(process.getInputStream(), System.out));
      OUTPUT_READER_EXECUTOR.execute(new BuildOutputRunnable(process.getErrorStream(), System.err));

      int exitCode = process.waitFor();
      this.runningBuildProcesses.remove(process);

      return exitCode;
    } catch (IOException | InterruptedException exception) {
      exception.printStackTrace();
    }
    return -1;
  }

  private static class BuildOutputRunnable implements Runnable {

    private final InputStream inputStream;

    private final PrintStream outputStream;

    public BuildOutputRunnable(InputStream inputStream, PrintStream outputStream) {
      this.inputStream = inputStream;
      this.outputStream = outputStream;
    }

    @Override
    public void run() {
      try (BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(this.inputStream, StandardCharsets.UTF_8))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          this.outputStream.println(line);
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }

  }

}
