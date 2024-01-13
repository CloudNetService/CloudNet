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

package eu.cloudnetservice.ext.platforminject.runtime.platform.nukkit;

import static eu.cloudnetservice.driver.inject.InjectUtil.createFixedBinding;
import static eu.cloudnetservice.ext.platforminject.runtime.util.BindingUtil.fixedBindingWithBound;

import cn.nukkit.Server;
import cn.nukkit.command.CommandMap;
import cn.nukkit.inventory.CraftingManager;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.plugin.service.ServiceManager;
import cn.nukkit.resourcepacks.ResourcePackManager;
import cn.nukkit.scheduler.ServerScheduler;
import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;

public final class NukkitPlatformPluginManager extends BasePlatformPluginManager<String, PluginBase> {

  public NukkitPlatformPluginManager() {
    super(Plugin::getName, FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<SpecifiedInjector> createInjectionLayer(@NonNull PluginBase platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", (layer, injector) -> {
      // install bindings for the platform
      layer.install(createFixedBinding(platformData.getServer(), Server.class));
      layer.install(createFixedBinding(platformData.getServer().getCommandMap(), CommandMap.class));
      layer.install(createFixedBinding(platformData.getServer().getScheduler(), ServerScheduler.class));
      layer.install(createFixedBinding(platformData.getServer().getPluginManager(), PluginManager.class));
      layer.install(createFixedBinding(platformData.getServer().getServiceManager(), ServiceManager.class));
      layer.install(createFixedBinding(platformData.getServer().getCraftingManager(), CraftingManager.class));
      layer.install(createFixedBinding(platformData.getServer().getResourcePackManager(), ResourcePackManager.class));

      // install the bindings which are specific to the plugin
      injector.installSpecified(fixedBindingWithBound(platformData, PluginBase.class, Plugin.class));
    });
  }
}
