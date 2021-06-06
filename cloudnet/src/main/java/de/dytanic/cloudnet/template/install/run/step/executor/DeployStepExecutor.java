package de.dytanic.cloudnet.template.install.run.step.executor;

import de.dytanic.cloudnet.template.install.run.InstallInformation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class DeployStepExecutor implements InstallStepExecutor {

  @Override
  public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException {
    for (Path path : inputPaths) {
      if (Files.isDirectory(path)) {
        continue;
      }

      String relativePath = workingDirectory.relativize(path).toString().replace("\\", "/");

      try (OutputStream outputStream = installInformation.getTemplateStorage()
        .newOutputStream(installInformation.getServiceTemplate(), relativePath)) {
        Files.copy(path, Objects.requireNonNull(outputStream));
      }
    }
    return inputPaths;
  }

}
