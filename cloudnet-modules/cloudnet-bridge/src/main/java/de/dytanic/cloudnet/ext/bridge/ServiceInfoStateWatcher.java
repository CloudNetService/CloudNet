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

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ServiceInfoStateWatcher {

  protected final Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> services = new ConcurrentHashMap<>();

  public void includeExistingServices() {
    CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
      .filter(this::shouldWatchService)
      .forEach(serviceInfoSnapshot -> this
        .putService(serviceInfoSnapshot, this.fromServiceInfoSnapshot(serviceInfoSnapshot), false));
  }

  protected abstract void handleUpdate();

  protected abstract boolean shouldWatchService(ServiceInfoSnapshot serviceInfoSnapshot);

  protected abstract boolean shouldShowFullServices();

  private void putService(ServiceInfoSnapshot serviceInfoSnapshot, ServiceInfoState serviceInfoState) {
    this.putService(serviceInfoSnapshot, serviceInfoState, true);
  }

  private void putService(ServiceInfoSnapshot serviceInfoSnapshot, ServiceInfoState serviceInfoState,
    boolean fireUpdate) {
    if (!this.shouldWatchService(serviceInfoSnapshot)) {
      return;
    }

    this.services
      .put(serviceInfoSnapshot.getServiceId().getUniqueId(), new Pair<>(serviceInfoSnapshot, serviceInfoState));

    if (fireUpdate) {
      this.handleUpdate();
    }
  }

  @EventListener
  public void handle(CloudServiceRegisterEvent event) {
    this.putService(event.getServiceInfo(), ServiceInfoState.STOPPED);
  }

  @EventListener
  public void handle(CloudServiceStartEvent event) {
    this.putService(event.getServiceInfo(), ServiceInfoState.STARTING);
  }

  @EventListener
  public void handle(CloudServiceConnectNetworkEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();
    this.putService(serviceInfoSnapshot, this.fromServiceInfoSnapshot(serviceInfoSnapshot));
  }

  @EventListener
  public void handle(CloudServiceDisconnectNetworkEvent event) {
    this.putService(event.getServiceInfo(), ServiceInfoState.STOPPED);
  }

  @EventListener
  public void handle(CloudServiceInfoUpdateEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();
    this.putService(serviceInfoSnapshot, this.fromServiceInfoSnapshot(serviceInfoSnapshot));
  }

  @EventListener
  public void handle(CloudServiceUnregisterEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (!this.shouldWatchService(serviceInfoSnapshot)) {
      return;
    }

    this.services.remove(serviceInfoSnapshot.getServiceId().getUniqueId());
    this.handleUpdate();
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    this.putService(event.getServiceInfo(), ServiceInfoState.STOPPED);
  }

  private ServiceInfoState fromServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    if (serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING ||
      serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false)) {
      return ServiceInfoState.STOPPED;
    }

    if (serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_EMPTY).orElse(false)) {
      return ServiceInfoState.EMPTY_ONLINE;
    }

    if (serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_FULL).orElse(false)) {
      if (this.shouldShowFullServices()) {
        return ServiceInfoState.FULL_ONLINE;
      } else {
        return ServiceInfoState.STOPPED;
      }
    }

    if (serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_STARTING).orElse(false)) {
      return ServiceInfoState.STARTING;
    }

    if (serviceInfoSnapshot.isConnected()) {
      return ServiceInfoState.ONLINE;
    } else {
      return ServiceInfoState.STOPPED;
    }
  }

  protected String replaceServiceInfo(@NotNull String input, @Nullable String group,
    @Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
    input = input.replace("%group%", group == null ? "" : group);

    if (serviceInfoSnapshot == null) {
      return input;
    }

    input = input.replace("%task%", serviceInfoSnapshot.getServiceId().getTaskName());
    input = input.replace("%task_id%", String.valueOf(serviceInfoSnapshot.getServiceId().getTaskServiceId()));
    input = input.replace("%name%", serviceInfoSnapshot.getServiceId().getName());
    input = input.replace("%uuid%", serviceInfoSnapshot.getServiceId().getUniqueId().toString().split("-")[0]);
    input = input.replace("%node%", serviceInfoSnapshot.getServiceId().getNodeUniqueId());
    input = input.replace("%environment%", String.valueOf(serviceInfoSnapshot.getServiceId().getEnvironment()));
    input = input.replace("%life_cycle%", String.valueOf(serviceInfoSnapshot.getLifeCycle()));
    input = input.replace("%runtime%", serviceInfoSnapshot.getConfiguration().getRuntime());
    input = input.replace("%port%", String.valueOf(serviceInfoSnapshot.getConfiguration().getPort()));
    input = input.replace("%cpu_usage%",
      CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(serviceInfoSnapshot.getProcessSnapshot().getCpuUsage()));
    input = input.replace("%threads%", String.valueOf(serviceInfoSnapshot.getProcessSnapshot().getThreads().size()));

    input = input.replace("%online%",
      (serviceInfoSnapshot.getProperties().contains("Online") && serviceInfoSnapshot.getProperties()
        .getBoolean("Online")
        ? "Online" : "Offline"
      ));
    input = input
      .replace("%online_players%", String.valueOf(serviceInfoSnapshot.getProperties().getInt("Online-Count")));
    input = input.replace("%max_players%", String.valueOf(serviceInfoSnapshot.getProperties().getInt("Max-Players")));
    input = input.replace("%motd%", serviceInfoSnapshot.getProperties().getString("Motd", ""));
    input = input.replace("%extra%", serviceInfoSnapshot.getProperties().getString("Extra", ""));
    input = input.replace("%state%", serviceInfoSnapshot.getProperties().getString("State", ""));
    input = input.replace("%version%", serviceInfoSnapshot.getProperties().getString("Version", ""));
    input = input.replace("%whitelist%", (serviceInfoSnapshot.getProperties().contains("Whitelist-Enabled") &&
      serviceInfoSnapshot.getProperties().getBoolean("Whitelist-Enabled")
      ? "Enabled" : "Disabled"
    ));

    return input;
  }

  public Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> getServices() {
    return this.services;
  }

  public enum ServiceInfoState {
    STOPPED(0),
    STARTING(1),
    EMPTY_ONLINE(2),
    FULL_ONLINE(3),
    ONLINE(4);

    private final int priority;

    ServiceInfoState(int priority) {
      this.priority = priority;
    }

    public int getPriority() {
      return this.priority;
    }
  }

}
