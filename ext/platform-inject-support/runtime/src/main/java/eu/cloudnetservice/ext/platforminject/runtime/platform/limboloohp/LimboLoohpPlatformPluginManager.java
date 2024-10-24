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

package eu.cloudnetservice.ext.platforminject.runtime.platform.limboloohp;

import com.loohp.limbo.Limbo;
import com.loohp.limbo.events.EventsManager;
import com.loohp.limbo.plugins.LimboPlugin;
import com.loohp.limbo.plugins.PluginManager;
import com.loohp.limbo.scheduler.LimboScheduler;
import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.defaults.BasePlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import lombok.NonNull;

public final class LimboLoohpPlatformPluginManager extends BasePlatformPluginManager<String, LimboPlugin> {

  public LimboLoohpPlatformPluginManager() {
    super(LimboPlugin::getName, FunctionalUtil.identity());
  }

  @Override
  protected @NonNull InjectionLayer<Injector> createInjectionLayer(@NonNull LimboPlugin platformData) {
    return InjectionLayer.specifiedChild(BASE_INJECTION_LAYER, "plugin", targetedBuilder -> {
      // install bindings for the platform
      var layer = BASE_INJECTION_LAYER;
      var bindingBuilder = layer.injector().createBindingBuilder();
      layer.install(bindingBuilder.bind(Limbo.class).toInstance(platformData.getServer()));
      layer.install(bindingBuilder.bind(PluginManager.class).toInstance(platformData.getServer().getPluginManager()));
      layer.install(bindingBuilder.bind(EventsManager.class).toInstance(platformData.getServer().getEventsManager()));
      layer.install(bindingBuilder.bind(LimboScheduler.class).toInstance(platformData.getServer().getScheduler()));

      // install the bindings which are specific to the plugin
      targetedBuilder.installBinding(bindingBuilder.bind(LimboPlugin.class).toInstance(platformData));
    });
  }
}
