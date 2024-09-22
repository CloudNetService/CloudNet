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

package eu.cloudnetservice.ext.platforminject.runtime.platform.minestom;

import static eu.cloudnetservice.ext.platforminject.runtime.util.BindingUtil.fixedBindingWithBound;

import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;
import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.advancements.AdvancementManager;
import net.minestom.server.adventure.bossbar.BossBarManager;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.exception.ExceptionManager;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extensions.ExtensionManager;
import net.minestom.server.gamedata.tags.TagManager;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.listener.manager.PacketListenerManager;
import net.minestom.server.monitoring.BenchmarkManager;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.PacketProcessor;
import net.minestom.server.recipe.RecipeManager;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.timer.SchedulerManager;

public final class MinestomPlatformPluginManager extends BasePlatformPluginManager<String, Extension> {

  public MinestomPlatformPluginManager() {
    super(extension -> extension.getOrigin().getName(), FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<SpecifiedInjector> createInjectionLayer(@NonNull Extension platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", (layer, injector) -> {
      // install bindings for the platform
      layer.install(bindingBuilder.bind(ServerProcess.class).toInstance(MinecraftServer.process()));
      layer.install(bindingBuilder.bind(ComponentLogger.class).toInstance(platformData.getLogger()));
      layer.install(bindingBuilder.bind(TagManager.class).toInstance(MinecraftServer.getTagManager()));
      layer.install(bindingBuilder.bind(TeamManager.class).toInstance(MinecraftServer.getTeamManager()));
      layer.install(bindingBuilder.bind(BlockManager.class).toInstance(MinecraftServer.getBlockManager()));
      layer.install(bindingBuilder.bind(RecipeManager.class).toInstance(MinecraftServer.getRecipeManager()));
      layer.install(bindingBuilder.bind(BossBarManager.class).toInstance(MinecraftServer.getBossBarManager()));
      layer.install(bindingBuilder.bind(CommandManager.class).toInstance(MinecraftServer.getCommandManager()));
      layer.install(bindingBuilder.bind(PacketProcessor.class).toInstance(MinecraftServer.getPacketProcessor()));
      layer.install(bindingBuilder.bind(InstanceManager.class).toInstance(MinecraftServer.getInstanceManager()));
      layer.install(bindingBuilder.bind(ExceptionManager.class).toInstance(MinecraftServer.getExceptionManager()));
      layer.install(bindingBuilder.bind(BenchmarkManager.class).toInstance(MinecraftServer.getBenchmarkManager()));
      layer.install(bindingBuilder.bind(SchedulerManager.class).toInstance(MinecraftServer.getSchedulerManager()));
      layer.install(bindingBuilder.bind(ConnectionManager.class).toInstance(MinecraftServer.getConnectionManager()));
      layer.install(bindingBuilder.bind(ExtensionManager.class).toInstance(ExtensionBootstrap.getExtensionManager()));
      layer.install(bindingBuilder.bind(GlobalEventHandler.class).toInstance(MinecraftServer.getGlobalEventHandler()));
      layer.install(bindingBuilder.bind(AdvancementManager.class).toInstance(MinecraftServer.getAdvancementManager()));
      layer.install(bindingBuilder.bind(PacketListenerManager.class).toInstance(MinecraftServer.getPacketListenerManager()));

      // install the bindings which are specific to the plugin
      injector.installSpecified(fixedBindingWithBound(platformData, Extension.class));
    });
  }
}
