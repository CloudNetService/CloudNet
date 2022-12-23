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

package eu.cloudnetservice.ext.platforminject.processor.platform.sponge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.json.JsonFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.util.PluginUtil;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import java.util.List;
import lombok.NonNull;

final class SpongePluginInfoGenerator extends NightConfigInfoGenerator {

  public SpongePluginInfoGenerator() {
    super(JsonFormat.minimalInstance(), "META-INF/sponge_plugins.json");
  }

  @Override
  protected void applyPlatformInfo(
    @NonNull Config target,
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName
  ) {
    // put in the loader information
    var loaderSection = ConfigUtil.tap(this.configFormat, config -> {
      config.set("name", "java_plain");
      config.set("version", "1.0");
    });
    target.set("loader", loaderSection);

    // put in the license info
    // todo: should we support this?
    target.set("license", "See project page");

    // construct the plugin information
    var pluginMeta = ConfigUtil.tap(this.configFormat, config -> {
      // base values
      config.add("id", pluginData.id());
      config.add("name", pluginData.name());
      config.add("version", pluginData.version());
      config.add("entrypoint", platformMainClassName);

      // optional values
      ConfigUtil.putIfPresent(config, "description", pluginData.description());

      // put in the homepage, if needed
      var homepage = pluginData.homepage();
      if (homepage != null) {
        var links = ConfigUtil.tap(this.configFormat, section -> section.add("homepage", homepage));
        config.add("links", links);
      }

      // put in the contributors
      var contributors = pluginData.authors().stream()
        .map(author -> ConfigUtil.tap(this.configFormat, section -> section.add("name", author)))
        .toList();
      ConfigUtil.putIfValuesPresent(config, "contributors", contributors);

      // put in the plugin dependencies
      var dependencies = pluginData.dependencies().stream()
        .map(dependency -> ConfigUtil.tap(this.configFormat, section -> {
          section.add("id", PluginUtil.convertNameToId(dependency.name()));
          section.add("version", dependency.version());
          section.add("optional", dependency.optional());
        }))
        .toList();
      ConfigUtil.putIfValuesPresent(config, "dependencies", dependencies);
    });
    target.add("plugins", List.of(pluginMeta));
  }
}
