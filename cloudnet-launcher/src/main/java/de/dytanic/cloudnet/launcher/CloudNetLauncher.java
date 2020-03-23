package de.dytanic.cloudnet.launcher;

import de.dytanic.cloudnet.launcher.cnl.CNLCommandExecuteException;
import de.dytanic.cloudnet.launcher.cnl.CNLInterpreter;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandCNL;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandEcho;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandVar;
import de.dytanic.cloudnet.launcher.cnl.install.CNLCommandInclude;
import de.dytanic.cloudnet.launcher.cnl.install.CNLCommandRepo;
import de.dytanic.cloudnet.launcher.version.InstalledVersionInfo;
import de.dytanic.cloudnet.launcher.version.VersionInfo;
import de.dytanic.cloudnet.launcher.version.update.FallbackUpdater;
import de.dytanic.cloudnet.launcher.version.update.RepositoryUpdater;
import de.dytanic.cloudnet.launcher.version.update.Updater;
import de.dytanic.cloudnet.launcher.version.update.jenkins.JenkinsUpdater;
import de.dytanic.cloudnet.launcher.version.util.Dependency;
import de.dytanic.cloudnet.launcher.version.util.GitCommit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class CloudNetLauncher {
    private static final Consumer<String> PRINT = System.out::println;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("cloudnet.launcher.config", "launcher.cnl"));
    private static final Path LAUNCHER_DIR_PATH = Paths.get(System.getProperty("cloudnet.launcher.dir", "launcher"));

    private static final Path LAUNCHER_VERSIONS = LAUNCHER_DIR_PATH.resolve("versions");
    private static final Path LAUNCHER_LIBS = LAUNCHER_DIR_PATH.resolve("libs");

    private Map<String, String> variables = new HashMap<>();
    private Map<String, String> repositories = new HashMap<>();

    private Collection<String> updaterVersionNames = new HashSet<>();

    private VersionInfo selectedVersion;

    private List<Dependency> dependencies = new ArrayList<>();

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("client.encoding.override", "UTF-8");

        String user = System.getProperty("user.name");

        if (user.equalsIgnoreCase("root") || user.equalsIgnoreCase("administrator")) {
            PRINT.accept("===================================================================");
            PRINT.accept("You currently use an administrative user for this application");
            PRINT.accept("It's better and save, if you create an extra user which you start CloudNet");
            PRINT.accept("===================================================================");
        }

        new CloudNetLauncher().run(args);
    }

    private void run(String[] args) {
        PRINT.accept("Running CloudNet launcher created with " + CloudNetLauncher.class.getPackage().getImplementationVersion() + "...");

        try {
            this.setupVariables();
        } catch (IOException | CNLCommandExecuteException exception) {
            throw new RuntimeException("Unable to setup the launcher variables!", exception);
        }

        try {
            this.setupApplication();
        } catch (IOException exception) {
            throw new RuntimeException("Unable to setup the application!", exception);
        }

        Collection<URL> dependencyResources;
        try {
            dependencyResources = this.installDependencies();
        } catch (IOException | CNLCommandExecuteException exception) {
            throw new RuntimeException("Unable to install needed dependencies!", exception);
        }

        this.setSystemProperties();

        try {
            PRINT.accept("Starting CloudNet...");
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));

            this.startApplication(args, dependencyResources);
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InterruptedException exception) {
            throw new RuntimeException("Failed to start the application!", exception);
        }
    }

    private void setupVariables() throws IOException, CNLCommandExecuteException {
        CNLInterpreter.registerCommand(new CNLCommandVar());
        CNLInterpreter.registerCommand(new CNLCommandEcho());
        CNLInterpreter.registerCommand(new CNLCommandCNL());

        CNLInterpreter.registerCommand(new CNLCommandInclude(this.dependencies));
        CNLInterpreter.registerCommand(new CNLCommandRepo(this.repositories));

        try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream("launcher.cnl")) {
            Objects.requireNonNull(inputStream, "Unable to find the default launcher config, is the launcher corrupted?");

            if (Files.exists(CONFIG_PATH)) {
                // adding the default variables first to make sure that all needed variables are existing
                CNLInterpreter.runInterpreter(inputStream, this.variables);
            } else {
                Files.copy(inputStream, CONFIG_PATH);
            }
        }

        CNLInterpreter.runInterpreter(CONFIG_PATH, this.variables);
    }

    private void setupApplication() throws IOException {
        Files.createDirectories(LAUNCHER_VERSIONS);
        Files.createDirectories(LAUNCHER_LIBS);

        this.selectedVersion = this.selectVersion();

        if (this.selectedVersion instanceof Updater) {
            Updater updater = (Updater) this.selectedVersion;

            PRINT.accept(String.format("Installing version %s from updater %s...", updater.getCurrentVersion(), updater.getClass().getSimpleName()));

            if (updater.installUpdate(this.variables.getOrDefault("cloudnet.modules.directory", "modules"))) {
                PRINT.accept("Successfully installed CloudNet version " + updater.getCurrentVersion());
            } else {
                PRINT.accept("Error while installing CloudNet!");
                LauncherUtils.deleteFile(updater.getTargetDirectory());
                System.exit(-1);
            }
        } else {
            PRINT.accept("Using installed CloudNet version " + this.selectedVersion.getCurrentVersion());
        }

        this.variables.put(LauncherUtils.CLOUDNET_SELECTED_VERSION, this.selectedVersion.getFullVersion());

        GitCommit latestGitCommit = this.selectedVersion.getLatestGitCommit();

        if (latestGitCommit.isKnown() && latestGitCommit.hasInformation()) {
            GitCommit.GitCommitAuthor author = latestGitCommit.getAuthor();

            PRINT.accept("");

            PRINT.accept(String.format("Latest commit is %s, authored by %s (%s)", latestGitCommit.getSha(), author.getName(), author.getEmail()));
            PRINT.accept(String.format("Commit message: \"%s\"", latestGitCommit.getMessage()));
            PRINT.accept("Commit time: " + DATE_FORMATTER.format(
                    author.getDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
            ));

            PRINT.accept("");
        } else {
            PRINT.accept("Unable to fetch the latest git commit, custom or outdated build?");
        }

    }

    private VersionInfo selectVersion() {
        Map<String, VersionInfo> loadedVersionInfo = this.loadVersions();

        String selectedVersion = this.variables.get(LauncherUtils.CLOUDNET_SELECTED_VERSION);
        if (selectedVersion != null && loadedVersionInfo.containsKey(selectedVersion)) {
            return loadedVersionInfo.get(selectedVersion);
        }

        Collection<VersionInfo> versionInfoCollection = loadedVersionInfo.values();

        VersionInfo newestVersion = versionInfoCollection.stream()
                .filter(versionInfo -> versionInfo.getLatestGitCommit().hasInformation())
                .max(Comparator.comparingLong(versionInfo -> versionInfo.getLatestGitCommit().getTime()))
                .orElse(loadedVersionInfo.get(LauncherUtils.FALLBACK_VERSION));

        // Looking for installed versions that are outdated and deleting them.
        // This is necessary because too many installed versions can lead to rate-limiting of GitHub's api,
        // which will make version selection impossible.
        versionInfoCollection.stream()
                .filter(versionInfo -> versionInfo instanceof InstalledVersionInfo
                        && !versionInfo.getFullVersion().equals(newestVersion.getFullVersion())
                        && !this.updaterVersionNames.contains(versionInfo.getFullVersion()))
                .forEach(versionInfo -> {
                    PRINT.accept(String.format("Deleting outdated version %s...", versionInfo.getFullVersion()));
                    try {
                        LauncherUtils.deleteFile(versionInfo.getTargetDirectory());
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });

        return newestVersion;
    }

    private Map<String, VersionInfo> loadVersions() {
        Map<String, VersionInfo> versionInfo = new HashMap<>();
        String gitHubRepository = this.variables.getOrDefault(LauncherUtils.CLOUDNET_REPOSITORY_GITHUB, "CloudNetService/CloudNet-v3");

        // handles the installing of the artifacts contained in the launcher itself
        versionInfo.put(LauncherUtils.FALLBACK_VERSION, new FallbackUpdater(LAUNCHER_VERSIONS.resolve(LauncherUtils.FALLBACK_VERSION), gitHubRepository));

        if (this.variables.getOrDefault(LauncherUtils.LAUNCHER_DEV_MODE, "false").equalsIgnoreCase("true")) {
            // dev mode is on, only using the fallback version
            return versionInfo;
        }

        if (this.variables.getOrDefault(LauncherUtils.CLOUDNET_REPOSITORY_AUTO_UPDATE, "true").equalsIgnoreCase("true")) {
            Updater updater = new RepositoryUpdater(this.variables.get(LauncherUtils.CLOUDNET_REPOSITORY));

            if (updater.init(LAUNCHER_VERSIONS, gitHubRepository)) {
                versionInfo.put(updater.getFullVersion(), updater);
            }
        }

        if (this.variables.getOrDefault(LauncherUtils.CLOUDNET_SNAPSHOTS, "false").equalsIgnoreCase("true")) {
            Updater updater = new JenkinsUpdater(this.variables.getOrDefault(LauncherUtils.CLOUDNET_SNAPSHOTS_JOB_URL,
                    "https://ci.cloudnetservice.eu/job/CloudNetService/job/CloudNet-v3/job/development/"));

            if (updater.init(LAUNCHER_VERSIONS, gitHubRepository)) {
                versionInfo.put(updater.getFullVersion(), updater);
            }
        }

        this.updaterVersionNames = versionInfo.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Updater)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        try {
            Files.list(LAUNCHER_VERSIONS)
                    .forEach(path -> versionInfo.put(path.getFileName().toString(), new InstalledVersionInfo(path, gitHubRepository)));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return versionInfo;
    }

    private Collection<URL> installDependencies() throws IOException, CNLCommandExecuteException {
        CNLInterpreter.runInterpreter(this.selectedVersion.getTargetDirectory().resolve("driver.cnl"));
        CNLInterpreter.runInterpreter(this.selectedVersion.getTargetDirectory().resolve("cloudnet.cnl"));

        Collection<URL> dependencyResources = new ArrayList<>();

        for (Dependency dependency : this.dependencies) {
            if (this.repositories.containsKey(dependency.getRepository())) {
                Path path = LAUNCHER_LIBS.resolve(dependency.toPath());

                this.installLibrary(this.repositories.get(dependency.getRepository()), dependency, path);

                dependencyResources.add(path.toUri().toURL());
            }
        }

        return dependencyResources;
    }

    private void installLibrary(String repositoryURL, Dependency dependency, Path path) throws IOException {
        if (!Files.exists(path)) {

            Files.createDirectories(path.getParent());

            String dependencyName = dependency.getGroup() + ":" + dependency.getName() + ":"
                    + dependency.getVersion() + (dependency.getClassifier() != null ? "-" + dependency.getClassifier() : "") + ".jar";

            PRINT.accept(String.format("Installing dependency %s from repository %s...", dependencyName, dependency.getRepository()));

            try (InputStream inputStream = LauncherUtils.readFromURL(repositoryURL + "/" + dependency.toPath().toString().replace(File.separatorChar, '/'))) {
                Files.copy(inputStream, path);
            }

        }
    }

    private void setSystemProperties() {
        //Set properties for default dependencies
        System.setProperty("io.netty.noPreferDirect", "true");
        System.setProperty("io.netty.maxDirectMemory", "0");
        System.setProperty("io.netty.leakDetectionLevel", "DISABLED");
        System.setProperty("io.netty.recycler.maxCapacity", "0");
        System.setProperty("io.netty.recycler.maxCapacity.default", "0");

        for (Map.Entry<String, String> entry : this.variables.entrySet()) {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private void startApplication(String[] args, Collection<URL> dependencyResources) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Path targetPath = this.selectedVersion.getTargetDirectory().resolve("cloudnet.jar");
        Path driverTargetPath = this.selectedVersion.getTargetDirectory().resolve("driver.jar");

        String mainClass;
        try (JarFile jarFile = new JarFile(targetPath.toFile())) {
            mainClass = jarFile.getManifest().getMainAttributes().getValue("Main-Class");
        }

        if (mainClass == null) {
            throw new RuntimeException("Cannot find Main-Class from " + targetPath.toAbsolutePath());
        }

        dependencyResources.add(targetPath.toUri().toURL());
        dependencyResources.add(driverTargetPath.toUri().toURL());

        ClassLoader classLoader = new URLClassLoader(dependencyResources.toArray(new URL[0]));
        Method method = classLoader.loadClass(mainClass).getMethod("main", String[].class);

        method.invoke(null, (Object) args);
    }

}