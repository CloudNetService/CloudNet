/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.module;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultModuleProvider implements IModuleProvider {

  protected static final Path DEFAULT_MODULE_DIR = Path.of("modules");
  protected static final Logger LOGGER = LogManager.logger(DefaultModuleProvider.class);
  protected static final IModuleDependencyLoader DEFAULT_DEPENDENCY_LOADER = new DefaultMemoryModuleDependencyLoader();

  protected final Collection<IModuleWrapper> modules = new CopyOnWriteArrayList<>();

  protected Path moduleDirectory;
  protected IModuleProviderHandler moduleProviderHandler;
  protected IModuleDependencyLoader moduleDependencyLoader;

  public DefaultModuleProvider() {
    this(DEFAULT_MODULE_DIR, DEFAULT_DEPENDENCY_LOADER);
  }

  public DefaultModuleProvider(Path moduleDirectory, IModuleDependencyLoader moduleDependencyLoader) {
    this.moduleDirectory = moduleDirectory;
    this.moduleDependencyLoader = moduleDependencyLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Path moduleDirectoryPath() {
    return this.moduleDirectory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moduleDirectoryPath(@NotNull Path moduleDirectory) {
    this.moduleDirectory = Preconditions.checkNotNull(moduleDirectory, "moduleDirectory");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable IModuleProviderHandler moduleProviderHandler() {
    return this.moduleProviderHandler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moduleProviderHandler(@Nullable IModuleProviderHandler moduleProviderHandler) {
    this.moduleProviderHandler = moduleProviderHandler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull IModuleDependencyLoader moduleDependencyLoader() {
    return this.moduleDependencyLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moduleDependencyLoader(@NotNull IModuleDependencyLoader moduleDependencyLoader) {
    this.moduleDependencyLoader = Preconditions.checkNotNull(moduleDependencyLoader, "moduleDependencyLoader");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<IModuleWrapper> modules() {
    return Collections.unmodifiableCollection(this.modules);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<IModuleWrapper> modules(@NotNull String group) {
    return this.modules.stream()
      .filter(module -> module.moduleConfiguration().group().equals(group))
      .collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IModuleWrapper module(@NotNull String name) {
    return this.modules.stream()
      .filter(module -> module.moduleConfiguration().name().equals(name))
      .findFirst().orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IModuleWrapper loadModule(@NotNull URL url) {
    try {
      // check if there is any other module loaded from the same url
      if (this.findModuleBySource(url).isPresent()) {
        return null;
      }
      // check if we can load the module configuration from the file
      var moduleConfiguration = this.findModuleConfiguration(url).orElse(null);
      if (moduleConfiguration == null) {
        throw new ModuleConfigurationNotFoundException(url);
      }
      // validate that the module configuration contains all necessary information
      moduleConfiguration.assertRequiredPropertiesSet();
      // check if the module can run on the current java version release.
      if (!moduleConfiguration.canRunOn(JavaVersion.runtimeVersion())) {
        LOGGER.warning(String.format("Unable to load module %s:%s because it only supports Java %d+",
          moduleConfiguration.group(), moduleConfiguration.name(), moduleConfiguration.minJavaVersionId()));
        return null;
      }
      // initialize all dependencies of the module
      var repositories = this.collectModuleProvidedRepositories(moduleConfiguration);
      var dependencies = this.loadDependencies(repositories, moduleConfiguration);
      // create the class loader for the module
      var loader = new FinalizeURLClassLoader(url, dependencies.first());
      loader.registerGlobally();
      // try to load and create the main class instance
      var mainModuleClass = loader.loadClass(moduleConfiguration.mainClass());
      // check if the main class is an instance of the IModule class
      if (!IModule.class.isAssignableFrom(mainModuleClass)) {
        throw new AssertionError(String.format("Module main class %s is not assignable from %s",
          mainModuleClass.getCanonicalName(), IModule.class.getCanonicalName()));
      }
      // get the data directory of the module
      var dataDirectory = moduleConfiguration.dataFolder(this.moduleDirectory);
      // create an instance of the class and the main module wrapper
      var moduleInstance = (IModule) mainModuleClass.getConstructor().newInstance();
      IModuleWrapper moduleWrapper = new DefaultModuleWrapper(url, moduleInstance, dataDirectory,
        this, loader, dependencies.second(), moduleConfiguration);
      // initialize the module instance now
      moduleInstance.init(loader, moduleWrapper, moduleConfiguration);
      // register the module, load it and return the created wrapper
      this.modules.add(moduleWrapper);
      return moduleWrapper.loadModule();
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
  public IModuleWrapper loadModule(@NotNull Path path) {
    try {
      return this.loadModule(Preconditions.checkNotNull(path, "path").toUri().toURL());
    } catch (MalformedURLException exception) {
      LOGGER.severe("Unable to resolve url of module path", exception);
      return null;
    }
  }

  @Override
  public @NotNull IModuleProvider loadAll() {
    FileUtils.walkFileTree(
      this.moduleDirectory,
      ($, current) -> this.loadModule(current),
      false,
      "*.{jar,war,zip}");
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull IModuleProvider startAll() {
    for (var module : this.modules) {
      module.startModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull IModuleProvider reloadAll() {
    for (var module : this.modules) {
      module.reloadModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull IModuleProvider stopAll() {
    for (var module : this.modules) {
      module.stopModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull IModuleProvider unloadAll() {
    for (var module : this.modules) {
      module.unloadModule();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean notifyPreModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle) {
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
  public void notifyPostModuleLifecycleChange(@NotNull IModuleWrapper wrapper, @NotNull ModuleLifeCycle lifeCycle) {
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
   * Finds the module.json file in the provided {@code moduleFile} and deserializes it.
   *
   * @param moduleFile the module file to find the module configuration of.
   * @return the deserialized module configuration file located in the provided {@code moduleFile}.
   * @throws IOException if an I/O or deserialize exception occurs.
   */
  protected @NotNull Optional<ModuleConfiguration> findModuleConfiguration(@NotNull URL moduleFile) throws IOException {
    try (
      InputStream buffered = new BufferedInputStream(moduleFile.openStream());
      var inputStream = new JarInputStream(buffered)
    ) {
      JarEntry entry;
      while ((entry = inputStream.getNextJarEntry()) != null) {
        if (entry.getName().equals("module.json")) {
          var serializedModuleConfiguration = JsonDocument.newDocument(inputStream);
          return Optional.of(serializedModuleConfiguration.toInstanceOf(ModuleConfiguration.class));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Finds a loaded module based on the given {@code fileSource} url.
   *
   * @param fileSource the source to find the associated module with.
   * @return the associated module to the given url.
   * @throws URISyntaxException if the given URL is not formatted strictly according to RFC2396.
   */
  protected @NotNull Optional<IModuleWrapper> findModuleBySource(@NotNull URL fileSource) throws URISyntaxException {
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
   */
  protected @NotNull Map<String, String> collectModuleProvidedRepositories(@NotNull ModuleConfiguration configuration) {
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
   * @throws AssertionError If one dependency can't be loaded.
   */
  protected @NotNull Pair<Set<URL>, Set<ModuleDependency>> loadDependencies(
    @NotNull Map<String, String> repos,
    @NotNull ModuleConfiguration configuration
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
        // decide which way to go (by url or repository). In this case we start with the more common repository way
        if (dependency.repo() != null) {
          var repoUrl = Preconditions.checkNotNull(
            repos.get(dependency.repo()),
            "Dependency %s declared unknown repository %s as it's source",
            dependency.toString(), dependency.repo());
          loadedDependencies.add(this.doLoadDependency(dependency, configuration, handler,
            () -> this.moduleDependencyLoader.loadModuleDependencyByRepository(configuration, dependency, repoUrl)));
          continue;
        }
        // check if the repository defined a fixed download url for us to use
        if (dependency.url() != null) {
          loadedDependencies.add(this.doLoadDependency(dependency, configuration, handler,
            () -> this.moduleDependencyLoader.loadModuleDependencyByUrl(configuration, dependency)));
          continue;
        }
        // the dependency might be a dependency for a module, save this
        pendingModuleDependencies.add(dependency);
      }
    }
    // combine and return the result of the load
    return new Pair<>(loadedDependencies, pendingModuleDependencies);
  }

  /**
   * Tries to load a module dependency. This method is fail-fast.
   *
   * @param dependency    the dependency to load.
   * @param configuration the configuration from which the dependency was declared.
   * @param handler       the provider handler if one is set and should be notified, else {@code null}.
   * @param loader        the callback which will load the dependency.
   * @return the location of the loaded dependency in url form.
   * @throws AssertionError if loader fails to load the dependency.
   */
  protected @NotNull URL doLoadDependency(
    @NotNull ModuleDependency dependency,
    @NotNull ModuleConfiguration configuration,
    @Nullable IModuleProviderHandler handler,
    @NotNull Callable<URL> loader
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
