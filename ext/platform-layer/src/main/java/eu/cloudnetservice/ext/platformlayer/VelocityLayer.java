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
import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.Bindings;
import dev.derklaro.aerogel.Element;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import java.lang.reflect.Type;
import lombok.NonNull;

public class VelocityLayer {

  public static @NonNull InjectionLayer<?> create(
    @NonNull ProxyServer proxy,
    @NonNull String name
  ) {
    return InjectionLayer.specifiedChild(InjectionLayer.ext(), name, (specifiedLayer, injector) -> {
      // some default bukkit bindings
      specifiedLayer.install(fixedBinding(ProxyServer.class, proxy));
      specifiedLayer.install(fixedBinding(Scheduler.class, proxy.getScheduler()));
      specifiedLayer.install(fixedBinding(PluginManager.class, proxy.getPluginManager()));
    });
  }

  private static @NonNull BindingConstructor fixedBinding(@NonNull Type type, @NonNull Object value) {
    return Bindings.fixed(Element.forType(type), value);
  }

}
