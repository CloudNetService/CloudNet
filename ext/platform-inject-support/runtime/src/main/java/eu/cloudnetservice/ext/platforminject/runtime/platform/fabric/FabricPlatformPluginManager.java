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

package eu.cloudnetservice.ext.platforminject.runtime.platform.fabric;

import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;

public final class FabricPlatformPluginManager extends BasePlatformPluginManager<Integer, Object> {

  public FabricPlatformPluginManager() {
    super(System::identityHashCode, FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(@NonNull Object platformData) {
    return InjectionLayer.specifiedChild(
      BASE_INJECTION_LAYER,
      "plugin",
      targetedBuilder -> {
        var bindingBuilder = BASE_INJECTION_LAYER.injector().createBindingBuilder();
        targetedBuilder.installBinding(bindingBuilder.bind(Object.class).toInstance(platformData));
      });
  }
}
