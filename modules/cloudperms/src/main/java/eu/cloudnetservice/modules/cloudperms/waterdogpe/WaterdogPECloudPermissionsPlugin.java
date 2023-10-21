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

package eu.cloudnetservice.modules.cloudperms.waterdogpe;

import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "waterdog",
  name = "CloudNet-CloudPerms",
  authors = "CloudNetService",
  version = "@version@"
)
public class WaterdogPECloudPermissionsPlugin implements PlatformEntrypoint {

  private final ModuleHelper moduleHelper;

  @Inject
  public WaterdogPECloudPermissionsPlugin(
    @NonNull ModuleHelper moduleHelper,
    @NonNull WaterdogPECloudPermissionsPlayerListener $ // can ignore, on init the listeners we need are registered
  ) {
    this.moduleHelper = moduleHelper;
  }

  @Override
  public void onLoad() {
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
