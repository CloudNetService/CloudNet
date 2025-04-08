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

package eu.cloudnetservice.node.impl.service.defaults.config;

import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.node.service.CloudService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import lombok.NonNull;

@Singleton
public class NukkitConfigurationPreparer extends AbstractServiceConfigurationPreparer {

  @Inject
  public NukkitConfigurationPreparer(@NonNull ServiceTaskProvider taskProvider) {
    super(taskProvider);
  }

  @Override
  public void configure(@NonNull CloudService cloudService) {
    // check if we should run now
    if (this.shouldRewriteIp(cloudService)) {
      // copy the default file
      var configFile = cloudService.directory().resolve("server.properties");
      this.copyCompiledFile("files/nukkit/server.properties", configFile);
      // load the configuration
      var properties = new Properties();
      try (var stream = Files.newInputStream(configFile)) {
        properties.load(stream);
        // update the configuration
        properties.setProperty("server-ip", cloudService.serviceConfiguration().hostAddress());
        properties.setProperty("server-port", String.valueOf(cloudService.serviceConfiguration().port()));
        // store the properties
        try (var out = Files.newOutputStream(configFile)) {
          properties.store(out, "Properties Config file - edited by CloudNet");
        }
      } catch (IOException exception) {
        LOGGER.error("Unable to edit server.properties in {}", cloudService.directory(), exception);
      }
    }
  }
}
