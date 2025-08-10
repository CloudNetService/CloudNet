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

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.wrapper.event.ServiceInfoPropertiesConfigureEvent;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fallback service info configure listener that just appends the bare minimum of information to the service properties.
 * Only in use when no version-specific bridge implementation is available.
 *
 * @since 4.0
 */
final class FabricFallbackEventListener {

  private final String serverVersion;
  private final BridgeServiceHelper bridgeServiceHelper;

  @Inject
  public FabricFallbackEventListener(@NonNull BridgeServiceHelper bridgeServiceHelper) {
    this.bridgeServiceHelper = bridgeServiceHelper;
    this.serverVersion = FabricLoader.getInstance().getRawGameVersion();
  }

  @EventListener
  public void onServicePropertiesConfigure(@NonNull ServiceInfoPropertiesConfigureEvent event) {
    var properties = event.propertyHolder();

    // base properties, see PlatformBridgeManagement.appendServiceInformation()
    properties.append("Online", Boolean.TRUE);
    properties.append("Motd", this.bridgeServiceHelper.motd().get());
    properties.append("Extra", this.bridgeServiceHelper.extra().get());
    properties.append("State", this.bridgeServiceHelper.state().get());
    properties.append("Max-Players", this.bridgeServiceHelper.maxPlayers().get());

    // extended properties, see e.g. BukkitBridgeManagement.appendServiceInformation()
    // the properties that are not available (such as player count) are not added to signal that
    properties.append("Version", this.serverVersion);
  }
}
