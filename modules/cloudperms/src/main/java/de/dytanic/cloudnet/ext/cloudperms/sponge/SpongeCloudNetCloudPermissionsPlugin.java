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

package de.dytanic.cloudnet.ext.cloudperms.sponge;

import com.velocitypowered.api.plugin.Plugin;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;

@Plugin(
  id = "cloudnet_cloudperms",
  name = "CloudNet-CloudPerms",
  version = "1.0",
  description = "Sponge extension which implement the permission management system from CloudNet into Sponge for players",
  url = "https://cloudnetservice.eu"
)
public final class SpongeCloudNetCloudPermissionsPlugin {

  @Listener
  public void onEnable(ConstructPluginEvent event) {

  }
}
