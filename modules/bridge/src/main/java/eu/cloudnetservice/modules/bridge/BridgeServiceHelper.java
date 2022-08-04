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
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The bridge service helper is designed to facilitate working with individual states of services. In addition, these
 * properties are stored here:
 * <ul>
 *   <li>{@link #MOTD}</li>
 *   <li>{@link #EXTRA}</li>
 *   <li>{@link #STATE}</li>
 *   <li>{@link #MAX_PLAYERS}</li>
 * </ul>
 *
 * @since 4.0
 */
public final class BridgeServiceHelper {

  public static final AtomicInteger MAX_PLAYERS = new AtomicInteger();

  public static final AtomicReference<String> MOTD = new AtomicReference<>("");
  public static final AtomicReference<String> EXTRA = new AtomicReference<>("");
  public static final AtomicReference<String> STATE = new AtomicReference<>("LOBBY");

  private BridgeServiceHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the state of the service this bridge instance is running on to {@code ingame} and starts a new service of the
   * task. The method is equivalent to calling {@code BridgeServiceHelper.changeToIngame(true)}.
   */
  public static void changeToIngame() {
    changeToIngame(true);
  }

  /**
   * Sets the state of the service this bridge instance is running on to {@code ingame}. A new service from the same
   * task is started if the given auto start option is true.
   * <p>
   * Note: The service is started asynchronously.
   *
   * @param autoStartService whether a new service should be started or not.
   */
  public static void changeToIngame(boolean autoStartService) {
    if (!STATE.getAndSet("INGAME").equalsIgnoreCase("ingame") && autoStartService) {
      // start a new service based on the task name
      var taskName = Wrapper.instance().serviceId().taskName();
      CloudNetDriver.instance().serviceTaskProvider()
        .serviceTaskAsync(taskName)
        .thenApply(task -> ServiceConfiguration.builder(task).build())
        .thenApply(config -> CloudNetDriver.instance().cloudServiceFactory().createCloudService(config))
        .thenAccept(createResult -> {
          if (createResult.state() == ServiceCreateResult.State.CREATED) {
            createResult.serviceInfo().provider().start();
          }
        });
    }
  }

  /**
   * Tries to guess the {@link ServiceInfoState} from the lifecycle and {@link BridgeServiceProperties} of the given
   * service.
   * <ol>
   *   <li>If the service is not running or not in-game {@link ServiceInfoState#STOPPED} is guessed.</li>
   *   <li>If the service is empty {@link ServiceInfoState#EMPTY_ONLINE} is guessed.</li>
   *   <li>If the service is full {@link ServiceInfoState#FULL_ONLINE} is guessed.</li>
   *   <li>If the service is starting {@link ServiceInfoState#STARTING} is guessed.</li>
   *   <li>If the service is just connected {@link ServiceInfoState#ONLINE} is guessed.</li>
   *   <li>If none of the above apply {@link ServiceInfoState#STOPPED} is used as fallback.</li>
   * </ol>
   *
   * @param service the service to guess the state of.
   * @return the guessed service info state.
   * @throws NullPointerException if the given service is null.
   * @see BridgeServiceProperties
   */
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
    }
    return ServiceInfoState.STOPPED;
  }

  /**
   * Replaces commonly used placeholders in the given input string using the given service as the information source. If
   * no service is given only the group property is replaced.
   *
   * @param value   the string to replace the placeholders in.
   * @param group   the group to replace {@literal %group%} with.
   * @param service the service to use as source for the placeholder values.
   * @return the String with the placeholders replaced.
   * @throws NullPointerException if the given input string is null.
   */
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

  /**
   * The service info state allows a better separation of a service state than a service lifecycle.
   *
   * @since 4.0
   */
  public enum ServiceInfoState {
    /**
     * This state represents a service that is stopped or does not match any other state.
     */
    STOPPED,
    /**
     * This state represents a service that is currently starting.
     */
    STARTING,
    /**
     * This state represents a service that is started and online but no players are connected to it.
     */
    EMPTY_ONLINE,
    /**
     * This state represents a service that is started, online and full.
     */
    FULL_ONLINE,
    /**
     * This state represents a service that is started and online but neither empty nor full.
     */
    ONLINE
  }
}
