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

package eu.cloudnetservice.plugins.luckperms;

import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.luckperms.api.LuckPermsProvider;

@Singleton
@PlatformPlugin(
  platform = "waterdog",
  name = "CloudNet-LuckPerms",
  version = "@version@",
  authors = "CloudNetService",
  pluginFileNames = "waterdog.yml",
  dependencies = @Dependency(name = "LuckPerms"),
  description = "Brings LuckPerms support to all server platforms"
)
public class WaterDogLuckPermsPlugin implements PlatformEntrypoint {

  private final CloudNetContextCalculator cloudNetContextCalculator;

  @Inject
  public WaterDogLuckPermsPlugin(@NonNull CloudNetContextCalculator cloudNetContextCalculator) {
    this.cloudNetContextCalculator = cloudNetContextCalculator;
  }

  @Override
  public void onLoad() {
    LuckPermsProvider.get().getContextManager().registerCalculator(this.cloudNetContextCalculator);
  }

}
