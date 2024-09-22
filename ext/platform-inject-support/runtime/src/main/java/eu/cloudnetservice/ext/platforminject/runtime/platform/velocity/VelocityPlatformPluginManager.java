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

import static eu.cloudnetservice.driver.inject.InjectUtil.createFixedBinding;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.util.Qualifiers;
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
  protected @NonNull InjectionLayer<SpecifiedInjector> createInjectionLayer(
    @NonNull PlatformedContainer<PluginContainer, ProxyServer> platformData
  ) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", (layer, injector) -> {
      // install bindings for the platform
      layer.install(bindingBuilder.bind(ProxyServer.class).toInstance(platformData.platformInstance()));
      layer.install(bindingBuilder.bind(Scheduler.class).toInstance(platformData.platformInstance().getScheduler()));
      layer.install(bindingBuilder.bind(EventManager.class).toInstance(platformData.platformInstance().getEventManager()));
      layer.install(bindingBuilder.bind(PluginManager.class).toInstance(platformData.platformInstance().getPluginManager()));
      layer.install(bindingBuilder.bind(CommandManager.class).toInstance(platformData.platformInstance().getCommandManager()));
      layer.install(bindingBuilder.bind(ChannelRegistrar.class).toInstance(platformData.platformInstance().getChannelRegistrar()));

      // install the bindings which are specific to the plugin
      injector.installSpecified(createFixedBinding(platformData.container(), PluginContainer.class));
      injector.installSpecified(BindingBuilder.create()
        .bind(Element.forType(Object.class).requireAnnotation(Qualifiers.named("plugin")))
        .toInstance(platformData.pluginInstance()));
    });
  }
}
