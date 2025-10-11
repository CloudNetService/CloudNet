/*
 * Copyright 2019-present CloudNetService team & contributors
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Internal marker class used by code generation during compilation. Just redirects all made calls to the actual runtime
 * implementation. Only intended to be used in generated code.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class PlatformInjectSupportLoader {

  private static final MethodHandle LOAD_PLUGIN;
  private static final MethodHandle DISABLE_PLUGIN;

  static {
    try {
      var lookup = MethodHandles.publicLookup();
      var runtimeLoader = Class.forName("eu.cloudnetservice.ext.platforminject.loader.PlatformInjectSupportLoaderImpl");

      // PlatformInjectSupportLoaderImpl.loadPlugin() [same signature]
      var loadPluginMt = MethodType.methodType(void.class, String.class, Class.class, Object.class, ClassLoader.class);
      LOAD_PLUGIN = lookup.findStatic(runtimeLoader, "loadPlugin", loadPluginMt);

      // PlatformInjectSupportLoaderImpl.disablePlugin() [same signature]
      var disablePluginMt = MethodType.methodType(void.class, String.class, Object.class);
      DISABLE_PLUGIN = lookup.findStatic(runtimeLoader, "disablePlugin", disablePluginMt);
    } catch (Throwable throwable) {
      throw new ExceptionInInitializerError(throwable);
    }
  }

  private PlatformInjectSupportLoader() {
    throw new UnsupportedOperationException();
  }

  public static void loadPlugin(
    @NonNull String platform,
    @NonNull Class<?> pluginClass,
    @NonNull Object platformData,
    @NonNull ClassLoader platformClassLoader
  ) {
    try {
      LOAD_PLUGIN.invokeExact(platform, pluginClass, platformData, platformClassLoader);
    } catch (Throwable throwable) {
      throw new RuntimeException("Exception while loading plugin", throwable);
    }
  }

  public static void disablePlugin(@NonNull String platform, @NonNull Object platformData) {
    try {
      DISABLE_PLUGIN.invokeExact(platform, platformData);
    } catch (Throwable throwable) {
      throw new RuntimeException("Exception while disabling plugin", throwable);
    }
  }
}
