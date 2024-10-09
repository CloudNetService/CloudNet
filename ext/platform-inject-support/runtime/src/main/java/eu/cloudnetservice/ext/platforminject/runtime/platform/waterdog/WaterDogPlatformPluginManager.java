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

import dev.derklaro.aerogel.Injector;
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
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(@NonNull Plugin platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", targetedBuilder -> {
      // install bindings for the platform
      var layer = BASE_INJECTION_LAYER;
      var bindingBuilder = layer.injector().createBindingBuilder();
      layer.install(bindingBuilder.bind(ProxyServer.class).toInstance(platformData.getProxy()));
      layer.install(bindingBuilder.bind(MainLogger.class).toInstance(platformData.getProxy().getLogger()));
      layer.install(bindingBuilder.bind(CommandMap.class).toInstance(platformData.getProxy().getCommandMap()));
      layer.install(bindingBuilder.bind(PackManager.class).toInstance(platformData.getProxy().getPackManager()));
      layer.install(bindingBuilder.bind(LangConfig.class).toInstance(platformData.getProxy().getLanguageConfig()));
      layer.install(bindingBuilder.bind(EventManager.class).toInstance(platformData.getProxy().getEventManager()));
      layer.install(bindingBuilder.bind(PlayerManager.class).toInstance(platformData.getProxy().getPlayerManager()));
      layer.install(bindingBuilder.bind(ServerInfoMap.class).toInstance(platformData.getProxy().getServerInfoMap()));
      layer.install(bindingBuilder.bind(PluginManager.class).toInstance(platformData.getProxy().getPluginManager()));
      layer.install(bindingBuilder.bind(WaterdogScheduler.class).toInstance(platformData.getProxy().getScheduler()));
      layer.install(bindingBuilder.bind(ConfigurationManager.class)
        .toInstance(platformData.getProxy().getConfigurationManager()));

      // install the bindings which are specific to the plugin
      targetedBuilder.installBinding(bindingBuilder.bind(Plugin.class).toInstance(platformData));
    });
  }
}
