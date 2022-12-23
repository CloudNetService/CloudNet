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

package eu.cloudnetservice.ext.platforminject.loader;

import dev.derklaro.reflexion.Result;
import dev.derklaro.reflexion.internal.util.Util;
import lombok.NonNull;

public final class PlatformInjectSupportLoader {

  private PlatformInjectSupportLoader() {
    throw new UnsupportedOperationException();
  }

  public static void loadPlugin(
    @NonNull String platform,
    @NonNull Class<?> pluginClass,
    @NonNull Object platformData,
    @NonNull ClassLoader platformClassLoader
  ) {
    // ensure that the parent provider is initialized
    PlatformInjectLoaderLazy.ensureInitialized(platformClassLoader);

    // construct and load the plugin
    resolvePlatformManager(platform)
      .flatMap(manager -> PlatformInjectLoaderLazy.constructAndLoad.invoke(manager, pluginClass, platformData))
      .ifExceptional(Util::throwUnchecked);
  }

  public static void disablePlugin(@NonNull String platform, @NonNull Object platformData) {
    // should not happen, when a plugin is loaded the init process should be executed
    if (PlatformInjectLoaderLazy.loader != null) {
      resolvePlatformManager(platform)
        .flatMap(manager -> PlatformInjectLoaderLazy.disablePlugin.invoke(manager, platformData))
        .ifExceptional(Util::throwUnchecked);
    }
  }

  private static @NonNull Result<Object> resolvePlatformManager(@NonNull String platform) {
    return PlatformInjectLoaderLazy.getPlatformInfoManager.invoke()
      .flatMap(manager -> PlatformInjectLoaderLazy.getPlatformInfoProvider.invoke(manager, platform))
      .flatMap(provider -> PlatformInjectLoaderLazy.getPluginManager.invoke(provider));
  }
}
