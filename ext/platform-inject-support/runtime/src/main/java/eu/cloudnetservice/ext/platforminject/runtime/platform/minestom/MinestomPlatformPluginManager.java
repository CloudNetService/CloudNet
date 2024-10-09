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

import dev.derklaro.aerogel.Injector;
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
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(@NonNull Extension platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", targetedBuilder -> {
      // install bindings for the platform
      var layer = BASE_INJECTION_LAYER;
      var builder = layer.injector().createBindingBuilder();
      layer.install(builder.bind(ServerProcess.class).toInstance(MinecraftServer.process()));
      layer.install(builder.bind(ComponentLogger.class).toInstance(platformData.getLogger()));
      layer.install(builder.bind(TagManager.class).toInstance(MinecraftServer.getTagManager()));
      layer.install(builder.bind(TeamManager.class).toInstance(MinecraftServer.getTeamManager()));
      layer.install(builder.bind(BlockManager.class).toInstance(MinecraftServer.getBlockManager()));
      layer.install(builder.bind(RecipeManager.class).toInstance(MinecraftServer.getRecipeManager()));
      layer.install(builder.bind(BossBarManager.class).toInstance(MinecraftServer.getBossBarManager()));
      layer.install(builder.bind(CommandManager.class).toInstance(MinecraftServer.getCommandManager()));
      layer.install(builder.bind(PacketProcessor.class).toInstance(MinecraftServer.getPacketProcessor()));
      layer.install(builder.bind(InstanceManager.class).toInstance(MinecraftServer.getInstanceManager()));
      layer.install(builder.bind(ExceptionManager.class).toInstance(MinecraftServer.getExceptionManager()));
      layer.install(builder.bind(BenchmarkManager.class).toInstance(MinecraftServer.getBenchmarkManager()));
      layer.install(builder.bind(SchedulerManager.class).toInstance(MinecraftServer.getSchedulerManager()));
      layer.install(builder.bind(ConnectionManager.class).toInstance(MinecraftServer.getConnectionManager()));
      layer.install(builder.bind(ExtensionManager.class).toInstance(ExtensionBootstrap.getExtensionManager()));
      layer.install(builder.bind(GlobalEventHandler.class).toInstance(MinecraftServer.getGlobalEventHandler()));
      layer.install(builder.bind(AdvancementManager.class).toInstance(MinecraftServer.getAdvancementManager()));
      layer.install(builder.bind(PacketListenerManager.class).toInstance(MinecraftServer.getPacketListenerManager()));

      // install the bindings which are specific to the plugin
      targetedBuilder.installBinding(builder.bind(Extension.class).toInstance(platformData));
    });
  }
}
