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

package eu.cloudnetservice.modules.bridge;

import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The bridge service helper is designed to facilitate working with individual states of services. In addition, these
 * properties are stored here:
 * <ul>
 *   <li>{@link #motd()}</li>
 *   <li>{@link #extra()}</li>
 *   <li>{@link #state()}</li>
 *   <li>{@link #maxPlayers()}</li>
 * </ul>
 *
 * @since 4.0
 */
@Singleton
public final class BridgeServiceHelper {

  private final AtomicInteger maxPlayers = new AtomicInteger();

  private final AtomicReference<String> motd = new AtomicReference<>("");
  private final AtomicReference<String> extra = new AtomicReference<>("");
  private final AtomicReference<String> state = new AtomicReference<>("LOBBY");

  private final ServiceTaskProvider taskProvider;
  private final CloudServiceFactory serviceFactory;
  private final WrapperConfiguration wrapperConfiguration;

  @Inject
  public BridgeServiceHelper(
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull CloudServiceFactory serviceFactory,
    @NonNull WrapperConfiguration wrapperConfiguration
  ) {
    this.taskProvider = taskProvider;
    this.serviceFactory = serviceFactory;
    this.wrapperConfiguration = wrapperConfiguration;
  }

  /**
   * Tries to guess the {@link ServiceInfoState} from the lifecycle and {@link BridgeDocProperties} of the given
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
   * @see BridgeDocProperties
   */
  public static @NonNull ServiceInfoState guessStateFromServiceInfoSnapshot(@NonNull ServiceInfoSnapshot service) {
    // convert not running or ingame services to STOPPED
    if (service.lifeCycle() != ServiceLifeCycle.RUNNING || inGameService(service)) {
      return ServiceInfoState.STOPPED;
    }

    // check if the service is empty
    if (emptyService(service)) {
      return ServiceInfoState.EMPTY_ONLINE;
    }

    // check if the service is full
    if (fullService(service)) {
      return ServiceInfoState.FULL_ONLINE;
    }

    // check if the service is starting
    if (startingService(service)) {
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
    value = value.replace(
      "%cpu_usage%",
      String.format("%.2f", service.processSnapshot().cpuUsage()));

    // bridge information
    var online = service.readProperty(BridgeDocProperties.IS_ONLINE);
    value = value.replace("%online%", online ? "Online" : "Offline");

    // make sure that the bridge is loaded before accessing any of the properties
    if (online) {
      value = value.replace(
        "%online_players%",
        Integer.toString(service.readProperty(BridgeDocProperties.ONLINE_COUNT)));
      value = value.replace(
        "%max_players%",
        Integer.toString(service.readProperty(BridgeDocProperties.MAX_PLAYERS)));
      value = value.replace("%motd%", service.readProperty(BridgeDocProperties.MOTD));
      value = value.replace("%extra%", service.readProperty(BridgeDocProperties.EXTRA));
      value = value.replace("%state%", service.readProperty(BridgeDocProperties.STATE));
      value = value.replace("%version%", service.readProperty(BridgeDocProperties.VERSION));
    }
    // done
    return value;
  }

  /**
   * Checks if the given service is empty. This is only the case if all following conditions apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is connected</li>
   *   <li>the service is marked as online {@link BridgeDocProperties#IS_ONLINE}</li>
   *   <li>the service has a present online count as it is 0</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is empty, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  public static boolean emptyService(@NonNull ServiceInfoSnapshot service) {
    return service.connected()
      && service.readProperty(BridgeDocProperties.IS_ONLINE)
      && service.readProperty(BridgeDocProperties.ONLINE_COUNT) == 0;
  }

  /**
   * Checks if the given service is full. This is only the case if all following conditions apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is connected</li>
   *   <li>the service is marked as online {@link BridgeDocProperties#IS_ONLINE}</li>
   *   <li>the service has both an online and max player count</li>
   *   <li>the online count is equal or higher than the max player count</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is full, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  public static boolean fullService(@NonNull ServiceInfoSnapshot service) {
    return service.connected()
      && service.readProperty(BridgeDocProperties.IS_ONLINE)
      && service.readProperty(BridgeDocProperties.ONLINE_COUNT)
      >= service.readProperty(BridgeDocProperties.MAX_PLAYERS);
  }

  /**
   * Checks if the given service is starting. This is only the case if all following conditions apply:
   * <ul>
   *  <li>the lifecycle is running</li>
   *  <li>the service is <strong>NOT</strong> marked as online {@link BridgeDocProperties#IS_ONLINE}</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is starting, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  public static boolean startingService(@NonNull ServiceInfoSnapshot service) {
    return service.lifeCycle() == ServiceLifeCycle.RUNNING && !service.readProperty(BridgeDocProperties.IS_ONLINE);
  }

  /**
   * Checks if the given service is in-game. This is only the case if all following conditions apply:
   *  <ul>
   *    <li>the lifecycle is running</li>
   *    <li>the service is connected</li>
   *    <li>the service is marked as online {@link BridgeDocProperties#IS_ONLINE}</li>
   *    <li>the motd, the state or the extra matches any value representing the in-game state {@link #matchesInGameString(String)}</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is in-game, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  public static boolean inGameService(@NonNull ServiceInfoSnapshot service) {
    return service.lifeCycle() == ServiceLifeCycle.RUNNING
      && service.connected()
      && service.readProperty(BridgeDocProperties.IS_ONLINE)
      && (matchesInGameString(service.readProperty(BridgeDocProperties.MOTD))
      || matchesInGameString(service.readProperty(BridgeDocProperties.EXTRA))
      || matchesInGameString(service.readProperty(BridgeDocProperties.STATE)));
  }

  /**
   * Checks if the given value matches either {@code ingame}, {@code running} or {@code playing}.
   *
   * @param value the value to check against.
   * @return true if the value contains an in-game indicator, false otherwise.
   */
  private static boolean matchesInGameString(@Nullable String value) {
    if (value == null) {
      // value is not present
      return false;
    } else {
      // value is present, check if the string contains one of the ingame string values
      var loweredValue = value.toLowerCase(Locale.ROOT);
      return loweredValue.contains("ingame") || loweredValue.contains("running") || loweredValue.contains("playing");
    }
  }

  /**
   * Sets the state of the service this bridge instance is running on to {@code ingame} and starts a new service of the
   * task. The method is equivalent to calling {@code BridgeServiceHelper.changeToIngame(true)}.
   */
  public void changeToIngame() {
    this.changeToIngame(true);
  }

  public @NonNull AtomicInteger maxPlayers() {
    return this.maxPlayers;
  }

  public @NonNull AtomicReference<String> motd() {
    return this.motd;
  }

  public @NonNull AtomicReference<String> extra() {
    return this.extra;
  }

  public @NonNull AtomicReference<String> state() {
    return this.state;
  }

  /**
   * Sets the state of the service this bridge instance is running on to {@code ingame}. A new service from the same
   * task is started if the given auto start option is true.
   * <p>
   * Note: The service is started asynchronously.
   *
   * @param autoStartService whether a new service should be started or not.
   */
  public void changeToIngame(boolean autoStartService) {
    if (!this.state.getAndSet("INGAME").equalsIgnoreCase("ingame") && autoStartService) {
      // start a new service based on the task name
      var taskName = this.wrapperConfiguration.serviceConfiguration().serviceId().taskName();
      this.taskProvider
        .serviceTaskAsync(taskName)
        .thenApply(task -> ServiceConfiguration.builder(task).build())
        .thenApply(this.serviceFactory::createCloudService)
        .thenAccept(createResult -> {
          if (createResult.state() == ServiceCreateResult.State.CREATED) {
            createResult.serviceInfo().provider().start();
          }
        });
    }
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
