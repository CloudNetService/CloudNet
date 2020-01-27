package de.dytanic.cloudnet.driver.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DefaultModuleWrapper implements IModuleWrapper {

    private static final Type MODULE_CONFIGURATION_TYPE = new TypeToken<ModuleConfiguration>() {
    }.getType();

    private static final Map<String, String> defaultRepositories = Maps.of(
            new Pair<>("maven", "https://repo1.maven.org/maven2/")
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String MODULE_CONFIG_PATH = "module.json";
    private final EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> moduleTasks = Maps.newEnumMap(ModuleLifeCycle.class);
    private ModuleLifeCycle moduleLifeCycle = ModuleLifeCycle.UNLOADED;
    private URL url;
    private DefaultModule module;
    private DefaultModuleProvider moduleProvider;
    private FinalizeURLClassLoader classLoader;
    private ModuleConfiguration moduleConfiguration;
    private JsonDocument moduleConfigurationSource;
    private File moduleDirectory = new File("modules");

    public DefaultModuleWrapper(DefaultModuleProvider moduleProvider, URL url) throws Exception {
        Validate.checkNotNull(url);
        Validate.checkNotNull(moduleProvider);

        this.url = url;
        this.moduleProvider = moduleProvider;

        for (ModuleLifeCycle moduleLifeCycle : ModuleLifeCycle.values()) {
            moduleTasks.put(moduleLifeCycle, Iterables.newCopyOnWriteArrayList());
        }

        this.init(url);
    }

    public DefaultModuleWrapper(DefaultModuleProvider moduleProvider, URL url, File moduleDirectory) throws Exception {
        this(moduleProvider, url);
        this.moduleDirectory = moduleDirectory;
    }

    private void init(URL url) throws Exception {
        //ModuleConfiguration moduleConfiguration;
        //Document moduleConfigurationSource;

        try (FinalizeURLClassLoader classLoader = new FinalizeURLClassLoader(url);
             InputStream inputStream = classLoader.getResourceAsStream(MODULE_CONFIG_PATH)) {
            if (inputStream == null) {
                throw new ModuleConfigurationNotFoundException(url);
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                moduleConfigurationSource = new JsonDocument().read(reader);
                moduleConfiguration = moduleConfigurationSource.toInstanceOf(MODULE_CONFIGURATION_TYPE);
            }
        }

        if (moduleConfiguration == null) {
            throw new ModuleConfigurationNotFoundException(url);
        }

        if (moduleConfiguration.getGroup() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("group");
        }
        if (moduleConfiguration.getName() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("name");
        }
        if (moduleConfiguration.getVersion() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("version");
        }
        if (moduleConfiguration.getMain() == null) {
            throw new ModuleConfigurationPropertyNotFoundException("main");
        }

        Map<String, String> repositories = Maps.newHashMap(defaultRepositories);

        List<URL> urls = Iterables.newArrayList();
        urls.add(url);

        if (moduleConfiguration.getRepos() != null) {
            for (ModuleRepository moduleRepository : moduleConfiguration.getRepos()) {
                if (moduleRepository.getName() != null && moduleRepository.getUrl() != null) {
                    repositories.put(moduleRepository.getName(), moduleRepository.getUrl().endsWith("/") ? moduleRepository.getUrl() : moduleRepository.getUrl() + "/");
                }
            }
        }

        if (moduleConfiguration.getDependencies() != null) {
            for (ModuleDependency moduleDependency : moduleConfiguration.getDependencies()) {
                if (moduleDependency.getGroup() != null && moduleDependency.getName() != null && moduleDependency.getVersion() != null) {
                    if (moduleDependency.getUrl() != null) {
                        if (moduleProvider.getModuleProviderHandler() != null) {
                            moduleProvider.getModuleProviderHandler().handlePreInstallDependency(this, moduleDependency);
                        }

                        urls.add(moduleProvider.getModuleDependencyLoader().loadModuleDependencyByUrl(moduleConfiguration, moduleDependency, repositories));

                        if (moduleProvider.getModuleProviderHandler() != null) {
                            moduleProvider.getModuleProviderHandler().handlePostInstallDependency(this, moduleDependency);
                        }
                        continue;
                    }

                    if (moduleDependency.getRepo() != null && repositories.containsKey(moduleDependency.getRepo())) {
                        if (moduleProvider.getModuleProviderHandler() != null) {
                            moduleProvider.getModuleProviderHandler().handlePreInstallDependency(this, moduleDependency);
                        }

                        urls.add(moduleProvider.getModuleDependencyLoader().loadModuleDependencyByRepository(moduleConfiguration, moduleDependency, repositories));

                        if (moduleProvider.getModuleProviderHandler() != null) {
                            moduleProvider.getModuleProviderHandler().handlePostInstallDependency(this, moduleDependency);
                        }
                    }
                }
            }
        }

        this.classLoader = new FinalizeURLClassLoader(urls.toArray(new URL[0]));
        Class<?> clazz = classLoader.loadClass(moduleConfiguration.getMainClass());

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

    @Override
    public EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> getModuleTasks() {
        return new EnumMap<>(this.moduleTasks);
    }

    @Override
    public IModuleWrapper loadModule() {
        if (moduleLifeCycle == ModuleLifeCycle.UNLOADED) {
            moduleProvider.getModuleProviderHandler().handlePreModuleLoad(this);
            fireTasks(this.moduleTasks.get(ModuleLifeCycle.LOADED));
            moduleProvider.getModuleProviderHandler().handlePostModuleLoad(this);

            this.moduleLifeCycle = ModuleLifeCycle.LOADED;
        }
        return this;
    }

    @Override
    public IModuleWrapper startModule() {
        this.loadModule();

        if (moduleLifeCycle == ModuleLifeCycle.LOADED || moduleLifeCycle == ModuleLifeCycle.STOPPED) {
            moduleProvider.getModuleProviderHandler().handlePreModuleStart(this);
            if (this.moduleConfiguration.getDependencies() != null) {
                for (ModuleDependency moduleDependency : this.moduleConfiguration.getDependencies()) {
                    if (moduleDependency != null && moduleDependency.getGroup() != null && moduleDependency.getName() != null &&
                            moduleDependency.getVersion() != null && moduleDependency.getRepo() == null &&
                            moduleDependency.getUrl() == null) {
                        IModuleWrapper moduleWrapper = Iterables.first(this.getModuleProvider().getModules(), module -> module.getModuleConfiguration().getGroup().equals(moduleDependency.getGroup()) &&
                                module.getModuleConfiguration().getName().equals(moduleDependency.getName()));

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

            fireTasks(this.moduleTasks.get(ModuleLifeCycle.STARTED));
            moduleProvider.getModuleProviderHandler().handlePostModuleStart(this);
            this.moduleLifeCycle = ModuleLifeCycle.STARTED;
        }

        return this;
    }

    @Override
    public IModuleWrapper stopModule() {
        if (moduleLifeCycle == ModuleLifeCycle.STARTED || moduleLifeCycle == ModuleLifeCycle.LOADED) {
            moduleProvider.getModuleProviderHandler().handlePreModuleStop(this);
            fireTasks(this.moduleTasks.get(ModuleLifeCycle.STOPPED));
            moduleProvider.getModuleProviderHandler().handlePostModuleStop(this);
            this.moduleLifeCycle = ModuleLifeCycle.STOPPED;
        }

        return this;
    }

    @Override
    public IModuleWrapper unloadModule() {
        if (moduleLifeCycle != ModuleLifeCycle.UNLOADED) {
            stopModule();
        }

        moduleProvider.getModuleProviderHandler().handlePreModuleUnload(this);
        fireTasks(moduleTasks.get(ModuleLifeCycle.UNLOADED));

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
        moduleProvider.getModuleProviderHandler().handlePostModuleUnload(this);
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
        return defaultRepositories;
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