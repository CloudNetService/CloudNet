package de.dytanic.cloudnet.launcher.version.update;

import de.dytanic.cloudnet.launcher.LauncherUtils;
import de.dytanic.cloudnet.launcher.module.CloudNetModule;
import de.dytanic.cloudnet.launcher.version.VersionInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface Updater extends VersionInfo {

  boolean init(Path versionDirectory, String githubRepository);

  boolean installFile(String name, Path path);

  default boolean installUpdate(String moduleDestinationBaseDirectory) {
    boolean successful = false;

    if (this.getCurrentVersion() != null) {

      try {
        Files.createDirectories(this.getTargetDirectory());

        successful = true;

        for (String versionFile : LauncherUtils.VERSION_FILE_NAMES) {
          if (!this.installFile(versionFile, this.getTargetDirectory().resolve(versionFile))) {
            successful = false;
          }
        }

        if (moduleDestinationBaseDirectory != null) {
          Path moduleDirectoryPath = Paths.get(moduleDestinationBaseDirectory);

          boolean modulesExist = Files.exists(moduleDirectoryPath);

          if (!modulesExist) {
            Files.createDirectories(moduleDirectoryPath);
          }

          for (CloudNetModule module : LauncherUtils.DEFAULT_MODULES) {
            Path modulePath = moduleDirectoryPath.resolve(module.getFileName());

            // avoiding the installation of manual removed modules
            if (!modulesExist || Files.exists(modulePath)) {
              System.out.printf("Installing module %s...%n", module.getName());

              if (!this.installFile(module.getFileName(), modulePath)) {
                successful = false;
              }
            }
          }
        }
      } catch (IOException exception) {
        exception.printStackTrace();
        return false;
      }

    }

    return successful;
  }

}
