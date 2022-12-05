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
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.extensions.Extension;
import net.minestom.server.timer.Scheduler;

public final class MinestomLayer {

  static {
    var process = MinecraftServer.process();
    var extLayer = InjectionLayer.ext();
    // install the default bindings
    extLayer.install(InjectUtil.createFixedBinding(ServerProcess.class, process));
    extLayer.install(InjectUtil.createFixedBinding(Scheduler.class, process.scheduler()));
  }

  private MinestomLayer() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull InjectionLayer<SpecifiedInjector> create(@NonNull Extension ext) {
    return InjectionLayer.specifiedChild(
      InjectionLayer.ext(),
      ext.getOrigin().getName(),
      (specifiedLayer, injector) -> injector.installSpecified(InjectUtil.createFixedBinding(Extension.class, ext)));
  }
}
