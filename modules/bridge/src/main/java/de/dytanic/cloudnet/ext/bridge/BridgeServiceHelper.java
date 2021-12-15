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

import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BridgeServiceHelper {

  public static final AtomicInteger MAX_PLAYERS = new AtomicInteger();

  public static final AtomicReference<String> MOTD = new AtomicReference<>("");
  public static final AtomicReference<String> EXTRA = new AtomicReference<>("");
  public static final AtomicReference<String> STATE = new AtomicReference<>("LOBBY");

  private BridgeServiceHelper() {
    throw new UnsupportedOperationException();
  }

  public static void changeToIngame() {
    changeToIngame(true);
  }

  public static void changeToIngame(boolean autoStartService) {
    if (!STATE.getAndSet("INGAME").equalsIgnoreCase("ingame") && autoStartService) {
      // start a new service based on the task name
      var taskName = Wrapper.getInstance().getServiceId().getTaskName();
      CloudNetDriver.getInstance().getServiceTaskProvider()
        .getServiceTaskAsync(taskName)
        .map(task -> ServiceConfiguration.builder(task).build())
        .map(config -> CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(config))
        .onComplete(service -> service.provider().start());
    }
  }

  public static @NotNull ServiceInfoState guessStateFromServiceInfoSnapshot(@NotNull ServiceInfoSnapshot service) {
    // convert not running or ingame services to STOPPED
    if (service.getLifeCycle() != ServiceLifeCycle.RUNNING
      || service.getProperty(BridgeServiceProperties.IS_IN_GAME).orElse(false)) {
      return ServiceInfoState.STOPPED;
    }
    // check if the service is empty
    if (service.getProperty(BridgeServiceProperties.IS_EMPTY).orElse(false)) {
      return ServiceInfoState.EMPTY_ONLINE;
    }
    // check if the service is full
    if (service.getProperty(BridgeServiceProperties.IS_FULL).orElse(false)) {
      return ServiceInfoState.FULL_ONLINE;
    }
    // check if the service is starting
    if (service.getProperty(BridgeServiceProperties.IS_STARTING).orElse(false)) {
      return ServiceInfoState.STARTING;
    }
    // check if the service is connected
    if (service.isConnected()) {
      return ServiceInfoState.ONLINE;
    } else {
      return ServiceInfoState.STOPPED;
    }
  }

  public static @NotNull String fillCommonPlaceholders(
    @NotNull String value,
    @Nullable String group,
    @Nullable ServiceInfoSnapshot service
  ) {
    value = value.replace("%group%", group == null ? "" : group);
    // stop replacing if no service is given
    if (service == null) {
      return value;
    }
    // replace all service id placeholders
    value = value.replace("%name%", service.getServiceId().name());
    value = value.replace("%task%", service.getServiceId().getTaskName());
    value = value.replace("%node%", service.getServiceId().getNodeUniqueId());
    value = value.replace("%unique_id%", service.getServiceId().getUniqueId().toString());
    value = value.replace("%environment%", service.getServiceId().getEnvironment().name());
    value = value.replace("%task_id%", Integer.toString(service.getServiceId().getTaskServiceId()));
    value = value.replace("%uid%", service.getServiceId().getUniqueId().toString().split("-")[0]);
    // general service information
    value = value.replace("%life_cycle%", service.getLifeCycle().name());
    value = value.replace("%runtime%", service.getConfiguration().getRuntime());
    value = value.replace("%port%", Integer.toString(service.getConfiguration().getPort()));
    // process information
    value = value.replace("%pid%", Long.toString(service.getProcessSnapshot().pid()));
    value = value.replace("%threads%", Integer.toString(service.getProcessSnapshot().threads().size()));
    value = value.replace("%heap_usage%", Long.toString(service.getProcessSnapshot().heapUsageMemory()));
    value = value.replace("%max_heap_usage%", Long.toString(service.getProcessSnapshot().maxHeapMemory()));
    value = value.replace("%cpu_usage%", CPUUsageResolver.FORMAT.format(service.getProcessSnapshot().cpuUsage()));
    // bridge information
    value = value.replace("%online%",
      BridgeServiceProperties.IS_ONLINE.get(service).orElse(false) ? "Online" : "Offline");
    value = value.replace("%online_players%",
      Integer.toString(BridgeServiceProperties.ONLINE_COUNT.get(service).orElse(0)));
    value = value.replace("%max_players%",
      Integer.toString(BridgeServiceProperties.MAX_PLAYERS.get(service).orElse(0)));
    value = value.replace("%motd%", BridgeServiceProperties.MOTD.get(service).orElse(""));
    value = value.replace("%extra%", BridgeServiceProperties.EXTRA.get(service).orElse(""));
    value = value.replace("%state%", BridgeServiceProperties.STATE.get(service).orElse(""));
    value = value.replace("%version%", BridgeServiceProperties.VERSION.get(service).orElse(""));
    // done
    return value;
  }

  public static @NotNull NetworkServiceInfo createServiceInfo(@NotNull ServiceInfoSnapshot snapshot) {
    return new NetworkServiceInfo(snapshot.getConfiguration().getGroups(), snapshot.getServiceId());
  }

  public enum ServiceInfoState {
    STOPPED,
    STARTING,
    EMPTY_ONLINE,
    FULL_ONLINE,
    ONLINE
  }
}
