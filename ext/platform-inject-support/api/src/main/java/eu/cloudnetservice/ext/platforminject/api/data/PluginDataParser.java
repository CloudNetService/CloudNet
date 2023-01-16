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

package eu.cloudnetservice.ext.platforminject.api.data;

import eu.cloudnetservice.ext.platforminject.api.stereotype.Command;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ExternalDependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class PluginDataParser {

  public static final int PLUGIN_COMMANDS = 0x01;
  public static final int PLUGIN_DEPENDENCIES = 0x02;
  public static final int EXTERNAL_DEPENDENCIES = 0x04;
  public static final int EXTERNAL_REPOSITORIES = 0x08;

  private int supportedPlatformComponents;

  private PluginDataParser() {
  }

  public static @NonNull PluginDataParser create() {
    return new PluginDataParser();
  }

  private static @NonNull String validateName(@NonNull String name, @NonNull String type) {
    if (name.isBlank()) {
      throw new IllegalArgumentException("Invalid name for " + type + ": \"" + name + "\" must not be blank");
    }
    return name;
  }

  private static @Nullable String normalizeValue(@NonNull String value) {
    return value.isBlank() ? null : value;
  }

  public @NonNull PluginDataParser enableSupport(int flag) {
    this.supportedPlatformComponents |= flag;
    return this;
  }

  public @NonNull ParsedPluginData parseAndValidateData(
    @NonNull PlatformPlugin plugin,
    @Nullable String listener
  ) {
    // these are required
    var name = validateName(plugin.name(), "name");
    var version = validateName(plugin.version(), "version");

    // non required values
    var api = normalizeValue(plugin.api());
    var homepage = normalizeValue(plugin.homepage());
    var desc = normalizeValue(plugin.description());

    // validate & convert arrays to collections
    List<String> authors = Arrays.stream(plugin.authors()).filter(s -> !s.isBlank()).distinct().toList();
    List<Dependency> deps = this.hasFlag(PLUGIN_DEPENDENCIES)
      ? Arrays.stream(plugin.dependencies()).filter(dep -> !dep.name().isBlank()).toList()
      : List.of();
    List<Command> commands = this.hasFlag(PLUGIN_COMMANDS)
      ? Arrays.stream(plugin.commands()).filter(cmd -> !cmd.name().isBlank()).toList()
      : List.of();
    List<String> pluginFileNames = Arrays.stream(plugin.pluginFileNames()).filter(fileName -> !name.isBlank()).toList();

    // check if external dependencies are supported, no need to check them if not
    if (!this.hasFlag(EXTERNAL_DEPENDENCIES)) {
      return new ParsedPluginData(
        name,
        version,
        api,
        desc,
        homepage,
        listener,
        authors,
        commands,
        pluginFileNames,
        deps,
        List.of());
    }

    // validate the dependencies
    Collection<ExternalDependency> externalDeps = new HashSet<>();
    for (var dependency : plugin.externalDependencies()) {
      // check the name, artifact id and version
      validateName(dependency.version(), "dependency version");
      validateName(dependency.groupId(), "dependency group id");
      validateName(dependency.artifactId(), "dependency artifact id");

      // validate the repository (if supported)
      if (this.hasFlag(EXTERNAL_REPOSITORIES)) {
        validateName(dependency.repository().id(), "repository id");
        validateName(dependency.repository().url(), "repository url");
      }

      // dependency is ok
      externalDeps.add(dependency);
    }

    // build the data
    return new ParsedPluginData(
      name,
      version,
      api,
      desc,
      homepage,
      listener,
      authors,
      commands,
      pluginFileNames,
      deps,
      externalDeps);
  }

  private boolean hasFlag(int flag) {
    return (this.supportedPlatformComponents & flag) != 0;
  }
}
