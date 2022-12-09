/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.defaults;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.PlatformPlugin;
import eu.cloudnetservice.ext.platforminject.PlatformPluginInfo;
import eu.cloudnetservice.ext.platforminject.PlatformPluginManager;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class BasePlatformPluginManager<I, T> implements PlatformPluginManager<I, T> {

  protected static final InjectionLayer<Injector> BASE_INJECTION_LAYER = InjectionLayer.ext();

  private final Function<T, I> idExtractor;
  // todo: move this map construction to a util class?
  private final Map<I, PlatformPluginInfo<I, T, ?>> constructedPlugins = new ConcurrentHashMap<>(16, 0.9f, 1);

  protected BasePlatformPluginManager(@NonNull Function<T, I> idExtractor) {
    this.idExtractor = idExtractor;
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
  public void constructAndLoad(@NonNull Class<? extends PlatformPlugin> pluginClass, @NonNull T platformData) {
    // check if the plugin was constructed already
    var pluginId = this.idExtractor.apply(platformData);
    if (pluginId == null || this.constructedPlugins.containsKey(pluginId)) {
      return;
    }

    // create an injection layer for the plugin and construct the plugin instance
    var pluginLayer = this.createInjectionLayer(platformData);
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

  protected abstract @NonNull InjectionLayer<SpecifiedInjector> createInjectionLayer(@NonNull T platformData);
}
