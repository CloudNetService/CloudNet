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

package eu.cloudnetservice.ext.platforminject.runtime.platform.sponge;

import dev.derklaro.aerogel.Injector;
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
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(
    @NonNull Container<PluginContainer> platformData
  ) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", targetedBuilder -> {
      // scheduler bindings
      var layer = BASE_INJECTION_LAYER;
      var bindingBuilder = layer.injector().createBindingBuilder();
      layer.install(bindingBuilder
        .bind(Scheduler.class)
        .qualifiedWithName("sync")
        .toInstance(Sponge.server().scheduler()));
      layer.install(bindingBuilder
        .bind(Scheduler.class)
        .qualifiedWithName("async")
        .toInstance(Sponge.game().asyncScheduler()));

      // game bindings
      layer.install(bindingBuilder.bind(Game.class).toInstance(Sponge.game()));
      layer.install(bindingBuilder.bind(Platform.class).toInstance(Sponge.game().platform()));
      layer.install(bindingBuilder.bind(DataManager.class).toInstance(Sponge.game().dataManager()));
      layer.install(bindingBuilder.bind(EventManager.class).toInstance(Sponge.game().eventManager()));
      layer.install(bindingBuilder.bind(ConfigManager.class).toInstance(Sponge.game().configManager()));
      layer.install(bindingBuilder.bind(PluginManager.class).toInstance(Sponge.game().pluginManager()));
      layer.install(bindingBuilder.bind(ChannelManager.class).toInstance(Sponge.game().channelManager()));
      layer.install(bindingBuilder.bind(BuilderProvider.class).toInstance(Sponge.game().builderProvider()));
      layer.install(bindingBuilder.bind(FactoryProvider.class).toInstance(Sponge.game().factoryProvider()));
      layer.install(bindingBuilder.bind(MetricsConfigManager.class).toInstance(Sponge.game().metricsConfigManager()));
      layer.install(bindingBuilder.bind(ServiceProvider.GameScoped.class).toInstance(Sponge.game().serviceProvider()));

      // server bindings
      var server = Sponge.server();
      layer.install(bindingBuilder.bind(Server.class).toInstance(server));
      layer.install(bindingBuilder.bind(RegistryHolder.class).toInstance(server));
      layer.install(bindingBuilder.bind(MapStorage.class).toInstance(server.mapStorage()));
      layer.install(bindingBuilder.bind(UserManager.class).toInstance(server.userManager()));
      layer.install(bindingBuilder.bind(WorldManager.class).toInstance(server.worldManager()));
      layer.install(bindingBuilder.bind(RecipeManager.class).toInstance(server.recipeManager()));
      layer.install(bindingBuilder.bind(TeleportHelper.class).toInstance(server.teleportHelper()));
      layer.install(bindingBuilder.bind(CommandManager.class).toInstance(server.commandManager()));
      layer.install(bindingBuilder.bind(PackRepository.class).toInstance(server.packRepository()));
      layer.install(bindingBuilder.bind(ResourceManager.class).toInstance(server.resourceManager()));
      layer.install(bindingBuilder.bind(CauseStackManager.class).toInstance(server.causeStackManager()));
      layer.install(bindingBuilder.bind(GameProfileManager.class).toInstance(server.gameProfileManager()));
      layer.install(bindingBuilder.bind(GameProfileProvider.class).toInstance(server.gameProfileManager()));
      layer.install(bindingBuilder.bind(ServiceProvider.class).toInstance(server.serviceProvider()));
      layer.install(bindingBuilder.bind(ServiceProvider.ServerScoped.class).toInstance(server.serviceProvider()));

      // install the bindings which are specific to the plugin
      targetedBuilder.installBinding(bindingBuilder
        .bind(Object.class)
        .qualifiedWithName("plugin")
        .toInstance(platformData.pluginInstance()));
      targetedBuilder.installBinding(bindingBuilder
        .bind(PluginContainer.class)
        .toInstance(platformData.container()));
    });
  }
}
