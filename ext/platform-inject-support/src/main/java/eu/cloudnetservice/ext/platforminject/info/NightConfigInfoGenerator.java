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

package eu.cloudnetservice.ext.platforminject.info;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import eu.cloudnetservice.ext.platforminject.data.ParsedPluginData;
import java.io.IOException;
import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import lombok.NonNull;

public abstract class NightConfigInfoGenerator implements PluginInfoGenerator {

  private static final String TEMPLATE_FILE_NAME_FORMAT = "%s.template";

  protected final String platformFileName;
  protected final ConfigFormat<Config> configFormat;

  protected final ConfigWriter writer;
  protected final ConfigParser<Config> parser;

  protected NightConfigInfoGenerator(@NonNull ConfigFormat<Config> configFormat, @NonNull String platformFileName) {
    // provided values
    this.configFormat = configFormat;
    this.platformFileName = platformFileName;

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
    var fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", this.platformFileName);
    try (var writer = fileObject.openWriter()) {
      this.writer.write(targetConfig, writer);
    }
  }

  protected abstract void applyPlatformInfo(
    @NonNull Config target,
    @NonNull ParsedPluginData pluginData,
    @NonNull String platformMainClassName);

  protected @NonNull Config loadFileTemplateOrNewConfig(@NonNull Filer filer) {
    try {
      // open the resource & read the content
      var resource = filer.getResource(
        StandardLocation.CLASS_OUTPUT,
        "",
        String.format(TEMPLATE_FILE_NAME_FORMAT, this.platformFileName));
      try (var reader = resource.openReader(false)) {
        return this.parser.parse(reader);
      }
    } catch (IOException exception) {
      return this.configFormat.createConfig();
    }
  }
}
