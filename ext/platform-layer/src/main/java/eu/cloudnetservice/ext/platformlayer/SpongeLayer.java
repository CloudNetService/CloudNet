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
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.plugin.PluginContainer;

public final class SpongeLayer {

  private static final InjectionLayer<SpecifiedInjector> SPONGE_PLATFORM_LAYER;

  static {
    SPONGE_PLATFORM_LAYER = InjectionLayer.specifiedChild(
      InjectionLayer.ext(),
      "Sponge",
      (specifiedLayer, injector) -> {
        // some default sponge bindings
        specifiedLayer.install(InjectUtil.createFixedBinding(Server.class, Sponge.server()));
        specifiedLayer.install(InjectUtil.createFixedBinding(Scheduler.class, Sponge.asyncScheduler()));
        specifiedLayer.install(InjectUtil.createFixedBinding(PluginManager.class, Sponge.pluginManager()));
      });
  }

  private SpongeLayer() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull InjectionLayer<SpecifiedInjector> create(@NonNull PluginContainer plugin) {
    return InjectionLayer.specifiedChild(
      SPONGE_PLATFORM_LAYER,
      plugin.metadata().id(),
      (layer, injector) -> injector.installSpecified(InjectUtil.createFixedBinding(PluginContainer.class, plugin)));
  }
}
