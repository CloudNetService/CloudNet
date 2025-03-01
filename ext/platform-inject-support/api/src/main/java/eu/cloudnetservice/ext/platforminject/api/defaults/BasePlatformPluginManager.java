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

package eu.cloudnetservice.ext.platforminject.api.defaults;

import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.PlatformPluginInfo;
import eu.cloudnetservice.ext.platforminject.api.PlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.inject.BindingsInstaller;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class BasePlatformPluginManager<I, T> implements PlatformPluginManager<I, T> {

  protected static final InjectionLayer<Injector> BASE_INJECTION_LAYER = InjectionLayer.ext();
  protected static final StackWalker RETAINING_STACK_WALKER = StackWalker.getInstance(
    StackWalker.Option.RETAIN_CLASS_REFERENCE);

  private final Function<T, I> idExtractor;
  private final Function<T, Object> mainClassExtractor;

  private final Map<I, PlatformPluginInfo<I, T, ?>> constructedPlugins = new ConcurrentHashMap<>(16, 0.9f, 1);

  protected BasePlatformPluginManager(
    @NonNull Function<T, I> idExtractor,
    @NonNull Function<T, Object> mainClassExtractor
  ) {
    this.idExtractor = idExtractor;
    this.mainClassExtractor = mainClassExtractor;
  }

  @Override
  public @NonNull Collection<PlatformPluginInfo<I, T, ?>> loadedPlugins() {
    return this.constructedPlugins.values();
  }

  @Override
  public @Nullable PlatformPluginInfo<I, T, ?> loadedPlugin(@NonNull T platformData) {
    var id = this.idExtractor.apply(platformData);
    return id == null ? null : this.loadedPluginById(id);
  }

  @Override
  public @Nullable PlatformPluginInfo<I, T, ?> loadedPluginById(@NonNull I id) {
    return this.constructedPlugins.get(id);
  }

  @Override
  public void constructAndLoad(@NonNull Class<? extends PlatformEntrypoint> pluginClass, @NonNull T platformData) {
    // check if the plugin was constructed already
    var pluginId = this.idExtractor.apply(platformData);
    if (pluginId == null || this.constructedPlugins.containsKey(pluginId)) {
      return;
    }

    // create the plugin injection layer
    var pluginLayer = this.createInjectionLayer(platformData);

    // configure the injection layer
    var callerClass = RETAINING_STACK_WALKER.walk(stream -> stream
      .skip(2)
      .map(StackWalker.StackFrame::getDeclaringClass)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Unable to resolve calling platform main class")));
    this.configureInjectionLayer(pluginLayer, callerClass);

    // bind the layer to the plugin main class & main class loader
    var platformMainInstance = this.mainClassExtractor.apply(platformData);
    pluginLayer.register(platformMainInstance, platformMainInstance.getClass().getClassLoader());

    // construct the platform main class instance
    var pluginInstance = pluginLayer.instance(pluginClass);

    // construct the plugin info
    var pluginInfo = new DefaultPlatformPluginInfo<>(pluginId, platformData, pluginLayer, pluginInstance, pluginClass);

    // register the plugin and load if we were the first to create an instance of the plugin
    var presentInstance = this.constructedPlugins.putIfAbsent(pluginId, pluginInfo);
    if (presentInstance == null) {
      pluginInstance.onLoad();
    }
  }

  @Override
  public void disablePlugin(@NonNull T platformData) {
    var pluginId = this.idExtractor.apply(platformData);
    if (pluginId != null) {
      this.disablePluginById(pluginId);
    }
  }

  @Override
  public void disablePluginById(@NonNull I id) {
    var pluginInfo = this.constructedPlugins.remove(id);
    if (pluginInfo != null) {
      pluginInfo.close();
    }
  }

  protected void configureInjectionLayer(@NonNull InjectionLayer<?> layer, @NonNull Class<?> platformMainClass) {
    try {
      // kinda hacky, but it works...
      var bindingsClassName = platformMainClass.getName().replaceFirst("(?s)(.*)Entrypoint", "$1Bindings");
      var bindingsClass = Class.forName(bindingsClassName, false, platformMainClass.getClassLoader());

      // get the no-args constructor, no need for accessible or something, the class & constructor must be public
      var constructor = bindingsClass.getConstructor();
      var installerInstance = constructor.newInstance();

      // check if the bindings class is actually an installer, ignore in all other cases
      if (installerInstance instanceof BindingsInstaller installer) {
        installer.applyBindings(layer);
      }
    } catch (Exception ignored) {
    }
  }

  protected abstract @NonNull InjectionLayer<Injector> createInjectionLayer(@NonNull T platformData);
}
