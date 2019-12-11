package de.dytanic.cloudnet.launcher;

import de.dytanic.cloudnet.launcher.cnl.CNLInterpreter;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandCNL;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandEcho;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandVar;
import de.dytanic.cloudnet.launcher.cnl.install.CNLCommandInclude;
import de.dytanic.cloudnet.launcher.cnl.install.CNLCommandRepo;
import de.dytanic.cloudnet.launcher.update.Updater;
import de.dytanic.cloudnet.launcher.version.Dependency;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class CloudNetLauncher {
    private static final Consumer<String> PRINT = System.out::println;
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("cloudnet.launcher.config", "launcher.cnl"));
    private static final Path LAUNCHER_DIR_PATH = Paths.get(System.getProperty("cloudnet.launcher.dir", "launcher"));

    private Map<String, String> variables = new HashMap<>();
    private Map<String, String> repositories = new HashMap<>();

    private Map<String, Updater> versionSpecificUpdaters = new HashMap<>();

    private List<Dependency> dependencies = new ArrayList<>();

    private static synchronized void run(List<String> args) throws Throwable {
        final long launcherStartTime = System.currentTimeMillis();


        /*CNLInterpreter.runInterpreter(new File(targetDirectory, "driver.cnl"));

        Collection<URL> dependencyResources = new ArrayList<>();
        StringBuilder driverLibs = new StringBuilder();

        for (Dependency dependency : dependencies) {
            if (repositories.containsKey(dependency.getRepository())) {
                File dest = new File(launcherDirectory.toFile(), "libs/" +
                        dependency.getGroup().replace(".", "/") + "/" + dependency.getName() + "/" + dependency.getVersion() + "/"
                        + dependency.getName() + "-" + dependency.getVersion() + (dependency.getClassifier() != null ? "-" + dependency.getClassifier() : "") + ".jar"
                );

                driverLibs.append(dest.getAbsolutePath()).append(File.pathSeparator);

                installLibrary(repositories.get(dependency.getRepository()), dependency, dest, buffer);
                dependencyResources.add(dest.toURI().toURL());
            }
        }

        driverLibs.append(new File(targetDirectory, "driver.jar").getAbsolutePath());
        System.setProperty("cloudnet.launcher.driver.dependencies", driverLibs.toString()); //For wrapper instances

        Collection<Dependency> includes = new ArrayList<>(dependencies);

        CNLInterpreter.runInterpreter(new File(targetDirectory, "cloudnet.cnl"));

        for (Dependency entry : includes) {
            dependencies.remove(entry);
        }

        includes.clear();

        for (Dependency dependency : dependencies) {
            if (repositories.containsKey(dependency.getRepository())) {
                File dest = new File(launcherDirectory.toFile(), "libs/" +
                        dependency.getGroup().replace(".", "/") + "/" + dependency.getName() + "/" + dependency.getVersion() + "/"
                        + dependency.getName() + "-" + dependency.getVersion() + (dependency.getClassifier() != null ? "-" + dependency.getClassifier() : "") + ".jar"
                );

                installLibrary(repositories.get(dependency.getRepository()), dependency, dest, buffer);
                dependencyResources.add(dest.toURI().toURL());
            }
        }

        startApplication(args, targetDirectory, dependencyResources, variables, launcherStartTime); */
    }

    private static void installLibrary(String repoUrl, Dependency dependency, File dest) {
        if (!dest.exists()) {
            try {
                dest.getParentFile().mkdirs();

                PRINT.accept("Install from repository " + dependency.getRepository() + " " + dependency.getGroup() +
                        ":" + dependency.getName() + ":" + dependency.getVersion() + (dependency.getClassifier() != null ? "-" + dependency.getClassifier() : "") + ".jar");

                URLConnection urlConnection = new URL(repoUrl + "/" +
                        dependency.getGroup().replace(".", "/") + "/" + dependency.getName() + "/" + dependency.getVersion() + "/"
                        + dependency.getName() + "-" + dependency.getVersion() + (dependency.getClassifier() != null ? "-" + dependency.getClassifier() : "") + ".jar").openConnection();

                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                urlConnection.setDoOutput(false);
                urlConnection.setUseCaches(false);
                urlConnection.connect();

                try (InputStream inputStream = urlConnection.getInputStream()) {
                    Files.copy(inputStream, dest.toPath());
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private static void startApplication(List<String> args, File targetDirectory, Collection<URL> dependencyResources, Map<String, String> variables, long launcherStartTime) throws Throwable {
        String mainClazz;
        File target = new File(targetDirectory, "cloudnet.jar");
        File driverTarget = new File(targetDirectory, "driver.jar");

        try (JarFile jarFile = new JarFile(target)) {
            mainClazz = jarFile.getManifest().getMainAttributes().getValue("Main-Class");
        }

        if (mainClazz == null) {
            throw new RuntimeException("Cannot find Main-Class by " + target.getAbsolutePath());
        }

        dependencyResources.add(target.toURI().toURL());
        dependencyResources.add(driverTarget.toURI().toURL());

        ClassLoader classLoader = new URLClassLoader(dependencyResources.toArray(new URL[0]));

        //prepareApplication(variables);

        Method method = classLoader.loadClass(mainClazz).getMethod("main", String[].class);

        PRINT.accept("Application setup complete! " + (System.currentTimeMillis() - launcherStartTime) + "ms");
        PRINT.accept("Starting application version " + System.getProperty(Constants.CLOUDNET_SELECTED_VERSION) + "...");

        startApplication0(method, classLoader, args.toArray(new String[0]));
    }

    public void setupVariables() throws Exception {
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

    public void setupSystemProperties() {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("client.encoding.override", "UTF-8");

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

    public void setupApplication() {
        for (Path directoryPath : new Path[]{
                LAUNCHER_DIR_PATH.resolve("versions"),
                LAUNCHER_DIR_PATH.resolve("libs")
        }) {
            directoryPath.toFile().mkdirs();
        }

        this.loadVersions();

        Updater updater = this.chooseVersion();
        updater.installUpdate("versions",
                System.getProperty("cloudnet.modules.directory", variables.getOrDefault("cloudnet.modules.directory", "modules")));

        this.installDependencies();
    }


    public static synchronized void main(String... args) throws Throwable {
        setAdministrativeWarning();

        List<String> arguments = Arrays.asList(args);
        run(arguments);
    }

    private static void setAdministrativeWarning() {
        String user = System.getProperty("user.name");

        if (user.equalsIgnoreCase("root") || user.equalsIgnoreCase("administrator")) {
            PRINT.accept("===================================================================");
            PRINT.accept("You currently use an administrative user for this application");
            PRINT.accept("It's better and save, if you create an extra user which you start CloudNet");
            PRINT.accept("===================================================================");
        }
    }

    private void loadVersions() {
        // TODO: load version specific updaters
    }

    private Updater chooseVersion() {
        Stream<Updater> updaterStream = this.versionSpecificUpdaters.values().stream();

        return updaterStream.filter(updater -> !updater.getLatestGitCommit().isKnown())
                .findFirst()
                .orElse(
                        updaterStream
                                .max(Comparator.comparingLong(updater -> updater.getLatestGitCommit().getAuthor().getDate().getTime()))
                                .orElse(null)
                );
    }

    private void installDependencies() {
        // TODO: implement installation of dependencies
    }

    private static void startApplication0(Method method, ClassLoader classLoader, String... args) {
        Thread thread = new Thread(() -> {
            try {
                method.invoke(null, new Object[]{args});
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }, "Application-Thread");

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setContextClassLoader(classLoader);
        thread.start();
    }

}