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

package eu.cloudnetservice.ext.platforminject.processor.infogen;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.generator.PluginInfoGenerator;
import eu.cloudnetservice.ext.platforminject.processor.util.ResourceUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import lombok.NonNull;

public abstract class NightConfigInfoGenerator implements PluginInfoGenerator {

  private static final String TEMPLATE_FILE_NAME_FORMAT = "%s.template";

  protected final Set<String> platformFileNames;
  protected final ConfigFormat<Config> configFormat;

  protected final ConfigWriter writer;
  protected final ConfigParser<Config> parser;

  protected NightConfigInfoGenerator(@NonNull ConfigFormat<Config> configFormat, @NonNull String... platformFileNames) {
    // assert that at least one file name is given
    Objects.checkIndex(0, platformFileNames.length);

    // provided values
    this.configFormat = configFormat;
    this.platformFileNames = Set.of(platformFileNames);

    // create writer and parser once
    this.writer = configFormat.createWriter();
    this.parser = configFormat.createParser();
  }

  @Override
  public void generatePluginInfo(
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName,
    @NonNull Filer filer
  ) throws IOException {
    // get the template config or create a new config & emit the platform data to it
    var targetConfig = this.loadFileTemplateOrNewConfig(filer);
    this.applyPlatformInfo(targetConfig, pluginData, platformMainClassName);

    // write the platform data
    var fileNames = pluginData.pluginFileNames().isEmpty() ? this.platformFileNames : pluginData.pluginFileNames();
    for (var platformFileName : fileNames) {
      var fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", platformFileName);
      try (var writer = fileObject.openWriter()) {
        this.writer.write(targetConfig, writer);
      }
    }
  }

  protected abstract void applyPlatformInfo(
    @NonNull Config target,
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName);

  protected @NonNull Config loadFileTemplateOrNewConfig(@NonNull Filer filer) {
    for (var fileNameCandidate : this.platformFileNames) {
      // find the expected template file in the resources directory
      var templateFileName = String.format(TEMPLATE_FILE_NAME_FORMAT, fileNameCandidate);
      var templateFile = ResourceUtil.resolveResource(filer, templateFileName);

      // check if the template file exists & load it if it does
      if (templateFile != null) {
        try (var reader = Files.newBufferedReader(templateFile, StandardCharsets.UTF_8)) {
          return this.parser.parse(reader);
        } catch (IOException ignored) {
        }
      }
    }

    // create a new empty file, the template file doesn't exist
    return this.configFormat.createConfig();
  }
}
