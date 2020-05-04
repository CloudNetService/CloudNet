package de.dytanic.cloudnet.launcher.version.update;

import de.dytanic.cloudnet.launcher.LauncherUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class RepositoryUpdater implements Updater {

    private final String url;

    private String repositoryVersion;

    private String appVersion;

    private String gitHubRepository;

    private long releaseTimestamp;

    private Path targetDirectory;

    public RepositoryUpdater(String url) {
        this.url = url.endsWith("/") ? url : url + "/";
    }

    @Override
    public boolean init(Path versionDirectory, String githubRepository) {
        this.gitHubRepository = githubRepository;

        try (InputStream inputStream = LauncherUtils.readFromURL(this.url + "repository")) {
            Properties properties = new Properties();
            properties.load(inputStream);

            if (properties.containsKey("app-version")) {
                this.repositoryVersion = properties.getProperty("repository-version");
                this.appVersion = properties.getProperty("app-version");
                this.releaseTimestamp = Long.parseLong(properties.getProperty("release-time", "-1"));

                this.targetDirectory = versionDirectory.resolve(this.getFullVersion());

                return true;
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean installFile(String name, Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            Files.createDirectories(path.getParent());

            try (InputStream inputStream = LauncherUtils.readFromURL(this.url + "versions/" + this.appVersion + "/" + name)) {
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public String getRepositoryVersion() {
        return this.repositoryVersion;
    }

    @Override
    public String getCurrentVersion() {
        return this.appVersion;
    }

    @Override
    public String getGitHubRepository() {
        return this.gitHubRepository;
    }

    @Override
    public long getReleaseTimestamp() {
        return this.releaseTimestamp;
    }

    @Override
    public Path getTargetDirectory() {
        return this.targetDirectory;
    }

}