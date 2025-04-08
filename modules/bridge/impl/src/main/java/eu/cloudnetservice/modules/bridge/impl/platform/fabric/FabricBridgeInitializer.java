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

package eu.cloudnetservice.modules.bridge.impl.platform.fabric;

import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "fabric",
  name = "CloudNet-Bridge",
  version = "@version@",
  dependencies = {
    @Dependency(name = "fabricloader", version = ">=0.16.6"),
    @Dependency(name = "minecraft", version = "~1.21"),
    @Dependency(name = "java", version = ">=23")
  },
  authors = "CloudNetService"
)
public final class FabricBridgeInitializer implements PlatformEntrypoint {

  private final ModuleHelper moduleHelper;

  @Inject
  public FabricBridgeInitializer(@NonNull ModuleHelper moduleHelper) {
    this.moduleHelper = moduleHelper;
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
