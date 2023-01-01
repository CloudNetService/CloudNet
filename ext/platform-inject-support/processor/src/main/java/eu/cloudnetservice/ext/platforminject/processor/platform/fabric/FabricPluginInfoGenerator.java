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

package eu.cloudnetservice.ext.platforminject.processor.platform.fabric;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.json.JsonFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

final class FabricPluginInfoGenerator extends NightConfigInfoGenerator {

  public FabricPluginInfoGenerator() {
    super(JsonFormat.minimalInstance(), "fabric.mod.json");
  }

  @Override
  protected void applyPlatformInfo(
    @NonNull Config target,
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName
  ) {
    // base data
    target.add("id", pluginData.id());
    target.add("schemaVersion", 1);
    target.add("name", pluginData.name());
    target.add("environment", "server");
    target.add("version", pluginData.version());

    // add the main clas for the server
    var main = ConfigUtil.tap(this.configFormat, config -> config.add("server", List.of(platformMainClassName)));
    target.add("entrypoints", main);

    // optional values
    ConfigUtil.putIfPresent(target, "description", pluginData.description());

    // put in the homepage (if given)
    var homepage = pluginData.homepage();
    if (homepage != null) {
      var contact = ConfigUtil.tap(this.configFormat, config -> config.add("homepage", homepage));
      target.add("contact", contact);
    }

    // put in the authors (if any)
    var authors = pluginData.authors().stream()
      .map(author -> ConfigUtil.tap(this.configFormat, config -> config.add("name", author)))
      .toList();
    ConfigUtil.putIfValuesPresent(target, "authors", authors);

    // put in the dependencies (if any)
    var dependencies = pluginData.dependencies().stream()
      .filter(dependency -> !dependency.optional())
      .map(dependency -> Map.entry(dependency.name(), dependency.version()))
      .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        entries -> ConfigUtil.tap(this.configFormat, config -> {
          for (var entry : entries) {
            config.add(entry.getKey(), entry.getValue());
          }
        })));
    target.add("depends", dependencies);

    // put in the soft dependencies (if any)
    var softDepends = pluginData.dependencies().stream()
      .filter(Dependency::optional)
      .map(dependency -> Map.entry(dependency.name(), dependency.version()))
      .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        entries -> ConfigUtil.tap(this.configFormat, config -> {
          for (var entry : entries) {
            config.add(entry.getKey(), entry.getValue());
          }
        })));
    target.add("recommends", softDepends);
  }
}
