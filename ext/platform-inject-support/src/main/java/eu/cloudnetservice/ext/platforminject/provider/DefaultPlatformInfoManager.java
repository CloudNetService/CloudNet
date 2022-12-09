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

package eu.cloudnetservice.ext.platforminject.provider;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class DefaultPlatformInfoManager implements PlatformInfoManager {

  public static final PlatformInfoManager INSTANCE = new DefaultPlatformInfoManager();

  private final Map<String, PlatformInfoProvider<?, ?>> registeredProviders = new ConcurrentHashMap<>(16, 0.9f, 1);

  public DefaultPlatformInfoManager() {
    var loader = ServiceLoader.load(PlatformInfoProvider.class, DefaultPlatformInfoManager.class.getClassLoader());
    loader.forEach(this::registerProvider);
  }

  private static @NonNull String sanitizeName(@NonNull String name) {
    return name.toLowerCase(Locale.ROOT);
  }

  @Override
  public boolean hasProviderForPlatform(@NonNull String platformName) {
    return this.registeredProviders.containsKey(sanitizeName(platformName));
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable <I, T> PlatformInfoProvider<I, T> provider(@NonNull String platformName) {
    return (PlatformInfoProvider<I, T>) this.registeredProviders.get(sanitizeName(platformName));
  }

  @Override
  public @NonNull <I, T> PlatformInfoProvider<I, T> safeProvider(@NonNull String platformName) {
    PlatformInfoProvider<I, T> provider = this.provider(platformName);
    Objects.requireNonNull(provider, String.format("No provider with the name %s is registered", platformName));
    return provider;
  }

  @Override
  public boolean unregisterProvider(@NonNull String platformName) {
    return this.registeredProviders.remove(sanitizeName(platformName)) != null;
  }

  @Override
  public @NonNull PlatformInfoManager registerProvider(@NonNull PlatformInfoProvider<?, ?> infoProvider) {
    this.registeredProviders.putIfAbsent(sanitizeName(infoProvider.name()), infoProvider);
    return this;
  }
}
