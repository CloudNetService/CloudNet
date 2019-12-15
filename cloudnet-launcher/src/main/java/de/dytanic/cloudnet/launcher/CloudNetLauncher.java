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
import java.util.function.Consumer;
import java.util.jar.JarFile;

public final class CloudNetLauncher {
    private static final Consumer<String> PRINT = System.out::println;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("cloudnet.launcher.config", "launcher.cnl"));
    private static final Path LAUNCHER_DIR_PATH = Paths.get(System.getProperty("cloudnet.launcher.dir", "launcher"));

    private static final Path LAUNCHER_VERSIONS = LAUNCHER_DIR_PATH.resolve("versions");
    private static final Path LAUNCHER_LIBS = LAUNCHER_DIR_PATH.resolve("libs");

    private Map<String, String> variables = new HashMap<>();
    private Map<String, String> repositories = new HashMap<>();

    private String gitHubRepository;
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
            this.startApplication(args, dependencyResources);
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
            throw new RuntimeException("Failed to start the application!", exception);
        }
    }

    private void setupVariables() throws IOException, CNLCommandExecuteException {
        PRINT.accept("Starting CloudNet launcher created with " + CloudNetLauncher.class.getPackage().getImplementationVersion() + "...");

        if (!Files.exists(CONFIG_PATH)) {
            try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream("launcher.cnl")) {
                Files.copy(Objects.requireNonNull(inputStream, "Unable to extract the default launcher config, is the launcher corrupted?"), CONFIG_PATH);
            }
        }

        CNLInterpreter.registerCommand(new CNLCommandVar());
        CNLInterpreter.registerCommand(new CNLCommandEcho());
        CNLInterpreter.registerCommand(new CNLCommandCNL());

        CNLInterpreter.registerCommand(new CNLCommandInclude(this.dependencies));
        CNLInterpreter.registerCommand(new CNLCommandRepo(this.repositories));

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
                updater.deleteUpdateFiles();
                System.exit(-1);
            }
        } else {
            PRINT.accept("Using installed CloudNet version " + this.selectedVersion.getCurrentVersion());
        }

        this.variables.put(Constants.CLOUDNET_SELECTED_VERSION, this.selectedVersion.getFullVersion());

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

        String selectedVersion = this.variables.get(Constants.CLOUDNET_SELECTED_VERSION);
        if (selectedVersion != null && loadedVersionInfo.containsKey(selectedVersion)) {
            return loadedVersionInfo.get(selectedVersion);
        }

        Collection<VersionInfo> versionInfoCollection = loadedVersionInfo.values();

        return versionInfoCollection.stream().filter(versionInfo -> !versionInfo.getLatestGitCommit().isKnown())
                .findFirst()
                .orElseGet(() ->
                        versionInfoCollection.stream()
                                .max(Comparator.comparingLong(versionInfo -> versionInfo.getLatestGitCommit().getTime()))
                                .orElse(null)
                );
    }

    private Map<String, VersionInfo> loadVersions() {
        Map<String, VersionInfo> versionInfo = new HashMap<>();
        this.gitHubRepository = this.variables.getOrDefault(Constants.CLOUDNET_REPOSITORY_GITHUB, "CloudNetService/CloudNet-v3");

        // handles the installing of the artifacts contained in the launcher itself
        versionInfo.put(Constants.FALLBACK_VERSION, new FallbackUpdater(LAUNCHER_VERSIONS.resolve(Constants.FALLBACK_VERSION), this.gitHubRepository));

        if (this.variables.get(Constants.CLOUDNET_REPOSITORY_AUTO_UPDATE).equalsIgnoreCase("true")) {
            Updater updater = new RepositoryUpdater(this.variables.get(Constants.CLOUDNET_REPOSITORY));

            if (updater.init(LAUNCHER_VERSIONS, this.gitHubRepository)) {
                versionInfo.put(updater.getFullVersion(), updater);
            }
        }

        try {
            Files.list(LAUNCHER_VERSIONS)
                    .forEach(path -> versionInfo.put(path.getFileName().toString(), new InstalledVersionInfo(path, this.gitHubRepository)));
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

            try (InputStream inputStream = this.selectedVersion.readFromURL(repositoryURL + "/" + dependency.toPath().toString().replace(File.separatorChar, '/'))) {
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