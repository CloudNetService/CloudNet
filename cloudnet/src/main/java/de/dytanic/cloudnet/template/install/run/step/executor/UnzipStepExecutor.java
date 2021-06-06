package de.dytanic.cloudnet.template.install.run.step.executor;

import de.dytanic.cloudnet.template.install.run.InstallInformation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;

public class UnzipStepExecutor implements InstallStepExecutor {

  @Override
  public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException {
    Set<Path> resultPaths = new HashSet<>();

    for (Path path : inputPaths) {
      try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
          Path targetPath = workingDirectory.resolve(entry.getName());

          if (!targetPath.normalize().startsWith(workingDirectory)) {
            throw new IllegalStateException("Zip entry path contains traversal element!");
          }

          resultPaths.add(targetPath);

          if (entry.isDirectory()) {
            Files.createDirectory(targetPath);
          } else {
            Files.copy(zipInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }

    return resultPaths;
  }

}
