package de.dytanic.cloudnet.driver.module.defaults;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.*;
import de.dytanic.cloudnet.driver.module.dependency.ModuleDependency;
import de.dytanic.cloudnet.driver.module.dependency.ModuleDependencyNotFoundException;
import de.dytanic.cloudnet.driver.module.dependency.ModuleRepository;
import de.dytanic.cloudnet.driver.module.repository.RepositoryModuleInfo;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultModuleWrapper implements IModuleWrapper {

    private static final Type MODULE_CONFIGURATION_TYPE = new TypeToken<ModuleConfiguration>() {
    }.getType();

    private static final Map<String, String> DEFAULT_REPOSITORIES = Collections.singletonMap("maven", "https://repo1.maven.org/maven2/");

    private static final String MODULE_CONFIG_PATH = "module.json";
    private final EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> moduleTasks = new EnumMap<>(ModuleLifeCycle.class);
    private ModuleLifeCycle moduleLifeCycle = ModuleLifeCycle.UNLOADED;
    private final URL url;
    private DefaultModule module;
    private final DefaultModuleProvider moduleProvider;
    private FinalizeURLClassLoader classLoader;
    private ModuleConfiguration moduleConfiguration;
    private JsonDocument moduleConfigurationSource;
    private File moduleDirectory = new File("modules");

    public DefaultModuleWrapper(DefaultModuleProvider moduleProvider, URL url) throws Exception {
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(moduleProvider);

        this.url = url;
        this.moduleProvider = moduleProvider;

        for (ModuleLifeCycle moduleLifeCycle : ModuleLifeCycle.values()) {
            this.moduleTasks.put(moduleLifeCycle, new CopyOnWriteArrayList<>());
        }

        this.init(url);
    }

    public DefaultModuleWrapper(DefaultModuleProvider moduleProvider, URL url, File moduleDirectory) throws Exception {
        this(moduleProvider, url);
        this.moduleDirectory = moduleDirectory;
    }

    private void init(URL url) throws Exception {
        this.loadConfiguration(url);

        if (this.moduleProvider.isAutoUpdateEnabled()) {
            URL updatedUrl = this.installUpdate(url);
            if (updatedUrl != null) {
                url = updatedUrl;
                this.loadConfiguration(url);
            }
        }

        Map<String, String> repositories = new HashMap<>(DEFAULT_REPOSITORIES);

        List<URL> urls = new ArrayList<>();
        urls.add(url);

        if (this.moduleConfiguration.getRepos() != null) {
            for (ModuleRepository moduleRepository : this.moduleConfiguration.getRepos()) {
                if (moduleRepository.getName() != null && moduleRepository.getUrl() != null) {
                    repositories.put(moduleRepository.getName(), moduleRepository.getUrl().endsWith("/") ? moduleRepository.getUrl() : moduleRepository.getUrl() + "/");
                }
            }
        }

        if (this.moduleConfiguration.getDependencies() != null) {
            for (ModuleDependency moduleDependency : this.moduleConfiguration.getDependencies()) {
                if (moduleDependency.getGroup() != null && moduleDependency.getName() != null && moduleDependency.getVersion() != null) {
                    if (moduleDependency.getUrl() != null) {
                        if (this.moduleProvider.getModuleProviderHandler() != null) {
                            this.moduleProvider.getModuleProviderHandler().handlePreInstallDependency(this, moduleDependency);
                        }

                        urls.add(this.moduleProvider.getModuleDependencyLoader().loadModuleDependencyByUrl(this.moduleConfiguration, moduleDependency, repositories));

                        if (this.moduleProvider.getModuleProviderHandler() != null) {
                            this.moduleProvider.getModuleProviderHandler().handlePostInstallDependency(this, moduleDependency);
                        }
                        continue;
                    }

                    if (moduleDependency.getRepo() != null && repositories.containsKey(moduleDependency.getRepo())) {
                        if (this.moduleProvider.getModuleProviderHandler() != null) {
                            this.moduleProvider.getModuleProviderHandler().handlePreInstallDependency(this, moduleDependency);
                        }

                        urls.add(this.moduleProvider.getModuleDependencyLoader().loadModuleDependencyByRepository(this.moduleConfiguration, moduleDependency, repositories));

                        if (this.moduleProvider.getModuleProviderHandler() != null) {
                            this.moduleProvider.getModuleProviderHandler().handlePostInstallDependency(this, moduleDependency);
                        }
                    }
                }
            }
        }

        this.classLoader = new FinalizeURLClassLoader(urls.toArray(new URL[0]));
        Class<?> clazz = this.classLoader.loadClass(this.moduleConfiguration.getMainClass());

        if (!DefaultModule.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Invalid module class type");
        }

        this.module = (DefaultModule) clazz.getDeclaredConstructor().newInstance();

        this.module.moduleWrapper = this;
        this.module.moduleConfig = this.moduleConfiguration;
        this.module.classLoader = this.classLoader;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.isAnnotationPresent(ModuleTask.class)) {
                ModuleTask moduleTask = method.getAnnotation(ModuleTask.class);
                this.moduleTasks.get(moduleTask.event()).add(new DefaultModuleTaskEntry(this, moduleTask, method));
            }
        }
    }

    private void loadConfiguration(URL url) throws Exception {
        try (FinalizeURLClassLoader classLoader = new FinalizeURLClassLoader(url);
             InputStream inputStream = classLoader.getResourceAsStream(MODULE_CONFIG_PATH)) {
            if (inputStream == null) {
                throw new ModuleConfigurationNotFoundException(url);
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                this.moduleConfigurationSource = new JsonDocument().read(reader);
                this.moduleConfiguration = this.moduleConfigurationSource.toInstanceOf(MODULE_CONFIGURATION_TYPE);
            }
        }

        if (this.moduleConfiguration == null) {
            throw new ModuleConfigurationNotFoundException(url);
        }

        if (this.moduleConfiguration.getGroup() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("group");
        }
        if (this.moduleConfiguration.getName() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("name");
        }
        if (this.moduleConfiguration.getVersion() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("version");
        }
        if (this.moduleConfiguration.getMain() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("main");
        }
    }

    private URL installUpdate(URL url) throws URISyntaxException, IOException {
        this.moduleProvider.getModuleProviderHandler().handleCheckForUpdates(this);

        RepositoryModuleInfo info = this.moduleProvider.getModuleRepository().getRepositoryModuleInfo(this.moduleConfiguration.getModuleId());
        if (info == null) {
            return null;
        }

        if (info.getModuleId().getVersion().equals(this.moduleConfiguration.getModuleId().getVersion())) {
            return null;
        }

        URI uri = url.toURI();
        URL downloadUrl = new URL(this.moduleProvider.getModuleRepository().getModuleURL(info.getModuleId()));

        if (!uri.getScheme().equals("file")) {
            return downloadUrl;
        }

        Path path = Paths.get(uri);

        this.moduleProvider.getModuleProviderHandler().handlePreInstallUpdate(this, info);

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            if (!this.moduleProvider.getModuleInstaller().installModule(info, outputStream)) {
                this.moduleProvider.getModuleProviderHandler().handleInstallUpdateFailed(this, info);
                return null;
            }
        }

        this.moduleProvider.getModuleProviderHandler().handlePostInstallUpdate(this, info);

        return path.toUri().toURL();
    }

    @Override
    public EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> getModuleTasks() {
        return new EnumMap<>(this.moduleTasks);
    }

    @Override
    public IModuleWrapper loadModule() {
        if (this.moduleLifeCycle == ModuleLifeCycle.UNLOADED && this.moduleProvider.getModuleProviderHandler().handlePreModuleLoad(this)) {
            this.fireTasks(this.moduleTasks.get(this.moduleLifeCycle = ModuleLifeCycle.LOADED));
            this.moduleProvider.getModuleProviderHandler().handlePostModuleLoad(this);
        }

        return this;
    }

    @Override
    public IModuleWrapper startModule() {
        this.loadModule();

        if ((this.moduleLifeCycle == ModuleLifeCycle.LOADED || this.moduleLifeCycle == ModuleLifeCycle.STOPPED)
                && this.moduleProvider.getModuleProviderHandler().handlePreModuleStart(this)) {
            if (this.moduleConfiguration.getDependencies() != null) {
                for (ModuleDependency moduleDependency : this.moduleConfiguration.getDependencies()) {
                    if (moduleDependency != null && moduleDependency.getGroup() != null && moduleDependency.getName() != null &&
                            moduleDependency.getVersion() != null && moduleDependency.getRepo() == null &&
                            moduleDependency.getUrl() == null) {

                        IModuleWrapper moduleWrapper = this.getModuleProvider().getModules().stream().filter(module -> module.getModuleConfiguration().getGroup().equals(moduleDependency.getGroup()) &&
                                module.getModuleConfiguration().getName().equals(moduleDependency.getName())).findFirst().orElse(null);

                        if (moduleWrapper != null) {
                            moduleWrapper.startModule();
                        } else {
                            new ModuleDependencyNotFoundException("Module Dependency for " + moduleDependency.getGroup() + ":" + moduleDependency.getName() +
                                    ":" + moduleDependency.getVersion()).printStackTrace();
                            return this;
                        }
                    }
                }
            }

            this.fireTasks(this.moduleTasks.get(ModuleLifeCycle.STARTED));
            this.moduleProvider.getModuleProviderHandler().handlePostModuleStart(this);
            this.moduleLifeCycle = ModuleLifeCycle.STARTED;
        }

        return this;
    }

    @Override
    public IModuleWrapper stopModule() {
        if ((this.moduleLifeCycle == ModuleLifeCycle.STARTED || this.moduleLifeCycle == ModuleLifeCycle.LOADED)
                && this.moduleProvider.getModuleProviderHandler().handlePreModuleStop(this)) {
            this.fireTasks(this.moduleTasks.get(ModuleLifeCycle.STOPPED));
            this.moduleProvider.getModuleProviderHandler().handlePostModuleStop(this);
            this.moduleLifeCycle = ModuleLifeCycle.STOPPED;
        }

        return this;
    }

    @Override
    public IModuleWrapper unloadModule() {
        if (this.moduleLifeCycle != ModuleLifeCycle.UNLOADED) {
            this.stopModule();
        }

        this.moduleProvider.getModuleProviderHandler().handlePreModuleUnload(this);
        this.fireTasks(this.moduleTasks.get(ModuleLifeCycle.UNLOADED));

        this.moduleLifeCycle = ModuleLifeCycle.UNUSEABLE;
        this.moduleProvider.moduleWrappers.remove(this);
        this.moduleTasks.clear();
        this.module = null;

        try {
            this.classLoader.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        this.classLoader = null;
        this.moduleProvider.getModuleProviderHandler().handlePostModuleUnload(this);
        return this;
    }

    @Override
    public File getDataFolder() {
        return this.getModuleConfigurationSource() != null && this.getModuleConfigurationSource().contains("dataFolder") ?
                new File(this.getModuleConfigurationSource().getString("dataFolder"))
                :
                new File(this.moduleDirectory, this.getModuleConfiguration().getName()
                );
    }

    @Override
    public Map<String, String> getDefaultRepositories() {
        return DEFAULT_REPOSITORIES;
    }


    private void fireTasks(List<IModuleTaskEntry> entries) {
        entries.sort((o1, o2) -> o2.getTaskInfo().order() - o1.getTaskInfo().order());

        for (IModuleTaskEntry entry : entries) {
            try {
                entry.getHandler().setAccessible(true);
                entry.getHandler().invoke(entry.getModule());
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    public ModuleLifeCycle getModuleLifeCycle() {
        return this.moduleLifeCycle;
    }

    @Override
    public URL getUrl() {
        return this.url;
    }

    public DefaultModule getModule() {
        return this.module;
    }

    public DefaultModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    public FinalizeURLClassLoader getClassLoader() {
        return this.classLoader;
    }

    public ModuleConfiguration getModuleConfiguration() {
        return this.moduleConfiguration;
    }

    public JsonDocument getModuleConfigurationSource() {
        return this.moduleConfigurationSource;
    }
}