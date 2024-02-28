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

package eu.cloudnetservice.node.service.defaults.config;

import com.electronwill.nightconfig.yaml.YamlFormat;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.node.service.CloudService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class GlowstoneConfigurationPreparer extends AbstractServiceConfigurationPreparer {

  @Inject
  public GlowstoneConfigurationPreparer(@NonNull ServiceTaskProvider taskProvider) {
    super(taskProvider);
  }

  @Override
  public void configure(@NonNull CloudService cloudService) {
    // check if we should run now
    if (this.shouldRewriteIp(cloudService)) {
      var configFile = cloudService.directory().resolve("config/glowstone.yml");
      try (var config = this.loadConfig(configFile, YamlFormat.defaultInstance(), "files/glowstone/glowstone.yml")) {
        // rewrite ip and port
        config.set("server.ip", cloudService.serviceConfiguration().hostAddress());
        config.set("server.port", cloudService.serviceConfiguration().port());

        // flush the changes
        config.save();
      }
    }
  }
}
