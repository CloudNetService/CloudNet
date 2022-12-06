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

import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.scheduler.ServerScheduler;
import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectUtil;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import lombok.NonNull;

public final class NukkitLayer {

  static {
    var server = Server.getInstance();
    var extLayer = InjectionLayer.ext();
    // install the default bindings
    extLayer.install(InjectUtil.createFixedBinding(Server.class, server));
    extLayer.install(InjectUtil.createFixedBinding(ServerScheduler.class, server.getScheduler()));
    extLayer.install(InjectUtil.createFixedBinding(PluginManager.class, server.getPluginManager()));
  }

  private NukkitLayer() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull InjectionLayer<SpecifiedInjector> create(@NonNull Plugin plugin) {
    return InjectionLayer.specifiedChild(
      InjectionLayer.ext(),
      "plugin",
      (specifiedLayer, injector) -> injector.installSpecified(InjectUtil.createFixedBinding(Plugin.class, plugin)));
  }
}
