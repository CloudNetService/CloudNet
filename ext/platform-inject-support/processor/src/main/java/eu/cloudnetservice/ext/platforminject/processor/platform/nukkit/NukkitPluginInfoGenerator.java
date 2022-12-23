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

package eu.cloudnetservice.ext.platforminject.processor.platform.nukkit;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.yaml.YamlFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

@SuppressWarnings("DuplicatedCode") // we love that nukkit is almost equal to bukkit, right?
final class NukkitPluginInfoGenerator extends NightConfigInfoGenerator {

  public NukkitPluginInfoGenerator() {
    super(YamlFormat.defaultInstance(), "plugin.nukkit.yml");
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

    // optional values
    ConfigUtil.putIfPresent(target, "api", pluginData.apiVersion());
    ConfigUtil.putIfPresent(target, "website", pluginData.homepage());
    ConfigUtil.putIfPresent(target, "description", pluginData.description());
    ConfigUtil.putIfValuesPresent(target, "authors", pluginData.authors());

    // collect the plugin dependencies
    var depends = pluginData.dependencies().stream().filter(dep -> !dep.optional()).map(Dependency::name).toList();
    var softDepends = pluginData.dependencies().stream().filter(Dependency::optional).map(Dependency::name).toList();

    // put the plugin dependencies
    ConfigUtil.putIfValuesPresent(target, "depend", depends);
    ConfigUtil.putIfValuesPresent(target, "softdepend", softDepends);

    // collect the commands
    var commands = pluginData.commands().stream().map(command -> {
      var commandSection = this.configFormat.createConfig();
      ConfigUtil.putIfNotBlank(commandSection, "usage", command.usage());
      ConfigUtil.putIfNotBlank(commandSection, "permission", command.permission());
      ConfigUtil.putIfNotBlank(commandSection, "description", command.description());
      ConfigUtil.putIfValuesPresent(commandSection, "aliases", command.aliases());
      return Map.entry(command, commandSection);
    }).collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
    ConfigUtil.putIfValuesPresent(target, "commands", commands);
  }
}
