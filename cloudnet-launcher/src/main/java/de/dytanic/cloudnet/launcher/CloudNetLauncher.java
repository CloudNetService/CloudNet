package de.dytanic.cloudnet.launcher;

import de.dytanic.cloudnet.launcher.cnl.CNLInterpreter;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandCNL;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandEcho;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandVar;
import de.dytanic.cloudnet.launcher.cnl.install.CNLCommandInclude;
import de.dytanic.cloudnet.launcher.cnl.install.CNLCommandRepo;
import de.dytanic.cloudnet.launcher.update.DefaultRepositoryUpdater;
import de.dytanic.cloudnet.launcher.update.IUpdater;
import de.dytanic.cloudnet.launcher.util.CloudNetModule;
import de.dytanic.cloudnet.launcher.util.Dependency;
import de.dytanic.cloudnet.launcher.util.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
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

public final class CloudNetLauncher {

    private static final Map<String, IUpdater> updateServices = new HashMap<>();

    private static final Consumer<String> PRINT = System.out::println;

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("io.netty.noPreferDirect", "true");
        System.setProperty("client.encoding.override", "UTF-8");

        updateServices.put("default", new DefaultRepositoryUpdater());
    }

    private CloudNetLauncher() {
        throw new UnsupportedOperationException();
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

    private static synchronized void run(List<String> args) throws Throwable {
        final long launcherStartTime = System.currentTimeMillis();

        PRINT.accept("Starting CloudNet launcher " + CloudNetLauncher.class.getPackage().getImplementationVersion() + "...");

        final Path configPath = Paths.get(Constants.LAUNCHER_CONFIG), launcherDirectory = Paths.get(Constants.LAUNCHER_DIR);

        for (File directory : new File[]{
                new File(launcherDirectory.toFile(), "versions"),
                new File(launcherDirectory.toFile(), "libs")
        }) {
            directory.mkdirs();
        }

        byte[] buffer = new byte[16384];
        if (!Files.exists(configPath)) {
            try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream("launcher.cnl")) {
                IOUtils.copy(buffer, inputStream, configPath);
            }
        }

        Map<String, String> variables = new HashMap<>(), repositories = new HashMap<>();

        Collection<Dependency> dependencies = new ArrayList<>();

        installCNLInterpreterCommands(repositories, dependencies);
        CNLInterpreter.runInterpreter(configPath, variables);
        overwriteCnlConfiguration(args, variables);

        //Installing default modules
        configureInstallDefaultModules(buffer, variables);

        if (variables.containsKey(Constants.CLOUDNET_REPOSITORY) && variables.containsKey(Constants.CLOUDNET_REPOSITORY_AUTO_UPDATE) &&
                variables.get(Constants.CLOUDNET_REPOSITORY_AUTO_UPDATE).equalsIgnoreCase("true")) {
            checkAutoUpdate(variables, launcherDirectory, variables.get(Constants.CLOUDNET_REPOSITORY));
        }

        File targetDirectory = new File(launcherDirectory.toFile(), "versions/" + variables.get(Constants.CLOUDNET_SELECTED_VERSION));

        if (!targetDirectory.exists()) {
            variables.put(Constants.CLOUDNET_SELECTED_VERSION, Constants.FALLBACK_VERSION);
            targetDirectory = new File(launcherDirectory.toFile(), "versions/" + Constants.FALLBACK_VERSION);

            if (!targetDirectory.exists()) {
                PRINT.accept("Extracting... fallback version " + Constants.FALLBACK_VERSION + "...");

                targetDirectory.mkdirs();
                extractFallbackVersion(targetDirectory, buffer);
            }
        }

        CNLInterpreter.runInterpreter(new File(targetDirectory, "driver.cnl"));

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

        startApplication(args, targetDirectory, dependencyResources, variables, launcherStartTime);
    }

    private static void extractFallbackVersion(File targetDirectory, byte[] buffer) throws Exception {
        String absolutePath = targetDirectory.getAbsolutePath();

        try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream(Constants.INTERNAL_CLOUDNET_JAR_FILE_NAME)) {
            IOUtils.copy(buffer, inputStream, Paths.get(absolutePath, "cloudnet.jar"));
        }

        try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream(Constants.INTERNAL_CLOUDNET_CNL_FILE_NAME)) {
            IOUtils.copy(buffer, inputStream, Paths.get(absolutePath, "cloudnet.cnl"));
        }

        try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream(Constants.INTERNAL_DRIVER_JAR_FILE_NAME)) {
            IOUtils.copy(buffer, inputStream, Paths.get(absolutePath, "driver.jar"));
        }

        try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream(Constants.INTERNAL_DRIVER_CNL_FILE_NAME)) {
            IOUtils.copy(buffer, inputStream, Paths.get(absolutePath, "driver.cnl"));
        }
    }

    private static void overwriteCnlConfiguration(List<String> arguments, Map<String, String> variables) {
        if (arguments.contains("--version") && arguments.indexOf("--version") + 1 < arguments.size()) {
            variables.put(Constants.CLOUDNET_SELECTED_VERSION, arguments.get(arguments.indexOf("--version") + 1));
        }

        if (variables.containsKey("cloudnet.auto-updte.with-embedded")) {
            variables.put("cloudnet.auto-update.with-embedded", variables.get("cloudnet.auto-updte.with-embedded"));
        }

    }

    private static void configureInstallDefaultModules(byte[] buffer, Map<String, String> variables) throws Exception {
        File directory = new File(System.getProperty("cloudnet.modules.directory", variables.getOrDefault("cloudnet.modules.directory", "modules")));

        boolean exists = directory.exists();

        if (!exists) {
            directory.mkdirs();
            configureInstallDefaultModules0(buffer, directory, true);
            return;
        }

        if (variables.containsKey(Constants.CLOUDNET_MODULES_AUTO_UPDATE_WITH_EMBEDDED) && variables.get(Constants.CLOUDNET_MODULES_AUTO_UPDATE_WITH_EMBEDDED)
                .equalsIgnoreCase("true")) {
            configureInstallDefaultModules0(buffer, directory, false);
        }
    }

    private static void configureInstallDefaultModules0(byte[] buffer, File directory, boolean insertIfNotExists) throws Exception {
        for (CloudNetModule module : Constants.DEFAULT_MODULES) {
            install(buffer, directory, module.getFileName(), insertIfNotExists);
        }
    }

    private static void install(byte[] buffer, File directory, String jarFile, boolean insertIfNotExists) throws Exception {
        try (InputStream inputStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream(jarFile)) {
            if (inputStream != null) {
                File file = new File(directory, jarFile);

                if (!insertIfNotExists && !file.exists()) {
                    return;
                }

                file.delete();
                file.createNewFile();

                try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
                    IOUtils.copy(buffer, inputStream, outputStream);

                    PRINT.accept("Applied or updated module " + jarFile + " with the local launcher resources");
                }
            }
        }
    }

    private static void installCNLInterpreterCommands(Map<String, String> repositories, Collection<Dependency> dependencies) {
        CNLInterpreter.registerCommand(new CNLCommandVar());
        CNLInterpreter.registerCommand(new CNLCommandEcho());
        CNLInterpreter.registerCommand(new CNLCommandCNL());

        CNLInterpreter.registerCommand(new CNLCommandInclude(dependencies));
        CNLInterpreter.registerCommand(new CNLCommandRepo(repositories));
    }

    private static void installLibrary(String repoUrl, Dependency dependency, File dest, byte[] buffer) {
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
                    IOUtils.copy(buffer, inputStream, dest.toPath());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void prepareApplication(Map<String, String> variables) {
        //Set properties for default dependencies
        System.setProperty("io.netty.maxDirectMemory", "0");
        System.setProperty("io.netty.leakDetectionLevel", "DISABLED");
        System.setProperty("io.netty.recycler.maxCapacity", "0");
        System.setProperty("io.netty.recycler.maxCapacity.default", "0");

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
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

        prepareApplication(variables);

        Method method = classLoader.loadClass(mainClazz).getMethod("main", String[].class);

        PRINT.accept("Application setup complete! " + (System.currentTimeMillis() - launcherStartTime) + "ms");
        PRINT.accept("Starting application version " + System.getProperty(Constants.CLOUDNET_SELECTED_VERSION) + "...");

        startApplication0(method, classLoader, args.toArray(new String[0]));
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

    private static void checkAutoUpdate(Map<String, String> variables, Path launcherDirectory, String url) {
        IUpdater updater = updateServices.get(variables.getOrDefault(Constants.CLOUDNET_REPOSITORY_TYPE, "default"));

        if (updater == null) {
            updater = updateServices.get("default");
        }

        PRINT.accept("Loading repository information...");
        if (updater.init(url) && updater.getRepositoryVersion() != null && updater.getCurrentVersion() != null) {
            PRINT.accept("Loaded data from Repository " + url);
            PRINT.accept("Repository version: " + updater.getRepositoryVersion());
            PRINT.accept("Current CloudNet AppVersion: " + updater.getCurrentVersion());
            PRINT.accept("Installing repository CloudNet version if not exists...");

            if (updater.installUpdate(new File(launcherDirectory.toFile(), "versions").getAbsolutePath(),
                    variables.get(Constants.CLOUDNET_MODULES_AUTO_UPDATE_WITH_EMBEDDED) != null &&
                            variables.get(Constants.CLOUDNET_MODULES_AUTO_UPDATE_WITH_EMBEDDED).equalsIgnoreCase("true") ?
                            System.getProperty("cloudnet.modules.directory", "modules") : null
            )) {
                PRINT.accept("Set using version to " + updater.getCurrentVersion());
                variables.put(Constants.CLOUDNET_SELECTED_VERSION, updater.getCurrentVersion());
            }
        }
    }
}