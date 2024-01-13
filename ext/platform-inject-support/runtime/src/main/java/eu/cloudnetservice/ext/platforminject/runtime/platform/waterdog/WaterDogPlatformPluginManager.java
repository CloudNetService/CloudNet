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

package eu.cloudnetservice.ext.platforminject.runtime.platform.waterdog;

import static eu.cloudnetservice.driver.inject.InjectUtil.createFixedBinding;
import static eu.cloudnetservice.ext.platforminject.runtime.util.BindingUtil.fixedBindingWithBound;

import dev.derklaro.aerogel.SpecifiedInjector;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.CommandMap;
import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.logger.MainLogger;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfoMap;
import dev.waterdog.waterdogpe.packs.PackManager;
import dev.waterdog.waterdogpe.player.PlayerManager;
import dev.waterdog.waterdogpe.plugin.Plugin;
import dev.waterdog.waterdogpe.plugin.PluginManager;
import dev.waterdog.waterdogpe.scheduler.WaterdogScheduler;
import dev.waterdog.waterdogpe.utils.ConfigurationManager;
import dev.waterdog.waterdogpe.utils.config.LangConfig;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;

final class WaterDogPlatformPluginManager extends BasePlatformPluginManager<String, Plugin> {

  public WaterDogPlatformPluginManager() {
    super(Plugin::getName, FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<SpecifiedInjector> createInjectionLayer(@NonNull Plugin platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", (layer, injector) -> {
      // install bindings for the platform
      layer.install(createFixedBinding(platformData.getProxy(), ProxyServer.class));
      layer.install(createFixedBinding(platformData.getProxy().getLogger(), MainLogger.class));
      layer.install(createFixedBinding(platformData.getProxy().getCommandMap(), CommandMap.class));
      layer.install(createFixedBinding(platformData.getProxy().getPackManager(), PackManager.class));
      layer.install(createFixedBinding(platformData.getProxy().getLanguageConfig(), LangConfig.class));
      layer.install(createFixedBinding(platformData.getProxy().getEventManager(), EventManager.class));
      layer.install(createFixedBinding(platformData.getProxy().getPlayerManager(), PlayerManager.class));
      layer.install(createFixedBinding(platformData.getProxy().getServerInfoMap(), ServerInfoMap.class));
      layer.install(createFixedBinding(platformData.getProxy().getPluginManager(), PluginManager.class));
      layer.install(createFixedBinding(platformData.getProxy().getScheduler(), WaterdogScheduler.class));
      layer.install(createFixedBinding(platformData.getProxy().getConfigurationManager(), ConfigurationManager.class));

      // install the bindings which are specific to the plugin
      injector.installSpecified(fixedBindingWithBound(platformData, Plugin.class));
    });
  }
}
