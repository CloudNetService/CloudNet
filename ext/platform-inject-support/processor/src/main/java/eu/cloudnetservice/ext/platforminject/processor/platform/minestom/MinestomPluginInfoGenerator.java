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

package eu.cloudnetservice.ext.platforminject.processor.platform.minestom;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.json.JsonFormat;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ExternalDependency;
import eu.cloudnetservice.ext.platforminject.processor.id.CharRange;
import eu.cloudnetservice.ext.platforminject.processor.id.PluginIdGenerator;
import eu.cloudnetservice.ext.platforminject.processor.infogen.NightConfigInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ConfigUtil;
import lombok.NonNull;

final class MinestomPluginInfoGenerator extends NightConfigInfoGenerator {

  // [A-Za-z][_A-Za-z0-9]+
  private static final PluginIdGenerator EXTENSION_NAME_GENERATOR = PluginIdGenerator.withInfiniteLength()
    .registerRange(
      0,
      1,
      'a',
      CharRange.range('a', 'z'),
      CharRange.range('A', 'Z'))
    .registerRange(
      1,
      '_',
      CharRange.range('_'),
      CharRange.range('a', 'z'),
      CharRange.range('A', 'Z'),
      CharRange.range('0', '9'));

  public MinestomPluginInfoGenerator() {
    super(JsonFormat.minimalInstance(), "extension.json");
  }

  @Override
  protected void applyPlatformInfo(
    @NonNull Config target,
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName
  ) {
    // base values
    target.add("version", pluginData.version());
    target.add("entrypoint", platformMainClassName);
    target.add("name", EXTENSION_NAME_GENERATOR.convert(pluginData.name()));

    // optional values
    ConfigUtil.putIfValuesPresent(target, "authors", pluginData.authors());

    // put in the extension dependencies
    var depends = pluginData.dependencies().stream()
      .map(Dependency::name)
      .map(EXTENSION_NAME_GENERATOR::convert)
      .toList();
    ConfigUtil.putIfValuesPresent(target, "dependencies", depends);

    // collect the external dependencies
    var repositories = pluginData.externalDependencies().stream()
      .map(ExternalDependency::repository)
      .map(repository -> {
        var repositorySection = this.configFormat.createConfig();
        repositorySection.add("name", repository.id());
        repositorySection.add("url", repository.url());
        return repositorySection;
      })
      .toList();
    var dependencies = pluginData.externalDependencies().stream()
      .map(dep -> String.format("%s:%s:%s", dep.groupId(), dep.artifactId(), dep.version()))
      .toList();

    // put in the repos and dependencies
    var externalDependencies = ConfigUtil.tap(this.configFormat, config -> {
      ConfigUtil.putIfValuesPresent(config, "artifacts", dependencies);
      ConfigUtil.putIfValuesPresent(config, "repositories", repositories);
    });
    target.set("externalDependencies", externalDependencies);
  }
}
