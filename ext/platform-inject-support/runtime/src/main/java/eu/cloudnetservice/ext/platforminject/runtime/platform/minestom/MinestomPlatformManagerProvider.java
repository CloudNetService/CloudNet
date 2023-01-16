/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.runtime.platform.minestom;

import static eu.cloudnetservice.ext.platforminject.runtime.util.LazyClassInstantiationUtil.makeLazyLoader;

import eu.cloudnetservice.ext.platforminject.api.spi.PlatformPluginManagerProvider;
import net.minestom.server.extensions.Extension;

public final class MinestomPlatformManagerProvider extends PlatformPluginManagerProvider<String, Extension> {

  public MinestomPlatformManagerProvider() {
    super("minestom", makeLazyLoader(MinestomPlatformManagerProvider.class, "MinestomPlatformPluginManager"));
  }
}
