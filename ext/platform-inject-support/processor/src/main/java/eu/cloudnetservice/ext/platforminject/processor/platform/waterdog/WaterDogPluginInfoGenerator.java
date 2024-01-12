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

package eu.cloudnetservice.ext.platforminject.processor.platform.waterdog;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.yaml.YamlFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import lombok.NonNull;

final class WaterDogPluginInfoGenerator extends NightConfigInfoGenerator {

  public WaterDogPluginInfoGenerator() {
    super(YamlFormat.defaultInstance(), "plugin.waterdog_pe.yml");
  }

  @Override
  protected void applyPlatformInfo(
    @NonNull Config target,
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName
  ) {
    // base values
    target.add("name", pluginData.name());
    target.add("version", pluginData.version());
    target.add("main", platformMainClassName);
    ConfigUtil.putFirstOrDefault(target, "author", pluginData.authors(), "CloudNetService");

    // put in the plugin dependencies
    var depends = pluginData.dependencies().stream().map(Dependency::name).toList();
    ConfigUtil.putIfValuesPresent(target, "depends", depends);
  }
}
