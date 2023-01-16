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

package eu.cloudnetservice.ext.platforminject.processor.platform.nukkit;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.yaml.YamlFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.processor.id.CharRange;
import eu.cloudnetservice.ext.platforminject.processor.id.PluginIdGenerator;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

@SuppressWarnings("DuplicatedCode") // we love that nukkit is almost equal to bukkit, right?
final class NukkitPluginInfoGenerator extends NightConfigInfoGenerator {

  // [^A-Za-z0-9 _.-]
  // whitespaces are replaced in runtime with underscores
  private static final PluginIdGenerator PLUGIN_NAME_GENERATOR = PluginIdGenerator.withInfiniteLength()
    .registerRange(
      0,
      '_',
      CharRange.range('_'),
      CharRange.range('.'),
      CharRange.range('-'),
      CharRange.range('a', 'z'),
      CharRange.range('A', 'Z'),
      CharRange.range('0', '9'));

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
    target.add("version", pluginData.version());
    target.add("main", platformMainClassName);
    target.add("name", PLUGIN_NAME_GENERATOR.convert(pluginData.name()));

    // optional values
    ConfigUtil.putIfPresent(target, "api", pluginData.apiVersion());
    ConfigUtil.putIfPresent(target, "website", pluginData.homepage());
    ConfigUtil.putIfPresent(target, "description", pluginData.description());
    ConfigUtil.putIfValuesPresent(target, "authors", pluginData.authors());

    // collect the plugin dependencies
    var depends = pluginData.dependencies().stream()
      .filter(dep -> !dep.optional())
      .map(Dependency::name)
      .map(PLUGIN_NAME_GENERATOR::convert)
      .toList();
    var softDepends = pluginData.dependencies().stream()
      .filter(Dependency::optional)
      .map(Dependency::name)
      .map(PLUGIN_NAME_GENERATOR::convert)
      .toList();

    // put the plugin dependencies
    ConfigUtil.putIfValuesPresent(target, "depend", depends);
    ConfigUtil.putIfValuesPresent(target, "softdepend", softDepends);

    // collect the commands
    var commandsSection = pluginData.commands().stream().map(command -> {
      var commandSection = this.configFormat.createConfig();
      ConfigUtil.putIfNotBlank(commandSection, "usage", command.usage());
      ConfigUtil.putIfNotBlank(commandSection, "permission", command.permission());
      ConfigUtil.putIfNotBlank(commandSection, "description", command.description());
      ConfigUtil.putIfValuesPresent(commandSection, "aliases", command.aliases());
      return Map.entry(command, commandSection);
    }).collect(Collectors.collectingAndThen(Collectors.toList(), commands -> {
      var finalSection = this.configFormat.createConfig();
      for (var entry : commands) {
        finalSection.add(entry.getKey().name(), entry.getValue());
      }
      return finalSection;
    }));
    target.add("commands", commandsSection);
  }
}
