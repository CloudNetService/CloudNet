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

package eu.cloudnetservice.ext.platforminject.processor.platform.velocity;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.json.JsonFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.processor.id.CharRange;
import eu.cloudnetservice.ext.platforminject.processor.id.PluginIdGenerator;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import lombok.NonNull;

final class VelocityPluginInfoGenerator extends NightConfigInfoGenerator {

  // [a-z][a-z0-9-_]{0,63}
  private static final PluginIdGenerator PLUGIN_ID_GENERATOR = PluginIdGenerator.withBoundedLength(1, 63)
    .registerRange(
      0,
      1,
      'a',
      CharRange.range('a', 'z'))
    .registerRange(
      1,
      '_',
      CharRange.range('-'),
      CharRange.range('_'),
      CharRange.range('a', 'z'),
      CharRange.range('0', '9'));

  public VelocityPluginInfoGenerator() {
    super(JsonFormat.minimalInstance(), "velocity-plugin.json");
  }

  @Override
  protected void applyPlatformInfo(
    @NonNull Config target,
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName
  ) {
    // base values
    target.add("name", pluginData.name());
    target.add("version", pluginData.name());
    target.add("main", platformMainClassName);
    target.add("id", PLUGIN_ID_GENERATOR.convert(pluginData.name()));

    // optional values
    ConfigUtil.putIfPresent(target, "url", pluginData.homepage());
    ConfigUtil.putIfPresent(target, "description", pluginData.description());
    ConfigUtil.putIfValuesPresent(target, "authors", pluginData.authors());

    // collect the plugin dependencies
    var depends = pluginData.dependencies().stream().map(dependency -> {
      var dependencySection = this.configFormat.createConfig();
      dependencySection.add("id", PLUGIN_ID_GENERATOR.convert(dependency.name()));
      dependencySection.add("optional", dependency.optional());
      return dependencySection;
    }).toList();
    ConfigUtil.putIfValuesPresent(target, "dependencies", depends);
  }
}
