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

package de.dytanic.cloudnet.ext.bridge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

public class OnlyProxyProtection {

  public final Multimap<UUID, String> proxyIpAddress = ArrayListMultimap.create();

  private final boolean serverOnlineMode;
  private final ServiceEnvironmentType environmentType = Wrapper.getInstance().getServiceId().getEnvironment();
  private boolean lastEnabledState = false;

  public OnlyProxyProtection(boolean serverOnlineMode) {
    this.serverOnlineMode = serverOnlineMode;
  }

  private boolean checkEnabledState() {
    BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

    boolean enabledState = !this.serverOnlineMode
      && bridgeConfiguration != null
      && bridgeConfiguration.isOnlyProxyProtection()
      && bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null
      && bridgeConfiguration.getExcludedOnlyProxyWalkableGroups().stream()
      .noneMatch(group -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(group));

    if (this.lastEnabledState != enabledState) {
      this.lastEnabledState = enabledState;

      if (enabledState) {
        CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().forEach(this::addProxyAddress);
        CloudNetDriver.getInstance().getEventManager().registerListener(this);
      } else {
        this.proxyIpAddress.clear();
        CloudNetDriver.getInstance().getEventManager().unregisterListener(this);
      }
    }

    return enabledState;
  }

  public boolean shouldDisallowPlayer(String playerAddress) {
    return this.checkEnabledState() && !this.proxyIpAddress.containsValue(playerAddress);
  }

  private void addProxyAddress(ServiceInfoSnapshot proxySnapshot) {
    if (proxySnapshot.getServiceId().getEnvironment().isMinecraftJavaProxy() && this.environmentType
      .isMinecraftJavaServer()
      || proxySnapshot.getServiceId().getEnvironment().isMinecraftBedrockProxy() && this.environmentType
      .isMinecraftBedrockServer()) {
      try {
        InetAddress proxyAddress = InetAddress.getByName(proxySnapshot.getAddress().getHost());
        InetAddress proxyConnectAddress = InetAddress.getByName(proxySnapshot.getConnectAddress().getHost());

        if (proxyAddress.isAnyLocalAddress() && proxyConnectAddress.isAnyLocalAddress()) {
          CloudNetDriver.getInstance().getLogger().warning(
            String.format(
              "[OnlyProxyJoin] Proxy %s is bound on a wildcard address and connects users to a wildcard address! " +
                "This might cause issues with OnlyProxyJoin, please set either the 'hostAddress'- or 'connectHostAddress'-property "
                +
                "to a static address. This can be done In the config.json file of Node %s.",
              proxySnapshot.getName(), proxySnapshot.getServiceId().getNodeUniqueId()
            )
          );
        }

        this.proxyIpAddress.putAll(proxySnapshot.getServiceId().getUniqueId(),
          Arrays.asList(proxyAddress.getHostAddress(), proxyConnectAddress.getHostAddress()));
      } catch (UnknownHostException exception) {
        exception.printStackTrace();
      }
    }
  }

  @EventListener
  public void handleServiceStart(CloudServiceStartEvent event) {
    this.addProxyAddress(event.getServiceInfo());
  }

  @EventListener
  public void handleServiceStop(CloudServiceStopEvent event) {
    this.proxyIpAddress.removeAll(event.getServiceInfo().getServiceId().getUniqueId());
  }

  public Multimap<UUID, String> getProxyIpAddress() {
    return this.proxyIpAddress;
  }

}
