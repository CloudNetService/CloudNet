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

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.node.service.CloudService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;

@Singleton
public class BungeeConfigurationPreparer extends AbstractServiceConfigurationPreparer {

  @Inject
  public BungeeConfigurationPreparer(@NonNull ServiceTaskProvider taskProvider) {
    super(taskProvider);
  }

  @Override
  public void configure(@NonNull CloudService cloudService) {
    // check if we should run now
    if (this.shouldRewriteIp(cloudService)) {
      var configFile = cloudService.directory().resolve("config.yml");
      try (var config = this.loadConfig(configFile, YamlFormat.defaultInstance(), "files/bungee/config.yml")) {
        List<Config> listeners = config.get("listeners");
        // get the first registered listeners - editing all of them will result in bungee start failures
        // but removing other entries might break setups...
        var firstListener = Iterables.getFirst(listeners, null);
        Objects.requireNonNull(
          firstListener,
          "No listeners configured in bungee config - please fix your configuration!");

        // edit the listener and re-set the config entry
        firstListener.set("host", String.format(
          "%s:%d",
          cloudService.serviceConfiguration().hostAddress(),
          cloudService.serviceConfiguration().port()));
        config.set("listeners", listeners);

        // flush the changes
        config.save();
      }
    }
  }
}
