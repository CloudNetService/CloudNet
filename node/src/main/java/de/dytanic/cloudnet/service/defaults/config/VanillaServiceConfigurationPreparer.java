/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.service.defaults.config;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.service.ICloudService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;

public class VanillaServiceConfigurationPreparer extends AbstractServiceConfigurationPreparer {

  @Override
  public void configure(@NotNull CloudNet nodeInstance, @NotNull ICloudService cloudService) {
    // copy the default file
    var configFile = cloudService.getDirectory().resolve("server.properties");
    this.copyCompiledFile("files/nms/server.properties", configFile);
    // load the configuration
    var properties = new Properties();
    try {
      // check if we need to update the server.properties
      if (this.shouldRewriteIp(nodeInstance, cloudService)) {
        try (var stream = Files.newInputStream(configFile)) {
          properties.load(stream);
          // update the configuration
          if (this.shouldRewriteIp(nodeInstance, cloudService)) {
            properties.setProperty("server-ip", nodeInstance.getConfig().getHostAddress());
            properties.setProperty("server-port", String.valueOf(cloudService.getServiceConfiguration().getPort()));
          }
          // store the properties
          try (var out = Files.newOutputStream(configFile)) {
            properties.store(out, "Minecraft server properties - edited by CloudNet");
          }
        }
      }
      // override the eula.txt
      properties.clear();
      properties.setProperty("eula", "true");
      // store the eula.txt
      try (var outputStream = Files.newOutputStream(cloudService.getDirectory().resolve("eula.txt"))) {
        properties.store(outputStream, "CloudNet auto eula (https://account.mojang.com/documents/minecraft_eula)");
      }
    } catch (IOException exception) {
      LOGGER.severe("Unable to edit server.properties or eula.txt in %s", exception, cloudService.getDirectory());
    }
  }
}
