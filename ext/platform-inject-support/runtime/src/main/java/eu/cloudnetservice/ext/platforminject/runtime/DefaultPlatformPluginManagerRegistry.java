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

package eu.cloudnetservice.ext.platforminject.runtime;

import eu.cloudnetservice.ext.platforminject.api.PlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.spi.PlatformPluginManagerProvider;
import eu.cloudnetservice.ext.platforminject.api.spi.PlatformPluginManagerRegistry;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultPlatformPluginManagerRegistry implements PlatformPluginManagerRegistry {

  private final Map<String, PlatformPluginManagerProvider<?, ?>> registeredManagers
    = new ConcurrentHashMap<>(16, 0.9f, 1);

  public DefaultPlatformPluginManagerRegistry() {
    var loader = ServiceLoader.load(PlatformPluginManagerProvider.class, this.getClass().getClassLoader());
    loader.forEach(provider -> this.registeredManagers.put(provider.name(), provider));
  }

  @Override
  public boolean hasManager(@NonNull String platformName) {
    return this.registeredManagers.containsKey(platformName);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable <I, T> PlatformPluginManager<I, T> manager(@NonNull String platformName) {
    var provider = this.registeredManagers.get(platformName);
    return provider == null ? null : (PlatformPluginManager<I, T>) provider.provideManager();
  }

  @Override
  public @NonNull <I, T> PlatformPluginManager<I, T> safeManager(@NonNull String platformName) {
    PlatformPluginManager<I, T> manager = this.manager(platformName);
    Objects.requireNonNull(manager, String.format("No manager with the name %s is registered", platformName));
    return manager;
  }

  @Override
  public boolean unregisterManager(@NonNull String platformName) {
    return this.registeredManagers.remove(platformName) != null;
  }

  @Override
  public @NonNull PlatformPluginManagerRegistry registerManager(@NonNull PlatformPluginManagerProvider<?, ?> provider) {
    this.registeredManagers.putIfAbsent(provider.name(), provider);
    return this;
  }
}
