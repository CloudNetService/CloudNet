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

package eu.cloudnetservice.driver.impl.module;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.auto.Provides;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.util.Qualifiers;
import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.Module;
import eu.cloudnetservice.driver.module.ModuleConfiguration;
import eu.cloudnetservice.driver.module.ModuleConfigurationNotFoundException;
import eu.cloudnetservice.driver.module.ModuleDependency;
import eu.cloudnetservice.driver.module.ModuleDependencyLoader;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleProviderHandler;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.utils.base.io.FileUtil;
import io.vavr.Tuple2;
import jakarta.inject.Singleton;
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
      // check if there is any other module loaded from the same url
      if (this.findModuleBySource(url).isPresent()) {
        return null;
      }
      // check if we can load the module configuration from the file
      var moduleConfiguration = this.findModuleConfiguration(url).orElse(null);
      if (moduleConfiguration == null) {
        throw new ModuleConfigurationNotFoundException(url);
      }
      // check if the module can run on the current java version release.
      if (!moduleConfiguration.canRunOn(JavaVersion.runtimeVersion())) {
        LOGGER.warn(
          "Unable to load module {}:{} because it only supports Java {}+",
          moduleConfiguration.group(),
          moduleConfiguration.name(),
          moduleConfiguration.minJavaVersionId());
        return null;
      }

      // get the data directory of the module
      var dataDirectory = moduleConfiguration.dataFolder(this.moduleDirectory);

      // create the injection layer for the module
      var externalLayer = InjectionLayer.ext();
      var moduleLayer = InjectionLayer.specifiedChild(externalLayer, "module", (layer, injector) -> {
        injector.installSpecified(BindingBuilder.create()
          .bind(DATA_DIRECTORY_ELEMENT)
          .toInstance(dataDirectory));
        injector.installSpecified(BindingBuilder.create()
          .bind(MODULE_CONFIGURATION_ELEMENT)
          .toInstance(moduleConfiguration));
      });

      // initialize all dependencies of the module
      var repositories = this.collectModuleProvidedRepositories(moduleConfiguration);
      var dependencies = this.loadDependencies(repositories, moduleConfiguration);
      // create the class loader for the module
      var loader = new ModuleURLClassLoader(url, dependencies._1(), moduleLayer);
      loader.registerGlobally();
      // try to load and create the main class instance
      var mainModuleClass = loader.loadClass(moduleConfiguration.main());
      // check if the main class is an instance of the IModule class
      if (!Module.class.isAssignableFrom(mainModuleClass)) {
        throw new AssertionError(String.format("Module main class %s is not assignable from %s",
          mainModuleClass.getCanonicalName(), Module.class.getCanonicalName()));
      }

      // create an instance of the class and the main module wrapper
      var moduleInstance = (Module) moduleLayer.instance(mainModuleClass);
      var moduleWrapper = new DefaultModuleWrapper(url, moduleInstance, dataDirectory,
        this, loader, dependencies._2(), moduleConfiguration, moduleLayer);
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
  public @Nullable ModuleWrapper loadModule(@NonNull Path path) {
    try {
      return this.loadModule(path.toUri().toURL());
    } catch (MalformedURLException exception) {
      LOGGER.error("Unable to resolve url of module path", exception);
      return null;
    }
  }

  @Override
  public @NonNull ModuleProvider loadAll() {
    FileUtil.walkFileTree(
      this.moduleDirectory,
      ($, current) -> this.loadModule(current),
      false,
      "*.{jar,war}");
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
