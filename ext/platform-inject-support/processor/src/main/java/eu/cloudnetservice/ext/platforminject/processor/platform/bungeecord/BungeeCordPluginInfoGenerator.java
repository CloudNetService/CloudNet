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

package eu.cloudnetservice.ext.platforminject.processor.platform.bungeecord;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.yaml.YamlFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import lombok.NonNull;

final class BungeeCordPluginInfoGenerator extends NightConfigInfoGenerator {

  public BungeeCordPluginInfoGenerator() {
    super(YamlFormat.defaultInstance(), "plugin.bungeecord.yml");
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

    // optional values
    ConfigUtil.putIfPresent(target, "description", pluginData.description());

    // collect the plugin dependencies
    var depends = pluginData.dependencies().stream().filter(dep -> !dep.optional()).map(Dependency::name).toList();
    var softDepends = pluginData.dependencies().stream().filter(Dependency::optional).map(Dependency::name).toList();

    // put the plugin dependencies
    ConfigUtil.putIfValuesPresent(target, "depends", depends);
    ConfigUtil.putIfValuesPresent(target, "softdepends", softDepends);

    // collect the external dependencies
    var libraries = pluginData.externalDependencies().stream()
      .map(lib -> String.format("%s:%s:%s", lib.groupId(), lib.artifactId(), lib.version()))
      .toList();
    ConfigUtil.putIfValuesPresent(target, "libraries", libraries);
  }
}
