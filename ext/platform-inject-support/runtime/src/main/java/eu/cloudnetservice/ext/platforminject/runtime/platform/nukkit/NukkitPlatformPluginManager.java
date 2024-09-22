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
import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;

public final class NukkitPlatformPluginManager extends BasePlatformPluginManager<String, PluginBase> {

  public NukkitPlatformPluginManager() {
    super(Plugin::getName, FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(@NonNull PluginBase platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", targetedBuilder -> {
      // install bindings for the platform
      var layer = BASE_INJECTION_LAYER;
      var builder = layer.injector().createBindingBuilder();
      layer.install(builder.bind(Server.class).toInstance(platformData.getServer()));
      layer.install(builder.bind(CommandMap.class).toInstance(platformData.getServer().getCommandMap()));
      layer.install(builder.bind(ServerScheduler.class).toInstance(platformData.getServer().getScheduler()));
      layer.install(builder.bind(PluginManager.class).toInstance(platformData.getServer().getPluginManager()));
      layer.install(builder.bind(ServiceManager.class).toInstance(platformData.getServer().getServiceManager()));
      layer.install(builder.bind(CraftingManager.class).toInstance(platformData.getServer().getCraftingManager()));
      layer.install(builder.bind(ResourcePackManager.class)
        .toInstance(platformData.getServer().getResourcePackManager()));

      // install the bindings which are specific to the plugin
      targetedBuilder.installBinding(fixedBindingWithBound(platformData, PluginBase.class, Plugin.class));
    });
  }
}
