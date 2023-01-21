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

package eu.cloudnetservice.ext.platforminject.runtime.platform.sponge;

import static eu.cloudnetservice.driver.inject.InjectUtil.createFixedBinding;
import static eu.cloudnetservice.ext.platforminject.runtime.util.BindingUtil.fixedBindingWithBound;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.util.Qualifiers;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.mapping.Container;
import lombok.NonNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.item.recipe.RecipeManager;
import org.spongepowered.api.map.MapStorage;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.profile.GameProfileProvider;
import org.spongepowered.api.registry.BuilderProvider;
import org.spongepowered.api.registry.FactoryProvider;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.resource.ResourceManager;
import org.spongepowered.api.resource.pack.PackRepository;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.service.ServiceProvider;
import org.spongepowered.api.sql.SqlManager;
import org.spongepowered.api.user.UserManager;
import org.spongepowered.api.util.metric.MetricsConfigManager;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.api.world.teleport.TeleportHelper;
import org.spongepowered.plugin.PluginContainer;

public final class SpongePlatformPluginManager extends BasePlatformPluginManager<String, Container<PluginContainer>> {

  public SpongePlatformPluginManager() {
    super(mapping -> mapping.container().metadata().id(), Container::pluginInstance);
  }

  @Override
  protected @NonNull InjectionLayer<SpecifiedInjector> createInjectionLayer(
    @NonNull Container<PluginContainer> platformData
  ) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", (layer, injector) -> {
      // scheduler bindings
      var schedulerElement = Element.forType(Scheduler.class);
      layer.install(BindingBuilder.create()
        .bind(schedulerElement.requireAnnotation(Qualifiers.named("sync")))
        .toInstance(Sponge.server().scheduler()));
      layer.install(BindingBuilder.create()
        .bind(schedulerElement.requireAnnotation(Qualifiers.named("async")))
        .toInstance(Sponge.game().asyncScheduler()));

      // game bindings
      layer.install(createFixedBinding(Sponge.game(), Game.class));
      layer.install(createFixedBinding(Sponge.game().platform(), Platform.class));
      layer.install(createFixedBinding(Sponge.game().sqlManager(), SqlManager.class));
      layer.install(createFixedBinding(Sponge.game().dataManager(), DataManager.class));
      layer.install(createFixedBinding(Sponge.game().eventManager(), EventManager.class));
      layer.install(createFixedBinding(Sponge.game().configManager(), ConfigManager.class));
      layer.install(createFixedBinding(Sponge.game().pluginManager(), PluginManager.class));
      layer.install(createFixedBinding(Sponge.game().channelManager(), ChannelManager.class));
      layer.install(createFixedBinding(Sponge.game().builderProvider(), BuilderProvider.class));
      layer.install(createFixedBinding(Sponge.game().factoryProvider(), FactoryProvider.class));
      layer.install(createFixedBinding(Sponge.game().metricsConfigManager(), MetricsConfigManager.class));
      layer.install(createFixedBinding(Sponge.game().serviceProvider(), ServiceProvider.GameScoped.class));

      // server bindings
      layer.install(createFixedBinding(Sponge.server().mapStorage(), MapStorage.class));
      layer.install(createFixedBinding(Sponge.server().userManager(), UserManager.class));
      layer.install(createFixedBinding(Sponge.server().worldManager(), WorldManager.class));
      layer.install(createFixedBinding(Sponge.server(), Server.class, RegistryHolder.class));
      layer.install(createFixedBinding(Sponge.server().recipeManager(), RecipeManager.class));
      layer.install(createFixedBinding(Sponge.server().teleportHelper(), TeleportHelper.class));
      layer.install(createFixedBinding(Sponge.server().commandManager(), CommandManager.class));
      layer.install(createFixedBinding(Sponge.server().packRepository(), PackRepository.class));
      layer.install(createFixedBinding(Sponge.server().resourceManager(), ResourceManager.class));
      layer.install(createFixedBinding(Sponge.server().causeStackManager(), CauseStackManager.class));
      layer.install(createFixedBinding(
        Sponge.server().gameProfileManager(),
        GameProfileManager.class, GameProfileProvider.class));
      layer.install(createFixedBinding(
        Sponge.server().serviceProvider(),
        ServiceProvider.ServerScoped.class, ServiceProvider.class));

      // install the bindings which are specific to the plugin
      injector.installSpecified(fixedBindingWithBound(platformData.pluginInstance()));
      injector.installSpecified(createFixedBinding(platformData.container(), PluginContainer.class));
    });
  }
}
