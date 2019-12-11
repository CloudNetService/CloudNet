package de.dytanic.cloudnet.launcher.update;

import de.dytanic.cloudnet.launcher.Constants;
import de.dytanic.cloudnet.launcher.module.CloudNetModule;
import de.dytanic.cloudnet.launcher.version.VersionInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Updater extends VersionInfo {

    boolean init(String url, String githubRepository);

    boolean installModuleFile(String name, Path path);

    boolean installFile(String name, Path path, boolean replace);

    default boolean installUpdate(String destinationBaseDirectory, String moduleDestinationBaseDirectory) {
        boolean successful = false;

        if (this.getCurrentVersion() != null) {
            successful = true;

            String versionDirName = this.getCurrentVersion() + "-" + this.getLatestGitCommit().getShortenedSha();
            for (String versionFile : Constants.VERSION_FILE_NAMES) {
                if (!this.installFile(versionFile, Paths.get(destinationBaseDirectory + "/" + versionDirName, versionFile), false)) {
                    successful = false;
                }
            }

            if (moduleDestinationBaseDirectory != null) {
                for (CloudNetModule module : Constants.DEFAULT_MODULES) {
                    if (!this.installModuleFile(module.getFileName(), Paths.get(moduleDestinationBaseDirectory, module.getFileName()))) {
                        successful = false;
                    }
                }
            }

        }

        return successful;
    }

}