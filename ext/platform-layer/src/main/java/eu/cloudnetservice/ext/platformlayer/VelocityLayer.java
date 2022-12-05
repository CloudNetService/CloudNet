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

import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectUtil;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import lombok.NonNull;

public final class VelocityLayer {

  private static InjectionLayer<SpecifiedInjector> VELOCITY_PLATFORM_LAYER;

  private VelocityLayer() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull InjectionLayer<SpecifiedInjector> create(
    @NonNull ProxyServer proxy,
    @NonNull String name
  ) {
    if (VELOCITY_PLATFORM_LAYER == null) {
      VELOCITY_PLATFORM_LAYER = InjectionLayer.specifiedChild(
        InjectionLayer.ext(),
        "Velocity",
        (specifiedLayer, injector) -> {
          // some default bukkit bindings
          specifiedLayer.install(InjectUtil.createFixedBinding(ProxyServer.class, proxy));
          specifiedLayer.install(InjectUtil.createFixedBinding(Scheduler.class, proxy.getScheduler()));
          specifiedLayer.install(InjectUtil.createFixedBinding(PluginManager.class, proxy.getPluginManager()));
        });
    }

    return InjectionLayer.specifiedChild(VELOCITY_PLATFORM_LAYER, name, (specifiedLayer, injector) -> {
    });
  }
}
