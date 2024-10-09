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

package eu.cloudnetservice.ext.platforminject.runtime.platform.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.mapping.PlatformedContainer;
import lombok.NonNull;

final class VelocityPlatformPluginManager
  extends BasePlatformPluginManager<String, PlatformedContainer<PluginContainer, ProxyServer>> {

  public VelocityPlatformPluginManager() {
    super(mapping -> mapping.container().getDescription().getId(), PlatformedContainer::pluginInstance);
  }

  @Override
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(
    @NonNull PlatformedContainer<PluginContainer, ProxyServer> platformData
  ) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", targetedBuilder -> {
      // install bindings for the platform
      var layer = BASE_INJECTION_LAYER;
      var bindingBuilder = layer.injector().createBindingBuilder();
      var proxyServer = platformData.platformInstance();
      layer.install(bindingBuilder.bind(ProxyServer.class).toInstance(proxyServer));
      layer.install(bindingBuilder.bind(Scheduler.class).toInstance(proxyServer.getScheduler()));
      layer.install(bindingBuilder.bind(EventManager.class).toInstance(proxyServer.getEventManager()));
      layer.install(bindingBuilder.bind(PluginManager.class).toInstance(proxyServer.getPluginManager()));
      layer.install(bindingBuilder.bind(CommandManager.class).toInstance(proxyServer.getCommandManager()));
      layer.install(bindingBuilder.bind(ChannelRegistrar.class).toInstance(proxyServer.getChannelRegistrar()));

      // install the bindings which are specific to the plugin
      targetedBuilder.installBinding(bindingBuilder.bind(PluginContainer.class).toInstance(platformData.container()));
      targetedBuilder.installBinding(bindingBuilder.bind(Object.class)
        .qualifiedWithName("plugin")
        .toInstance(platformData.pluginInstance()));
    });
  }
}
