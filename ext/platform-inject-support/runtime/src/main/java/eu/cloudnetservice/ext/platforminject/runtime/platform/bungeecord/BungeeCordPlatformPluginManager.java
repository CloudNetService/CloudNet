/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.runtime.platform.bungeecord;

import static eu.cloudnetservice.driver.inject.InjectUtil.createFixedBinding;
import static eu.cloudnetservice.ext.platforminject.runtime.util.BindingUtil.fixedBindingWithBound;

import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;

public final class BungeeCordPlatformPluginManager extends BasePlatformPluginManager<String, Plugin> {

  public BungeeCordPlatformPluginManager() {
    super(plugin -> plugin.getDescription().getName(), FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<SpecifiedInjector> createInjectionLayer(@NonNull Plugin platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", (layer, injector) -> {
      // install bindings for the platform
      layer.install(createFixedBinding(platformData.getProxy(), ProxyServer.class));
      layer.install(createFixedBinding(platformData.getProxy().getConfig(), ProxyConfig.class));
      layer.install(createFixedBinding(platformData.getProxy().getScheduler(), TaskScheduler.class));
      layer.install(createFixedBinding(platformData.getProxy().getPluginManager(), PluginManager.class));

      // install the bindings which are specific to the plugin
      injector.installSpecified(fixedBindingWithBound(platformData, Plugin.class));
    });
  }
}
