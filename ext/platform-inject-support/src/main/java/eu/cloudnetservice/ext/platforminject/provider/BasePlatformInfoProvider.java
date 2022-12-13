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
import eu.cloudnetservice.ext.platforminject.util.FunctionalUtil;
import java.util.function.Supplier;
import lombok.NonNull;

public class BasePlatformInfoProvider<I, T> implements PlatformInfoProvider<I, T> {

  private final String name;
  private final Supplier<PlatformPluginManager<I, T>> pluginManager;

  private final Supplier<PluginDataParser> pluginDataParser;
  private final Supplier<PluginInfoGenerator> infoGenerator;
  private final Supplier<PlatformMainClassGenerator> mainClassGenerator;

  protected BasePlatformInfoProvider(
    @NonNull String name,
    @NonNull Supplier<PlatformPluginManager<I, T>> pluginManager,
    @NonNull Supplier<PluginInfoGenerator> infoGenerator,
    @NonNull Supplier<PlatformMainClassGenerator> mainClassGenerator,
    @NonNull Supplier<PluginDataParser> pluginDataParser
  ) {
    this.name = name;
    this.pluginManager = FunctionalUtil.memoizing(pluginManager);
    this.infoGenerator = FunctionalUtil.memoizing(infoGenerator);
    this.mainClassGenerator = FunctionalUtil.memoizing(mainClassGenerator);
    this.pluginDataParser = FunctionalUtil.memoizing(pluginDataParser);
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
    return this.pluginDataParser.get();
  }

  @Override
  public @NonNull PluginInfoGenerator infoGenerator() {
    return this.infoGenerator.get();
  }

  @Override
  public @NonNull PlatformMainClassGenerator mainClassGenerator() {
    return this.mainClassGenerator.get();
  }
}
