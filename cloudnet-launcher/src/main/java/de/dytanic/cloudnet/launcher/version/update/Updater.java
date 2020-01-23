package de.dytanic.cloudnet.launcher.version.update;

import de.dytanic.cloudnet.launcher.Constants;
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

                for (String versionFile : Constants.VERSION_FILE_NAMES) {
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

                    for (CloudNetModule module : Constants.DEFAULT_MODULES) {
                        Path modulePath = moduleDirectoryPath.resolve(module.getFileName());

                        // avoiding the installation of manual removed modules
                        if (!modulesExist || Files.exists(modulePath)) {
                            System.out.println(String.format("Installing module %s...", module.getName()));

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

    default void deleteUpdateFiles() {
        try {
            if (Files.exists(this.getTargetDirectory())) {

                Files.list(this.getTargetDirectory())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }
                        });

                Files.delete(this.getTargetDirectory());

            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}