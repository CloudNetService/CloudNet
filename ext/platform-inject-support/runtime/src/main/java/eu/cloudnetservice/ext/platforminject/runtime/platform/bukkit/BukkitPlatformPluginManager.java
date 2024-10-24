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

package eu.cloudnetservice.ext.platforminject.runtime.platform.bukkit;

import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;

public final class BukkitPlatformPluginManager extends BasePlatformPluginManager<String, JavaPlugin> {

  public BukkitPlatformPluginManager() {
    super(Plugin::getName, FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(@NonNull JavaPlugin platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", targetedBuilder -> {
      // install bindings for the platform
      var layer = BASE_INJECTION_LAYER;
      var builder = layer.injector().createBindingBuilder();
      layer.install(builder.bind(Server.class).toInstance(platformData.getServer()));
      layer.install(builder.bind(BukkitScheduler.class).toInstance(platformData.getServer().getScheduler()));
      layer.install(builder.bind(PluginManager.class).toInstance(platformData.getServer().getPluginManager()));
      layer.install(builder.bind(ServicesManager.class).toInstance(platformData.getServer().getServicesManager()));
      layer.install(builder.bind(ScoreboardManager.class).toInstance(platformData.getServer().getScoreboardManager()));

      // install the bindings which are specific to the plugin
      targetedBuilder.installBinding(builder.bind(Plugin.class).toInstance(platformData));
      targetedBuilder.installBinding(builder.bind(PluginBase.class).toInstance(platformData));
      targetedBuilder.installBinding(builder.bind(JavaPlugin.class).toInstance(platformData));
    });
  }
}
