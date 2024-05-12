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

import eu.cloudnetservice.ext.platforminject.api.data.PluginDataParser;
import eu.cloudnetservice.ext.platforminject.api.generator.PlatformMainClassGenerator;
import eu.cloudnetservice.ext.platforminject.api.generator.PluginInfoGenerator;
import eu.cloudnetservice.ext.platforminject.api.util.FunctionalUtil;
import java.util.function.Supplier;
import lombok.NonNull;

public class PlatformDataGeneratorProvider {

  private final String name;

  private final Supplier<PluginDataParser> pluginDataParser;
  private final Supplier<PluginInfoGenerator> infoGenerator;
  private final Supplier<PlatformMainClassGenerator> mainClassGenerator;

  public PlatformDataGeneratorProvider(
    @NonNull String name,
    @NonNull Supplier<PluginInfoGenerator> infoGenerator,
    @NonNull Supplier<PlatformMainClassGenerator> mainClassGenerator,
    @NonNull Supplier<PluginDataParser> pluginDataParser
  ) {
    this.name = name;
    this.pluginDataParser = FunctionalUtil.memoizing(pluginDataParser);
    this.infoGenerator = FunctionalUtil.memoizing(infoGenerator);
    this.mainClassGenerator = FunctionalUtil.memoizing(mainClassGenerator);
  }

  public @NonNull PluginDataParser dataParser() {
    return this.pluginDataParser.get();
  }

  public @NonNull PluginInfoGenerator infoGenerator() {
    return this.infoGenerator.get();
  }

  public @NonNull PlatformMainClassGenerator mainClassGenerator() {
    return this.mainClassGenerator.get();
  }

  public @NonNull String name() {
    return this.name;
  }
}
