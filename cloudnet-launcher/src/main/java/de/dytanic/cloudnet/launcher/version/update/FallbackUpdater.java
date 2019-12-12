package de.dytanic.cloudnet.launcher.version.update;


import de.dytanic.cloudnet.launcher.version.InstalledVersionInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FallbackUpdater extends InstalledVersionInfo implements Updater {

    public FallbackUpdater(Path targetDirectory, String gitHubRepository) {
        super(targetDirectory, gitHubRepository);
    }

    @Override
    public boolean init(Path versionDirectory, String url, String githubRepository) {
        try {
            Files.createDirectories(super.getTargetDirectory());
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean installModuleFile(String name, Path path) {
        System.out.println("Installing version module " + name + " from fallback version " + super.appVersion);

        return this.installFile(name, path, true);
    }

    @Override
    public boolean installFile(String name, Path path, boolean replace) {
        if (!replace && Files.exists(path)) {
            return true;
        } else if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        try {
            Files.createDirectories(path.getParent());

            try (InputStream inputStream = this.getClass().getResourceAsStream(name)) {
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }


}
