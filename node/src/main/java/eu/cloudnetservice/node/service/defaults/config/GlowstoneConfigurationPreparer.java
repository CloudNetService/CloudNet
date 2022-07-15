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

package eu.cloudnetservice.node.service.defaults.config;

import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.service.CloudService;
import lombok.NonNull;

public class GlowstoneConfigurationPreparer extends AbstractServiceConfigurationPreparer {

  @Override
  public void configure(@NonNull Node nodeInstance, @NonNull CloudService cloudService) {
    // check if we should run now
    if (this.shouldRewriteIp(nodeInstance, cloudService)) {
      // copy the default file
      var configFile = cloudService.directory().resolve("config/glowstone.yml");
      this.copyCompiledFile("files/glowstone/glowstone.yml", configFile);
      // rewrite the configuration file
      this.rewriteFile(configFile, line -> {
        if (line.trim().startsWith("ip:")) {
          line = String.format("  ip: '%s'", cloudService.serviceInfo().address().host());
        } else if (line.trim().startsWith("port:")) {
          line = String.format("  port: %d", cloudService.serviceConfiguration().port());
        }
        return line;
      });
    }
  }
}
