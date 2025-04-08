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

package eu.cloudnetservice.ext.platforminject.api.spi;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.ext.platforminject.api.PlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import java.util.function.Supplier;
import lombok.NonNull;

public class PlatformPluginManagerProvider<I, T> implements Named {

  private final String name;
  private final Supplier<PlatformPluginManager<I, T>> managerProvider;

  public PlatformPluginManagerProvider(
    @NonNull String name,
    @NonNull Supplier<PlatformPluginManager<I, T>> managerProvider
  ) {
    this.name = name;
    this.managerProvider = FunctionalUtil.memoizing(managerProvider);
  }

  public @NonNull PlatformPluginManager<I, T> provideManager() {
    return this.managerProvider.get();
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }
}
