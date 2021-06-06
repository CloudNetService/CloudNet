package de.dytanic.cloudnet.template.install.run.step.executor;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.template.install.run.InstallInformation;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class DownloadStepExecutor implements InstallStepExecutor {

  @Override
  public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException {
    Path targetPath = workingDirectory
      .resolve(Paths.get(installInformation.getServiceVersionType().getTargetEnvironment().getName() + ".jar"));

    try (InputStream inputStream = ProgressBarInputStream
      .wrapDownload(CloudNet.getInstance().getConsole(), new URL(installInformation.getServiceVersion().getUrl()))) {
      Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

      return new HashSet<>(Collections.singleton(targetPath));
    }
  }

}
