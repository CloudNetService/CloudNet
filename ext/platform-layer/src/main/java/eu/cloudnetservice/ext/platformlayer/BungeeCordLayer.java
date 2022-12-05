/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platformlayer;

import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectUtil;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;

public final class BungeeCordLayer {

  private static final InjectionLayer<SpecifiedInjector> BUNGEECORD_PLATFORM_LAYER;

  static {
    var proxy = ProxyServer.getInstance();
    BUNGEECORD_PLATFORM_LAYER = InjectionLayer.specifiedChild(
      InjectionLayer.ext(),
      "BungeeCord",
      (specifiedLayer, injector) -> {
        // some default bungee bindings
        specifiedLayer.install(InjectUtil.createFixedBinding(ProxyServer.class, proxy));
        specifiedLayer.install(InjectUtil.createFixedBinding(TaskScheduler.class, proxy.getScheduler()));
        specifiedLayer.install(InjectUtil.createFixedBinding(PluginManager.class, proxy.getPluginManager()));
      });
  }

  private BungeeCordLayer() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull InjectionLayer<SpecifiedInjector> create(@NonNull Plugin plugin) {
    return InjectionLayer.specifiedChild(
      BUNGEECORD_PLATFORM_LAYER,
      plugin.getDescription().getName(),
      (specifiedLayer, injector) -> injector.installSpecified(InjectUtil.createFixedBinding(Plugin.class, plugin)));
  }
}
