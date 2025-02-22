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

import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.Module;
import eu.cloudnetservice.driver.module.ModuleConfiguration;
import eu.cloudnetservice.driver.module.ModuleDependency;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.ModuleTaskEntry;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the default implementation of the module wrapper.
 *
 * @see ModuleWrapper
 * @since 4.0
 */
public class DefaultModuleWrapper implements ModuleWrapper {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultModuleWrapper.class);
  // This looks strange in the first place but is the only way to go as java generics are a bit strange.
  // When using Comparator.comparingInt(...).reverse() the type is now a Comparator<Object> which leads to problems
  // extracting the key of the task entry... And yes, reversing is necessary as the module task with the highest order
  // should be called first but the natural ordering of java sorts the lowest number first.
  protected static final Comparator<ModuleTaskEntry> TASK_COMPARATOR = Comparator.comparing(
    entry -> entry.taskInfo().order(), Comparator.reverseOrder());

  private final URL source;
  private final URI sourceUri;
  private final Module module;
  private final Path dataDirectory;
  private final ModuleProvider provider;
  private final URLClassLoader classLoader;
  private final Set<ModuleDependency> dependingModules;
  private final ModuleConfiguration moduleConfiguration;
  private final InjectionLayer<SpecifiedInjector> moduleInjectionLayer;

  private final Lock moduleLifecycleUpdateLock = new ReentrantLock();
  private final Map<ModuleLifeCycle, List<ModuleTaskEntry>> tasks = new EnumMap<>(ModuleLifeCycle.class);
  private final AtomicReference<ModuleLifeCycle> lifeCycle = new AtomicReference<>(ModuleLifeCycle.CREATED);

  /**
   * Creates a new instance of a default module wrapper.
   *
   * @param source               the module file from which this module was loaded initially.
   * @param module               the instance of the module main class constructed by the provider.
   * @param dataDirectory        the data directory of this module relative to the module provider directory.
   * @param provider             the provider which loaded this module.
   * @param classLoader          the class loader which was used to load the main class from the file.
   * @param dependingModules     the modules this module depends on and which need to get loaded first.
   * @param moduleConfiguration  the parsed module configuration located in the module file.
   * @param moduleInjectionLayer the injection layer of the module.
   * @throws URISyntaxException   if the given module source is not formatted strictly according to RFC2396.
   * @throws NullPointerException if one of the given arguments is null.
   */
  public DefaultModuleWrapper(
    @NonNull URL source,
    @NonNull Module module,
    @NonNull Path dataDirectory,
    @NonNull ModuleProvider provider,
    @NonNull URLClassLoader classLoader,
    @NonNull Set<ModuleDependency> dependingModules,
    @NonNull ModuleConfiguration moduleConfiguration,
    @NonNull InjectionLayer<SpecifiedInjector> moduleInjectionLayer
  ) throws URISyntaxException {
    this.source = source;
    this.module = module;
    this.dataDirectory = dataDirectory;
    this.provider = provider;
    this.classLoader = classLoader;
    this.dependingModules = dependingModules;
    this.moduleConfiguration = moduleConfiguration;
    this.moduleInjectionLayer = moduleInjectionLayer;
    // initialize the uri of the module now as it's always required in order for the default provider to work
    this.sourceUri = source.toURI();
    // resolve all tasks the module must execute now as we need them later anyway
    this.tasks.putAll(this.resolveModuleTasks(module));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<ModuleLifeCycle, List<ModuleTaskEntry>> moduleTasks() {
    return Collections.unmodifiableMap(this.tasks);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull @Unmodifiable Set<ModuleDependency> dependingModules() {
    return Collections.unmodifiableSet(this.dependingModules);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Module module() {
    return this.module;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleLifeCycle moduleLifeCycle() {
    return this.lifeCycle.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleProvider moduleProvider() {
    return this.provider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleConfiguration moduleConfiguration() {
    return this.moduleConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassLoader classLoader() {
    return this.classLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleWrapper loadModule() {
    this.pushLifecycleChange(ModuleLifeCycle.LOADED, true);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleWrapper startModule() {
    if (this.moduleLifeCycle().canChangeTo(ModuleLifeCycle.STARTED)
      && this.provider.notifyPreModuleLifecycleChange(this, ModuleLifeCycle.STARTED)) {
      // Resolve all dependencies of this module to start them before this module
      for (var wrapper : ModuleDependencyUtil.collectDependencies(this, this.provider)) {
        wrapper.startModule();
      }
      // now we can start this module
      this.pushLifecycleChange(ModuleLifeCycle.STARTED, false);
      // and we now need to notify the provider here
      this.provider.notifyPostModuleLifecycleChange(this, ModuleLifeCycle.STARTED);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleWrapper reloadModule() {
    // runtime modules are not reloadable
    if (this.moduleConfiguration.runtimeModule()) {
      return this;
    }

    if (this.moduleLifeCycle().canChangeTo(ModuleLifeCycle.RELOADING)
      && this.provider.notifyPreModuleLifecycleChange(this, ModuleLifeCycle.RELOADING)) {
      // Resolve all dependencies of this module to reload them before this module
      for (var wrapper : ModuleDependencyUtil.collectDependencies(this, this.provider)) {
        wrapper.reloadModule();
      }
      //now we can reload this module
      this.pushLifecycleChange(ModuleLifeCycle.RELOADING, false);
      // and we now need to notify the provider here
      this.provider.notifyPostModuleLifecycleChange(this, ModuleLifeCycle.RELOADING);
      //push the module back to started
      this.lifeCycle.set(ModuleLifeCycle.STARTED);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleWrapper stopModule() {
    this.pushLifecycleChange(ModuleLifeCycle.STOPPED, true);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleWrapper unloadModule() {
    if (this.moduleLifeCycle().canChangeTo(ModuleLifeCycle.UNLOADED)) {
      this.pushLifecycleChange(ModuleLifeCycle.UNLOADED, true);
      // remove all known module tasks & dependencies
      this.tasks.clear();
      this.dependingModules.clear();
      // close the class loader
      try {
        this.classLoader.close();
      } catch (IOException exception) {
        LOGGER.error("Exception closing class loader of module {}", this.moduleConfiguration.name(), exception);
      }
      // set the state to unusable
      this.lifeCycle.set(ModuleLifeCycle.UNUSABLE);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Path dataDirectory() {
    return this.dataDirectory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull URL url() {
    return this.source;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull URI uri() {
    return this.sourceUri;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull InjectionLayer<SpecifiedInjector> injectionLayer() {
    return this.moduleInjectionLayer;
  }

  /**
   * Resolves all module tasks in the main class of a module. Declared methods are working as well. The returned map
   * should only contain the tasks sorted as there will be no later step to sort them. Not all module lifecycle states
   * must be in the returned map, if a lifecycle is missing it will be assumed that there is no tasks for that
   * lifecycle.
   *
   * @param module the module instance (better known as the module main class) to resolve the tasks of.
   * @return all sorted resolved tasks mapped to the lifecycle they will be called in.
   */
  protected @NonNull Map<ModuleLifeCycle, List<ModuleTaskEntry>> resolveModuleTasks(@NonNull Module module) {
    Map<ModuleLifeCycle, List<ModuleTaskEntry>> result = new EnumMap<>(ModuleLifeCycle.class);
    // check all declared methods to get all methods of this and super classes
    for (var method : module.getClass().getDeclaredMethods()) {
      // check if this method is a method we need to register
      var moduleTask = method.getAnnotation(ModuleTask.class);
      if (moduleTask != null && !Modifier.isStatic(method.getModifiers())) {
        try {
          var entries = result.computeIfAbsent(moduleTask.lifecycle(), $ -> new ArrayList<>());
          entries.add(new DefaultModuleTaskEntry(this, moduleTask, method));
          // re-sort the list now as we don't want to re-iterate later
          entries.sort(TASK_COMPARATOR);
        } catch (IllegalAccessException exception) {
          // this should not happen as we had successfully overridden the java lang access flag earlier
          LOGGER.error("Unable to access module task entry to unreflect method", exception);
        }
      }
    }
    return result;
  }

  /**
   * Fires all handlers registered for the specified lifeCycle.
   *
   * @param lifeCycle      the lifecycle to fire the tasks of.
   * @param notifyProvider if the module provider should be notified about the change or not.
   */
  protected void pushLifecycleChange(@NonNull ModuleLifeCycle lifeCycle, boolean notifyProvider) {
    var tasks = this.tasks.get(lifeCycle);
    if (this.moduleLifeCycle().canChangeTo(lifeCycle)) {
      this.moduleLifecycleUpdateLock.lock();
      try {
        // notify the provider for changes which are required based on the lifecycle and other stuff (like to invoke
        // of the associated methods in the module provider handler)
        if (!notifyProvider || this.moduleProvider().notifyPreModuleLifecycleChange(this, lifeCycle)) {
          // The tasks are always in the logical order in the backing map, so there is no need to sort here
          if (tasks != null && !tasks.isEmpty()) {
            for (var task : tasks) {
              if (this.fireModuleTaskEntry(task)) {
                // we couldn't complete firing all tasks as one failed, so we break here and warn the user about that.
                LOGGER.warn(
                  "Stopping lifecycle update to {} for {} because the task {} failed. See console log for more details.",
                  lifeCycle,
                  this.moduleConfiguration.name(),
                  task.fullMethodSignature());
                return;
              }
            }
          }
          // actually set the current life cycle of this module
          this.lifeCycle.set(lifeCycle);
          // notify after the change again if we have to
          if (notifyProvider) {
            this.moduleProvider().notifyPostModuleLifecycleChange(this, lifeCycle);
          }
        }
      } finally {
        this.moduleLifecycleUpdateLock.unlock();
      }
    }
  }

  /**
   * Fires a specific module task entry.
   *
   * @param entry the entry to fire.
   * @return true if the entry couldn't be fired successfully, false otherwise.
   */
  protected boolean fireModuleTaskEntry(@NonNull ModuleTaskEntry entry) {
    try {
      entry.fire();
      return false;
    } catch (Throwable exception) {
      LOGGER.error("Exception firing module task entry {}", entry, exception);
      return true;
    }
  }
}
