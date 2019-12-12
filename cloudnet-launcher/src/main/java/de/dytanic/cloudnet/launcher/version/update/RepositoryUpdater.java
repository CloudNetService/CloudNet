package de.dytanic.cloudnet.launcher.version.update;

import de.dytanic.cloudnet.launcher.version.util.GitCommit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class RepositoryUpdater implements Updater {

    private String url;

    private String repositoryVersion;

    private String appVersion;

    private String gitHubRepository;

    private GitCommit latestGitCommit;

    private Path targetDirectory;

    @Override
    public boolean init(Path versionDirectory, String url, String githubRepository) {
        this.url = url = url.endsWith("/") ? url : url + "/";
        this.gitHubRepository = githubRepository;

        try {

            try (InputStream inputStream = this.readFromURL(url + "repository")) {
                Properties properties = new Properties();
                properties.load(inputStream);

                this.repositoryVersion = properties.getProperty("repository-version");
                this.appVersion = properties.getProperty("app-version");

                this.latestGitCommit = this.requestLatestGitCommit(properties.getProperty("git-commit"));

                this.targetDirectory = versionDirectory.resolve(this.appVersion + "-" + this.latestGitCommit.getShortenedSha());
            }

            return true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean installModuleFile(String name, Path path) {
        System.out.println("Installing remote version module " + name + " from version " + this.appVersion);

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

            try (InputStream inputStream = this.readFromURL(this.url + "versions/" + this.appVersion + "/" + name)) {
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
    public GitCommit getLatestGitCommit() {
        return this.latestGitCommit;
    }

    @Override
    public Path getTargetDirectory() {
        return this.targetDirectory;
    }

}