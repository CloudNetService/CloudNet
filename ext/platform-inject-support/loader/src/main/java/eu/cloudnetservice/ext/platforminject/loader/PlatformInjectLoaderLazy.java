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

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.ext.platforminject.api.registry.PlatformManagerRegistryHolder;
import lombok.NonNull;

final class PlatformInjectLoaderLazy {

  private static final String SUPPORT_JAR_NAME = "platform-inject-support.jar";
  private static final String BASE_PACKAGE_NAME_FORMAT = "eu.cloudnetservice.ext.platforminject.%s";

  // the root class loader we use to find all our classes
  /* @MonotonicNonNull */ static ClassLoader loader;

  /* @MonotonicNonNull */ static MethodAccessor<?> getPlatformInfoManager;
  /* @MonotonicNonNull */ static MethodAccessor<?> getPlatformInfoProvider;
  /* @MonotonicNonNull */ static MethodAccessor<?> getPluginManager;
  /* @MonotonicNonNull */ static MethodAccessor<?> constructAndLoad;
  /* @MonotonicNonNull */ static MethodAccessor<?> disablePlugin;

  public static void ensureInitialized(@NonNull ClassLoader parentLoader) {
    // check if the loader is initialized - that is the best indication if
    // we already tried to resolve all needed values elsewhere
    if (loader == null) {
      // get the platform inject loader jar
      var loaderJar = PlatformInjectLoaderLazy.class.getClassLoader().getResource(SUPPORT_JAR_NAME);
      if (loaderJar == null) {
        throw new IllegalStateException("Loader jar is missing, invalid compile?");
      }

      // construct a new class loader which is aware of the loader jar
      // to ensure we are not overriding something by accident, check again if the loader is still needed
      var internalLoader = new JarInJarClassLoader("inject-support", loaderJar, parentLoader);
      if (loader == null) {
        // assign the loader and init the manager registry
        loader = internalLoader;
        PlatformManagerRegistryHolder.init(internalLoader);

        // resolve all classes from the loaded jar
        var platformInfoManagerClass = loadClass(loader, "provider.PlatformInfoManager");
        var platformInfoProviderClass = loadClass(loader, "provider.PlatformInfoProvider");
        var platformPluginManagerClass = loadClass(loader, "PlatformPluginManager");

        // get the method accessors
        getPlatformInfoManager = Reflexion.on(platformInfoManagerClass).findMethod("manager").orElseThrow();
        getPlatformInfoProvider = Reflexion.on(platformInfoManagerClass)
          .findMethod("safeProvider", String.class)
          .orElseThrow();
        getPluginManager = Reflexion.on(platformInfoProviderClass).findMethod("pluginManager").orElseThrow();
        constructAndLoad = Reflexion.on(platformPluginManagerClass)
          .findMethod("constructAndLoad", Class.class, Object.class)
          .orElseThrow();
        disablePlugin = Reflexion.on(platformPluginManagerClass)
          .findMethod("disablePlugin", Object.class)
          .orElseThrow();
      }
    }
  }

  private static @NonNull Class<?> loadClass(@NonNull ClassLoader loader, @NonNull String relativeName) {
    try {
      var fullName = String.format(BASE_PACKAGE_NAME_FORMAT, relativeName);
      return Class.forName(fullName, true, loader);
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException("Unable to resolve class " + relativeName, exception);
    }
  }
}
