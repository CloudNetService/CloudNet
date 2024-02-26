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

package eu.cloudnetservice.ext.platforminject.loader;

import eu.cloudnetservice.ext.platforminject.api.registry.PlatformManagerRegistryHolder;
import lombok.NonNull;

final class PlatformInjectLoaderLazy {

  private static final String SUPPORT_JAR_NAME = "platform-inject-support.jar";

  // the root class loader we use to find all our classes
  /* @MonotonicNonNull */ static ClassLoader loader;

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
      }
    }
  }
}
