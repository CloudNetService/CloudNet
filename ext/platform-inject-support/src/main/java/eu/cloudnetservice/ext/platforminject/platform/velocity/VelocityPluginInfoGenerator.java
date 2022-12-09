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

package eu.cloudnetservice.ext.platforminject.platform.velocity;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.json.JsonFormat;
import eu.cloudnetservice.ext.platforminject.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.info.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.util.ConfigUtil;
import eu.cloudnetservice.ext.platforminject.util.PluginUtil;
import lombok.NonNull;

final class VelocityPluginInfoGenerator extends NightConfigInfoGenerator {

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
    target.add("id", pluginData.id());
    target.add("name", pluginData.name());
    target.add("version", pluginData.name());
    target.add("main", platformMainClassName);

    // optional values
    ConfigUtil.putIfPresent(target, "url", pluginData.homepage());
    ConfigUtil.putIfPresent(target, "description", pluginData.description());
    ConfigUtil.putIfValuesPresent(target, "authors", pluginData.authors());

    // collect the plugin dependencies
    var depends = pluginData.dependencies().stream().map(dependency -> {
      var dependencySection = this.configFormat.createConfig();
      dependencySection.add("id", PluginUtil.convertNameToId(dependency.name()));
      dependencySection.add("optional", dependency.optional());
      return dependencySection;
    }).toList();
    ConfigUtil.putIfValuesPresent(target, "dependencies", depends);
  }
}
