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
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

public class DefaultModuleWrapper implements IModuleWrapper {

  private static final Type MODULE_CONFIGURATION_TYPE = new TypeToken<ModuleConfiguration>() {
  }.getType();

  private static final Map<String, String> DEFAULT_REPOSITORIES = Collections
    .singletonMap("maven", "https://repo1.maven.org/maven2/");

  private static final String MODULE_CONFIG_PATH = "module.json";
  private final EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> moduleTasks = new EnumMap<>(ModuleLifeCycle.class);
  private final URL url;
  private final DefaultModuleProvider moduleProvider;
  private ModuleLifeCycle moduleLifeCycle = ModuleLifeCycle.UNLOADED;
  private DefaultModule module;
  private FinalizeURLClassLoader classLoader;
  private ModuleConfiguration moduleConfiguration;
  private JsonDocument moduleConfigurationSource;
  private Path moduleDirectory = Paths.get("modules");

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

  @Deprecated
  public DefaultModuleWrapper(DefaultModuleProvider moduleProvider, URL url, File moduleDirectory) throws Exception {
    this(moduleProvider, url, moduleDirectory.toPath());
  }

  public DefaultModuleWrapper(DefaultModuleProvider moduleProvider, URL url, Path moduleDirectory) throws Exception {
    this(moduleProvider, url);
    this.moduleDirectory = moduleDirectory;
  }

  private void init(URL url) throws Exception {
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

    Map<String, String> repositories = new HashMap<>(DEFAULT_REPOSITORIES);

    List<URL> urls = new ArrayList<>();
    urls.add(url);

    if (this.moduleConfiguration.getRepos() != null) {
      for (ModuleRepository moduleRepository : this.moduleConfiguration.getRepos()) {
        if (moduleRepository.getName() != null && moduleRepository.getUrl() != null) {
          repositories.put(moduleRepository.getName(),
            moduleRepository.getUrl().endsWith("/") ? moduleRepository.getUrl() : moduleRepository.getUrl() + "/");
        }
      }
    }

    if (this.moduleConfiguration.getDependencies() != null) {
      for (ModuleDependency moduleDependency : this.moduleConfiguration.getDependencies()) {
        if (moduleDependency.getGroup() != null && moduleDependency.getName() != null
          && moduleDependency.getVersion() != null) {
          if (moduleDependency.getUrl() != null) {
            if (this.moduleProvider.getModuleProviderHandler() != null) {
              this.moduleProvider.getModuleProviderHandler().handlePreInstallDependency(this, moduleDependency);
            }

            urls.add(this.moduleProvider.getModuleDependencyLoader()
              .loadModuleDependencyByUrl(this.moduleConfiguration, moduleDependency, repositories));

            if (this.moduleProvider.getModuleProviderHandler() != null) {
              this.moduleProvider.getModuleProviderHandler().handlePostInstallDependency(this, moduleDependency);
            }
            continue;
          }

          if (moduleDependency.getRepo() != null && repositories.containsKey(moduleDependency.getRepo())) {
            if (this.moduleProvider.getModuleProviderHandler() != null) {
              this.moduleProvider.getModuleProviderHandler().handlePreInstallDependency(this, moduleDependency);
            }

            urls.add(this.moduleProvider.getModuleDependencyLoader()
              .loadModuleDependencyByRepository(this.moduleConfiguration, moduleDependency, repositories));

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

  @Override
  public EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> getModuleTasks() {
    return new EnumMap<>(this.moduleTasks);
  }

  @Override
  public IModuleWrapper loadModule() {
    if (this.moduleLifeCycle == ModuleLifeCycle.UNLOADED && this.moduleProvider.getModuleProviderHandler()
      .handlePreModuleLoad(this)) {
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

            IModuleWrapper moduleWrapper = this.getModuleProvider().getModules().stream()
              .filter(module -> module.getModuleConfiguration().getGroup().equals(moduleDependency.getGroup()) &&
                module.getModuleConfiguration().getName().equals(moduleDependency.getName())).findFirst().orElse(null);

            if (moduleWrapper != null) {
              moduleWrapper.startModule();
            } else {
              new ModuleDependencyNotFoundException(
                "Module Dependency for " + moduleDependency.getGroup() + ":" + moduleDependency.getName() +
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
  public @NotNull Path getDataDirectory() {
    return this.getModuleConfigurationSource() != null && this.getModuleConfigurationSource().contains("dataFolder")
      ? Paths.get(this.getModuleConfigurationSource().getString("dataFolder"))
      : this.moduleDirectory.resolve(this.getModuleConfiguration().getName());
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
