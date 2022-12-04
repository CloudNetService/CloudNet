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

package eu.cloudnetservice.ext.platformlayer;import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.Bindings;
import dev.derklaro.aerogel.Element;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class BukkitLayer {

  public static @NonNull InjectionLayer<?> create(@NonNull JavaPlugin plugin) {
    var server = plugin.getServer();

    return InjectionLayer.specifiedChild(InjectionLayer.ext(), plugin.getName(), (specifiedLayer, injector) -> {
      // some default bukkit bindings
      specifiedLayer.install(fixedBinding(Server.class, server));
      specifiedLayer.install(fixedBinding(BukkitScheduler.class, server.getScheduler()));
      specifiedLayer.install(fixedBinding(PluginManager.class, server.getPluginManager()));
      injector.installSpecified(fixedBinding(JavaPlugin.class, plugin));
    });
  }

  private static @NonNull BindingConstructor fixedBinding(@NonNull Type type, @NonNull Object value) {
    return Bindings.fixed(Element.forType(type), value);
  }

}
