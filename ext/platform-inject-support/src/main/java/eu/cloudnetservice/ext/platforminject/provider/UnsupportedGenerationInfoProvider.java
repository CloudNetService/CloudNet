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

import eu.cloudnetservice.ext.platforminject.PlatformPluginManager;
import eu.cloudnetservice.ext.platforminject.data.PluginDataParser;
import eu.cloudnetservice.ext.platforminject.generator.PlatformMainClassGenerator;
import eu.cloudnetservice.ext.platforminject.info.PluginInfoGenerator;
import eu.cloudnetservice.ext.platforminject.util.SupplierUtil;
import java.util.function.Supplier;
import lombok.NonNull;

public class UnsupportedGenerationInfoProvider<I, T> implements PlatformInfoProvider<I, T> {

  private final String name;
  private final Supplier<PlatformPluginManager<I, T>> pluginManager;

  protected UnsupportedGenerationInfoProvider(
    @NonNull String name,
    @NonNull Supplier<PlatformPluginManager<I, T>> pluginManager
  ) {
    this.name = name;
    this.pluginManager = SupplierUtil.memoizing(pluginManager);
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  @Override
  public @NonNull PlatformPluginManager<I, T> pluginManager() {
    return this.pluginManager.get();
  }

  @Override
  public @NonNull PluginDataParser dataParser() {
    throw new UnsupportedOperationException("Info generation is not supported on " + this.name);
  }

  @Override
  public @NonNull PluginInfoGenerator infoGenerator() {
    throw new UnsupportedOperationException("Info generation is not supported on " + this.name);
  }

  @Override
  public @NonNull PlatformMainClassGenerator mainClassGenerator() {
    throw new UnsupportedOperationException("Main class generation is not supported on " + this.name);
  }
}
