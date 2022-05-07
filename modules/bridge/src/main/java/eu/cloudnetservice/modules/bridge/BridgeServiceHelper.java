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

package eu.cloudnetservice.modules.bridge;

import eu.cloudnetservice.common.unsafe.CPUUsageResolver;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
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
      var taskName = Wrapper.instance().serviceId().taskName();
      CloudNetDriver.instance().serviceTaskProvider()
        .serviceTaskAsync(taskName)
        .thenApply(task -> ServiceConfiguration.builder(task).build())
        .thenApply(config -> CloudNetDriver.instance().cloudServiceFactory().createCloudService(config))
        .thenAccept(service -> service.provider().start());
    }
  }

  public static @NonNull ServiceInfoState guessStateFromServiceInfoSnapshot(@NonNull ServiceInfoSnapshot service) {
    // convert not running or ingame services to STOPPED
    if (service.lifeCycle() != ServiceLifeCycle.RUNNING
      || service.propertyOr(BridgeServiceProperties.IS_IN_GAME, false)) {
      return ServiceInfoState.STOPPED;
    }
    // check if the service is empty
    if (service.propertyOr(BridgeServiceProperties.IS_EMPTY, false)) {
      return ServiceInfoState.EMPTY_ONLINE;
    }
    // check if the service is full
    if (service.propertyOr(BridgeServiceProperties.IS_FULL, false)) {
      return ServiceInfoState.FULL_ONLINE;
    }
    // check if the service is starting
    if (service.propertyOr(BridgeServiceProperties.IS_STARTING, false)) {
      return ServiceInfoState.STARTING;
    }
    // check if the service is connected
    if (service.connected()) {
      return ServiceInfoState.ONLINE;
    } else {
      return ServiceInfoState.STOPPED;
    }
  }

  public static @NonNull String fillCommonPlaceholders(
    @NonNull String value,
    @Nullable String group,
    @Nullable ServiceInfoSnapshot service
  ) {
    value = value.replace("%group%", group == null ? "" : group);
    // stop replacing if no service is given
    if (service == null) {
      return value;
    }
    // replace all service id placeholders
    value = value.replace("%name%", service.serviceId().name());
    value = value.replace("%task%", service.serviceId().taskName());
    value = value.replace("%node%", service.serviceId().nodeUniqueId());
    value = value.replace("%unique_id%", service.serviceId().uniqueId().toString());
    value = value.replace("%environment%", service.serviceId().environment().name());
    value = value.replace("%task_id%", Integer.toString(service.serviceId().taskServiceId()));
    value = value.replace("%uid%", service.serviceId().uniqueId().toString().split("-")[0]);
    // general service information
    value = value.replace("%life_cycle%", service.lifeCycle().name());
    value = value.replace("%runtime%", service.configuration().runtime());
    value = value.replace("%port%", Integer.toString(service.configuration().port()));
    // process information
    value = value.replace("%pid%", Long.toString(service.processSnapshot().pid()));
    value = value.replace("%threads%", Integer.toString(service.processSnapshot().threads().size()));
    value = value.replace("%heap_usage%", Long.toString(service.processSnapshot().heapUsageMemory()));
    value = value.replace("%max_heap_usage%", Long.toString(service.processSnapshot().maxHeapMemory()));
    value = value.replace("%cpu_usage%", CPUUsageResolver.FORMAT.format(service.processSnapshot().cpuUsage()));
    // bridge information
    value = value.replace("%online%", BridgeServiceProperties.IS_ONLINE.readOr(service, false) ? "Online" : "Offline");
    value = value.replace("%online_players%",
      Integer.toString(BridgeServiceProperties.ONLINE_COUNT.readOr(service, 0)));
    value = value.replace("%max_players%",
      Integer.toString(BridgeServiceProperties.MAX_PLAYERS.readOr(service, 0)));
    value = value.replace("%motd%", BridgeServiceProperties.MOTD.readOr(service, ""));
    value = value.replace("%extra%", BridgeServiceProperties.EXTRA.readOr(service, ""));
    value = value.replace("%state%", BridgeServiceProperties.STATE.readOr(service, ""));
    value = value.replace("%version%", BridgeServiceProperties.VERSION.readOr(service, ""));
    // done
    return value;
  }

  public static @NonNull NetworkServiceInfo createServiceInfo(@NonNull ServiceInfoSnapshot snapshot) {
    return new NetworkServiceInfo(snapshot.configuration().groups(), snapshot.serviceId());
  }

  public enum ServiceInfoState {
    STOPPED,
    STARTING,
    EMPTY_ONLINE,
    FULL_ONLINE,
    ONLINE
  }
}
