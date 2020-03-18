package de.dytanic.cloudnet.launcher.version.update.jenkins;


import de.dytanic.cloudnet.launcher.Constants;
import de.dytanic.cloudnet.launcher.version.update.Updater;
import de.dytanic.cloudnet.launcher.version.util.GitCommit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarInputStream;

public class JenkinsUpdater implements Updater {

    private String jenkinsJobUrl;

    private JenkinsBuild jenkinsBuild;

    private String appVersion;

    private String gitHubRepository;

    private GitCommit latestGitCommit;

    private Path targetDirectory;

    public JenkinsUpdater(String jenkinsJobUrl) {
        this.jenkinsJobUrl = jenkinsJobUrl.endsWith("/") ? jenkinsJobUrl : jenkinsJobUrl + "/";
    }

    @Override
    public boolean init(Path versionDirectory, String githubRepository) {
        this.gitHubRepository = githubRepository;

        try (InputStream inputStream = this.readFromURL(this.jenkinsJobUrl + "lastSuccessfulBuild/api/json")) {
            this.jenkinsBuild = Constants.GSON.fromJson(new InputStreamReader(inputStream), JenkinsBuild.class);

            String[] versionParts = this.readImplementationVersion(
                    this.jenkinsBuild.getArtifacts().stream()
                            .filter(artifact -> artifact.getFileName().equalsIgnoreCase("launcher.jar"))
                            .findFirst()
                            .orElse(null)
            ).split("-");

            this.appVersion = versionParts[0] + "-" + versionParts[1];
            this.latestGitCommit = this.requestLatestGitCommit(this.jenkinsBuild.getGitCommitHash());

            this.targetDirectory = versionDirectory.resolve(this.getFullVersion());

            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    private String readImplementationVersion(JenkinsBuild.BuildArtifact artifact) throws Exception {
        try (JarInputStream jarInputStream = new JarInputStream(this.readFromURL(this.getArtifactDownloadURL(artifact)))) {
            return jarInputStream.getManifest().getMainAttributes().getValue("Implementation-Version");
        }
    }

    private String getArtifactDownloadURL(JenkinsBuild.BuildArtifact buildArtifact) {
        return this.jenkinsJobUrl + "lastSuccessfulBuild/artifact/" + buildArtifact.getRelativePath();
    }

    @Override
    public boolean installFile(String name, Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            Files.createDirectories(path.getParent());

            JenkinsBuild.BuildArtifact buildArtifact = this.jenkinsBuild.getArtifacts().stream()
                    .filter(artifact -> artifact.getFileName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new NullPointerException(String.format("Unable to find file %s on the jenkins!", name)));

            try (InputStream inputStream = this.readFromURL(this.getArtifactDownloadURL(buildArtifact))) {
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
        return null;
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
