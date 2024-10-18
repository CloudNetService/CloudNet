/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.driver.module;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.auto.Provides;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.util.Qualifiers;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.jvm.JavaVersion;
import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.util.ModuleDependencyUtil;
import jakarta.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the default implementation of the module provider.
 *
 * @since 4.0
 */
@Singleton
@Provides(ModuleProvider.class)
public class DefaultModuleProvider implements ModuleProvider {

  public static final Path DEFAULT_LIB_DIR = Path.of(".libs");
  public static final Path DEFAULT_MODULE_DIR = Path.of("modules");

  protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultModuleProvider.class);
  protected static final ModuleDependencyLoader DEFAULT_DEP_LOADER = new DefaultModuleDependencyLoader(DEFAULT_LIB_DIR);

  private static final Element MODULE_CONFIGURATION_ELEMENT = Element.forType(ModuleConfiguration.class);
  private static final Element DATA_DIRECTORY_ELEMENT = Element.forType(Path.class)
    .requireAnnotation(Qualifiers.named("dataDirectory"));

  protected final Collection<ModuleWrapper> modules = new CopyOnWriteArrayList<>();

  protected Path moduleDirectory;
  protected ModuleProviderHandler moduleProviderHandler;
  protected ModuleDependencyLoader moduleDependencyLoader;

  /**
   * Creates a new default module provider by calling with the default library directory and the default dependency
   * loader.
   * <p>
   * The default library directory is {@link #DEFAULT_LIB_DIR}. The default dependency loader is
   * {@link #DEFAULT_DEP_LOADER}
   */
  public DefaultModuleProvider() {
    this(DEFAULT_MODULE_DIR, DEFAULT_DEP_LOADER);
  }

  /**
   * Creates a new default module provider with the given directory and dependency loader.
   *
   * @param moduleDirectory        the directory where modules are stored.
   * @param moduleDependencyLoader the dependency loader to load all module dependencies.
   * @throws NullPointerException if the given module directory or dependency loader is null.
   * @see ModuleDependencyLoader
   */
  public DefaultModuleProvider(@NonNull Path moduleDirectory, @NonNull ModuleDependencyLoader moduleDependencyLoader) {
    this.moduleDirectory = moduleDirectory;
    this.moduleDependencyLoader = moduleDependencyLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Path moduleDirectoryPath() {
    return this.moduleDirectory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moduleDirectoryPath(@NonNull Path moduleDirectory) {
    this.moduleDirectory = moduleDirectory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ModuleProviderHandler moduleProviderHandler() {
    return this.moduleProviderHandler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moduleProviderHandler(@Nullable ModuleProviderHandler moduleProviderHandler) {
    this.moduleProviderHandler = moduleProviderHandler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleDependencyLoader moduleDependencyLoader() {
    return this.moduleDependencyLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moduleDependencyLoader(@NonNull ModuleDependencyLoader moduleDependencyLoader) {
    this.moduleDependencyLoader = moduleDependencyLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<ModuleWrapper> modules() {
    return Collections.unmodifiableCollection(this.modules);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<ModuleWrapper> modules(@NonNull String group) {
    return this.modules.stream()
      .filter(module -> module.moduleConfiguration().group().equals(group))
      .toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ModuleWrapper module(@NonNull String name) {
    return this.modules.stream()
      .filter(module -> module.moduleConfiguration().name().equals(name))
      .findFirst().orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ModuleWrapper loadModule(@NonNull URL url) {
    try {
      var moduleConfiguration = this.loadModuleConfigurationIfValid(url).orElse(null);
      if (moduleConfiguration == null) {
        return null;
      }

      // initialize all dependencies of the module
      var repositories = this.collectModuleProvidedRepositories(moduleConfiguration);
      var dependencies = this.loadDependencies(repositories, moduleConfiguration);

      return this.loadAndInitialize(url, dependencies, moduleConfiguration);
    } catch (IOException | URISyntaxException exception) {
      throw new AssertionError("Exception reading module information of " + url, exception);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError("Exception creating module instance", exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ModuleWrapper loadModule(@NonNull Path path) {
    try {
      return this.loadModule(path.toUri().toURL());
    } catch (MalformedURLException exception) {
      LOGGER.error("Unable to resolve url of module path", exception);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see #loadAll(Collection)
   */
  @Override
  public @NonNull ModuleProvider loadAll() {
    var urls = new ArrayList<URL>();
    FileUtil.walkFileTree(this.moduleDirectory, (_, current) -> {
      try {
        urls.add(current.toUri().toURL());
      } catch (MalformedURLException exception) {
        LOGGER.error("Unable to resolve url of module path", exception);
      }
    }, false, "*.{jar,war}");

    this.loadAll(urls);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleProvider startAll() {
    for (var module : this.modules) {
      module.startModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleProvider reloadAll() {
    for (var module : this.modules) {
      module.reloadModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleProvider stopAll() {
    for (var module : this.modules) {
      module.stopModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleProvider unloadAll() {
    for (var module : this.modules) {
      module.unloadModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean notifyPreModuleLifecycleChange(@NonNull ModuleWrapper wrapper, @NonNull ModuleLifeCycle lifeCycle) {
    // remove the module from the loaded ones
    if (lifeCycle == ModuleLifeCycle.UNLOADED) {
      this.modules.remove(wrapper);
    }
    // post the change to the handler (if one is set)
    var handler = this.moduleProviderHandler;
    if (handler != null) {
      switch (lifeCycle) {
        case LOADED:
          return handler.handlePreModuleLoad(wrapper);
        case STARTED:
          return handler.handlePreModuleStart(wrapper);
        case RELOADING:
          return handler.handlePreModuleReload(wrapper);
        case STOPPED:
          return handler.handlePreModuleStop(wrapper);
        case UNLOADED:
          handler.handlePreModuleUnload(wrapper);
          break;
        default:
          break;
      }
    }
    // if there is no handler or the handler can't change the result - just do it
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void notifyPostModuleLifecycleChange(@NonNull ModuleWrapper wrapper, @NonNull ModuleLifeCycle lifeCycle) {
    // post the change to the handler (if one is set)
    var handler = this.moduleProviderHandler;
    if (handler != null) {
      switch (lifeCycle) {
        case LOADED -> handler.handlePostModuleLoad(wrapper);
        case STARTED -> handler.handlePostModuleStart(wrapper);
        case RELOADING -> handler.handlePostModuleReload(wrapper);
        case STOPPED -> handler.handlePostModuleStop(wrapper);
        case UNLOADED -> handler.handlePostModuleUnload(wrapper);
        default -> {
        }
      }
    }
  }

  /**
   * Loads modules from a {@link Collection} of {@link URL}s
   *
   * @param urls a collection of URLs to modules that should be loaded
   * @throws ModuleCyclicDependenciesException if cyclic dependencies are detected
   * @see ModuleWrapper#moduleLifeCycle()
   * @see ModuleLifeCycle#canChangeTo(ModuleLifeCycle)
   * @see #loadAll()
   */
  protected void loadAll(Collection<URL> urls) {
    // Collect the configurations before loading any classes
    var loadableModules = new HashMap<String, ModuleConfiguration>();
    var loadableUrls = new HashMap<String, URL>();

    for (URL url : urls) {
      try {
        this.loadModuleConfigurationIfValid(url).ifPresent(moduleConfiguration -> {
          loadableModules.put(moduleConfiguration.name(), moduleConfiguration);
          loadableUrls.put(moduleConfiguration.name(), url);
        });
      } catch (IOException | URISyntaxException exception) {
        throw new AssertionError("Exception reading module information of " + url, exception);
      }
    }

    // map for easy dependency resolution by name. this will be populated further, so it must be mutable.
    var knownModules = this.modules().stream().map(ModuleWrapper::moduleConfiguration)
      .collect(Collectors.toMap(ModuleConfiguration::name, configuration -> configuration, ($1, $2) -> {
        throw new AssertionError("Two modules must not have the same name");
      }, HashMap::new));

    // iterate in a queue-like manner. Using a map is preferable, because it makes dependency resolution easier.
    while (!loadableModules.isEmpty()) {
      var moduleName = loadableModules.keySet().iterator().next();
      var moduleConfiguration = loadableModules.remove(moduleName);
      var url = loadableUrls.remove(moduleName);
      try {
        // load the module after recursively loading all its dependencies.
        this.loadModuleAndDependencies(url, moduleConfiguration, knownModules, loadableUrls, loadableModules,
          new ArrayDeque<>());
      } catch (ReflectiveOperationException exception) {
        throw new AssertionError("Exception creating module instance for module " + moduleName, exception);
      } catch (URISyntaxException exception) {
        throw new AssertionError("Exception reading module information of " + url, exception);
      }
    }
  }

  /**
   * Recursively loads a module and its dependencies
   *
   * @param url                 the url to the module
   * @param moduleConfiguration the module configuration
   * @param knownModules        all modules that are already loaded
   * @param loadableUrls        urls to all modules that can be loaded
   * @param loadableModules     configurations to all modules that can be loaded
   * @throws ReflectiveOperationException      if there is an access problem
   * @throws ModuleCyclicDependenciesException if cyclic dependencies are detected
   * @throws URISyntaxException                if the syntax of the given url is invalid
   */
  protected void loadModuleAndDependencies(@NonNull URL url, @NonNull ModuleConfiguration moduleConfiguration,
    @NonNull Map<String, ModuleConfiguration> knownModules, @NonNull Map<String, URL> loadableUrls,
    @NonNull Map<String, ModuleConfiguration> loadableModules, @NonNull Deque<String> dependencyPath)
    throws ReflectiveOperationException, URISyntaxException {
    var moduleName = moduleConfiguration.name();
    // add this module to the dependency path. this allows for cyclic dependency detection
    dependencyPath.offerLast(moduleName);
    var repositories = this.collectModuleProvidedRepositories(moduleConfiguration);
    var dependencies = this.loadDependencies(repositories, moduleConfiguration);
    // make sure all dependencies are loaded.
    for (var dependency : dependencies.second()) {
      var dependencyName = dependency.name();
      if (dependencyPath.contains(dependencyName)) {
        // cyclic dependencies detected! add the last dependency for complete cycle and throw an error
        dependencyPath.offerLast(dependencyName);
        throw new ModuleCyclicDependenciesException(dependencyPath.toArray(String[]::new));
      }

      // do we already know the dependency?
      if (!knownModules.containsKey(dependencyName)) {
        // can we load the dependency?
        if (!loadableModules.containsKey(dependencyName)) {
          // the dependency wasn't found.
          throw new ModuleDependencyNotFoundException(dependencyName, moduleName);
        }
        // load the dependency (and its dependencies).
        var dependencyConfiguration = loadableModules.remove(dependencyName);
        var dependencyUrl = loadableUrls.remove(dependencyName);

        // recursive load for the dependency module
        this.loadModuleAndDependencies(dependencyUrl, dependencyConfiguration, knownModules, loadableUrls,
          loadableModules, dependencyPath);
      }
      // the dependency is available.
      var presentDependency = knownModules.get(dependencyName);
      // make sure the version demands are met
      ModuleDependencyUtil.checkDependencyVersion(moduleConfiguration, presentDependency, dependency);
    }
    // all the dependency requirements are met. Now we have to load the module
    this.loadAndInitialize(url, dependencies, moduleConfiguration);
    // after loading the module, store it so other modules can resolve it.
    knownModules.put(moduleName, moduleConfiguration);
    // don't forget removing this module from the dependency path
    dependencyPath.pollLast();
  }

  /**
   * Creates an {@link InjectionLayer} for the module, then calls
   * {@link #loadAndInitialize(URL, Tuple2, InjectionLayer, ModuleConfiguration, Path)}
   *
   * @param url                 the url to the module file
   * @param dependencies        the dependencies for the module
   * @param moduleConfiguration the module configuration
   * @return the loaded and initialized wrapper for the module
   * @throws ReflectiveOperationException      if there is an access problem
   * @throws URISyntaxException                if the syntax of the given url is invalid
   * @throws ModuleDependencyNotFoundException if a {@link ModuleDependency} is missing
   */
  protected @NonNull ModuleWrapper loadAndInitialize(@NonNull URL url,
    @NonNull Tuple2<Set<URL>, Set<ModuleDependency>> dependencies, @NonNull ModuleConfiguration moduleConfiguration)
    throws ReflectiveOperationException, ModuleDependencyNotFoundException, URISyntaxException {
    // get the data directory of the module
    var dataDirectory = moduleConfiguration.dataFolder(this.moduleDirectory);

    // create the injection layer for the module
    var externalLayer = InjectionLayer.ext();
    var moduleLayer = InjectionLayer.specifiedChild(externalLayer, "module", (layer, injector) -> {
      injector.installSpecified(BindingBuilder.create().bind(DATA_DIRECTORY_ELEMENT).toInstance(dataDirectory));
      injector.installSpecified(
        BindingBuilder.create().bind(MODULE_CONFIGURATION_ELEMENT).toInstance(moduleConfiguration));
    });
    return this.loadAndInitialize(url, dependencies, moduleLayer, moduleConfiguration, dataDirectory);
  }

  /**
   * @param url                 the url to the module file
   * @param dependencies        the dependencies for the module
   * @param moduleLayer         the injection layer for the module
   * @param moduleConfiguration the module configuration
   * @param dataDirectory       the data directory of the module
   * @return the loaded and initialized wrapper for the module
   * @throws ReflectiveOperationException      if there is an access problem
   * @throws URISyntaxException                if the syntax of the given url is invalid
   * @throws ModuleDependencyNotFoundException if a {@link ModuleDependency} is missing
   */
  protected @NonNull ModuleWrapper loadAndInitialize(@NonNull URL url,
    @NonNull Tuple2<Set<URL>, Set<ModuleDependency>> dependencies,
    @NonNull InjectionLayer<SpecifiedInjector> moduleLayer, @NonNull ModuleConfiguration moduleConfiguration,
    @NonNull Path dataDirectory)
    throws ReflectiveOperationException, ModuleDependencyNotFoundException, URISyntaxException {
    for (ModuleDependency dependency : dependencies.second()) {
      var wrapper = this.module(dependency.name());
      // ensure that the wrapper is present
      if (wrapper == null) {
        throw new ModuleDependencyNotFoundException(dependency.name(), moduleConfiguration.name());
      }
    }
    // create the class loader for the module
    var loader = new ModuleURLClassLoader(url, dependencies.first(), moduleLayer);
    loader.registerGlobally();
    // try to load and create the main class instance
    var mainModuleClass = loader.loadClass(moduleConfiguration.main());
    // check if the main class is an instance of the IModule class
    if (!Module.class.isAssignableFrom(mainModuleClass)) {
      throw new AssertionError(
        String.format("Module main class %s is not assignable from %s", mainModuleClass.getCanonicalName(),
          Module.class.getCanonicalName()));
    }

    // create an instance of the class and the main module wrapper
    var moduleInstance = (Module) moduleLayer.instance(mainModuleClass);
    var moduleWrapper = new DefaultModuleWrapper(url, moduleInstance, dataDirectory, this, loader,
      dependencies.second(), moduleConfiguration, moduleLayer);
    // initialize the module instance now
    moduleInstance.init(loader, moduleWrapper, moduleConfiguration);
    // register the module, load it and return the created wrapper
    this.modules.add(moduleWrapper);
    return moduleWrapper.loadModule();
  }

  /**
   * Same as {@link #findModuleConfiguration(URL)} with additional checks:
   * <ul>
   *     <li>the {@code moduleFile} must exist</li>
   *     <li>there must be no other module loaded from the same url</li>
   *     <li>the module must support the current java version</li>
   * </ul>
   *
   * @param moduleFile the module file to find the module configuration of.
   * @return the deserialized module configuration file located in the provided module file, or an empty Optional if a
   * check failed.
   * @throws ModuleConfigurationNotFoundException if the file doesn't contain a module.json.
   * @throws IOException                          if an I/O or deserialize exception occurs.
   * @throws NullPointerException                 if the given module file is null.
   */
  protected @NonNull Optional<ModuleConfiguration> loadModuleConfigurationIfValid(@NonNull URL moduleFile)
    throws IOException, URISyntaxException {
    // check if there is any other module loaded from the same url
    if (this.findModuleBySource(moduleFile).isPresent()) {
      return Optional.empty();
    }
    // check if we can load the module configuration from the file
    var moduleConfiguration = this.findModuleConfiguration(moduleFile).orElse(null);
    if (moduleConfiguration == null) {
      throw new ModuleConfigurationNotFoundException(moduleFile);
    }
    // check if the module can run on the current java version release.
    if (!moduleConfiguration.canRunOn(JavaVersion.runtimeVersion())) {
      LOGGER.warn(
        "Unable to load module {}:{} because it only supports Java {}+",
        moduleConfiguration.group(),
        moduleConfiguration.name(),
        moduleConfiguration.minJavaVersionId());
      return Optional.empty();
    }
    return Optional.of(moduleConfiguration);
  }

  /**
   * Finds the module.json file in the provided module file and deserializes it.
   *
   * @param moduleFile the module file to find the module configuration of.
   * @return the deserialized module configuration file located in the provided module file.
   * @throws IOException          if an I/O or deserialize exception occurs.
   * @throws NullPointerException if the given module file is null.
   */
  protected @NonNull Optional<ModuleConfiguration> findModuleConfiguration(@NonNull URL moduleFile) throws IOException {
    try (
      InputStream buffered = new BufferedInputStream(moduleFile.openStream());
      var inputStream = new JarInputStream(buffered)
    ) {
      JarEntry entry;
      while ((entry = inputStream.getNextJarEntry()) != null) {
        if (entry.getName().equals("module.json")) {
          var serializedModuleConfiguration = DocumentFactory.json().parse(inputStream);
          return Optional.of(serializedModuleConfiguration.toInstanceOf(ModuleConfiguration.class));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Finds a loaded module based on the given file source url.
   *
   * @param fileSource the source to find the associated module with.
   * @return the associated module to the given url.
   * @throws URISyntaxException   if the given URL is not formatted strictly according to RFC2396.
   * @throws NullPointerException if the given file source is null.
   */
  protected @NonNull Optional<ModuleWrapper> findModuleBySource(@NonNull URL fileSource) throws URISyntaxException {
    // This implementation validates that the uri's of the path do equal. From the talk "Java Puzzlers Serves Up Brain" by Benders Galore:
    // "The important thing the audience learned here is that the URL object's equals() method is, in effect, broken."
    // and further:
    // "Don't use URL; use URI instead. URI makes no attempt to compare addresses or ports."
    // And from the URI docs:
    // For two hierarchical URIs to be considered equal, their paths must be equal and their queries must
    // either both be undefined or else be equal.
    // So file URLs MUST not equal but their URI will, so use this here.
    var fileSourceUri = fileSource.toURI();
    for (var module : this.modules) {
      if (module.uri().equals(fileSourceUri)) {
        return Optional.of(module);
      }
    }
    return Optional.empty();
  }

  /**
   * Collects all repositories provided in the given {@link ModuleConfiguration} to a standard url format.
   *
   * @param configuration the module configuration to read the repositories from.
   * @return all configured repositories in a standardized way.
   * @throws NullPointerException if configuration is null.
   */
  protected @NonNull Map<String, String> collectModuleProvidedRepositories(@NonNull ModuleConfiguration configuration) {
    Map<String, String> repositories = new HashMap<>();
    if (configuration.repositories() != null) {
      // iterate over all repositories and ensure that the repository url is in a readable format.
      for (var repo : configuration.repositories()) {
        if (repo == null) {
          continue;
        }
        // ensure that all properties of the repo are present
        repo.assertComplete();
        repositories.putIfAbsent(repo.name(), repo.url().endsWith("/") ? repo.url() : repo.url() + "/");
      }
    }
    return repositories;
  }

  /**
   * Loads all dependencies defined in the module configuration file.
   *
   * @param repos         the repositories from which the dependencies can get loaded.
   * @param configuration the configuration of the module to load the dependencies of.
   * @return a pair, first are all the loaded dependencies; second are all the pending ones (module dependencies)
   * @throws AssertionError       if one dependency can't be loaded.
   * @throws NullPointerException if repos or configuration is null.
   */
  protected @NonNull Tuple2<Set<URL>, Set<ModuleDependency>> loadDependencies(
    @NonNull Map<String, String> repos,
    @NonNull ModuleConfiguration configuration
  ) {
    Set<URL> loadedDependencies = new HashSet<>();
    Set<ModuleDependency> pendingModuleDependencies = new HashSet<>();
    if (configuration.dependencies() != null) {
      // yep for later posting of events to this thing
      var handler = this.moduleProviderHandler;
      // iterate over all dependencies - these may be a module or a remote dependency (visible by the given properties)
      for (var dependency : configuration.dependencies()) {
        if (dependency == null) {
          continue;
        }
        // ensure that the required properties are set
        dependency.assertDefaultPropertiesSet();
        // decide which way to go (by url or repository). In this case we start with the developer defined url if there's one
        if (dependency.url() != null) {
          loadedDependencies.add(this.doLoadDependency(dependency, configuration, handler,
            () -> this.moduleDependencyLoader.loadModuleDependencyByUrl(configuration, dependency)));
          continue;
        }
        // check if the repository defined a repository for us to use
        if (dependency.repo() != null) {
          var repoUrl = Preconditions.checkNotNull(
            repos.get(dependency.repo()),
            "Dependency %s declared unknown repository %s as it's source",
            dependency.toString(), dependency.repo());
          loadedDependencies.add(this.doLoadDependency(dependency, configuration, handler,
            () -> this.moduleDependencyLoader.loadModuleDependencyByRepository(configuration, dependency, repoUrl)));
          continue;
        }
        // the dependency might be a dependency for a module, save this
        pendingModuleDependencies.add(dependency);
      }
    }
    // combine and return the result of the load
    return new Tuple2<>(loadedDependencies, pendingModuleDependencies);
  }

  /**
   * Tries to load a module dependency. This method is fail-fast.
   *
   * @param dependency    the dependency to load.
   * @param configuration the configuration from which the dependency was declared.
   * @param handler       the provider handler if one is set and should be notified, else null.
   * @param loader        the callback which will load the dependency.
   * @return the location of the loaded dependency in url form.
   * @throws AssertionError       if loader fails to load the dependency.
   * @throws NullPointerException if dependency, configuration, handler or loader is null.
   */
  protected @NonNull URL doLoadDependency(
    @NonNull ModuleDependency dependency,
    @NonNull ModuleConfiguration configuration,
    @Nullable ModuleProviderHandler handler,
    @NonNull Callable<URL> loader
  ) {
    // post the pre-load to the handler if necessary
    if (handler != null) {
      handler.handlePreInstallDependency(configuration, dependency);
    }
    // try to actually load the dependency
    try {
      var loadResult = loader.call();
      // handle the post load if necessary
      if (handler != null) {
        handler.handlePostInstallDependency(configuration, dependency);
      }
      // return the load result
      return loadResult;
    } catch (Exception exception) {
      throw new AssertionError(String.format("Failed to load module dependency %s", dependency), exception);
    }
  }
}
