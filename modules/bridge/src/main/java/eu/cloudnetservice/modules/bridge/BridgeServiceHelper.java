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

import eu.cloudnetservice.common.resource.ResourceFormatter;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
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

  private final AtomicReference<Component> motd = new AtomicReference<>(Component.empty());
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
   * @param group   the group to replace {@literal <group>} with.
   * @param service the service to use as source for the placeholder values.
   * @return the String with the placeholders replaced.
   * @throws NullPointerException if the given input string is null.
   * @deprecated Use {#{@link #fillCommonPlaceholders(Map, String, ServiceInfoSnapshot)}} instead
   */
  @Deprecated
  public static @NonNull String fillCommonPlaceholders(
    @NonNull String value,
    @Nullable String group,
    @Nullable ServiceInfoSnapshot service
  ) {
    var placeholders = new HashMap<String, Component>();
    fillCommonPlaceholders(placeholders, group, service);
    for (var placeholder : placeholders.entrySet()) {
      value = value.replace("<" + placeholder.getKey() + ">", ComponentFormats.LEGACY_HEX.fromAdventure(placeholder.getValue()));
    }
    // done
    return value;
  }

  /**
   * Puts commonly used placeholders in the given input map using the given service as the information source. If
   * no service is given only the group property is replaced.
   *
   * @param placeholders the map to put the placeholders in.
   * @param group        the group for the {@literal group} placeholder.
   * @param service      the service to use as source for the placeholder values.
   * @throws NullPointerException if the given map is null.
   */
  public static void fillCommonPlaceholders(
    @NonNull Map<String, Component> placeholders,
    @Nullable String group,
    @Nullable ServiceInfoSnapshot service
  ) {
    placeholders.put("group", Component.text(group == null ? "" : group));

    // stop replacing if no service is given
    if (service == null) {
      return;
    }

    // put all service id placeholders
    placeholders.put("name", Component.text(service.serviceId().name()));
    placeholders.put("task", Component.text(service.serviceId().taskName()));
    placeholders.put("node", Component.text(service.serviceId().nodeUniqueId()));
    placeholders.put("unique_id", Component.text(service.serviceId().uniqueId().toString()));
    placeholders.put("environment", Component.text(service.serviceId().environment().name()));
    placeholders.put("task_id", Component.text(service.serviceId().taskServiceId()));
    placeholders.put("uid", Component.text(service.serviceId().uniqueId().toString().split("-")[0]));
    // general service information
    placeholders.put("life_cycle", Component.text(service.lifeCycle().name()));
    placeholders.put("runtime", Component.text(service.configuration().runtime()));
    placeholders.put("port", Component.text(service.configuration().port()));
    // process information
    placeholders.put("pid", Component.text(service.processSnapshot().pid()));
    placeholders.put("threads", Component.text(service.processSnapshot().threads().size()));
    placeholders.put("heap_usage", Component.text(service.processSnapshot().heapUsageMemory()));
    placeholders.put("max_heap_usage", Component.text(service.processSnapshot().maxHeapMemory()));
    placeholders.put(
      "cpu_usage",
      Component.text(ResourceFormatter.formatTwoDigitPrecision(service.processSnapshot().cpuUsage())));

    // bridge information
    var online = service.readProperty(BridgeDocProperties.IS_ONLINE);
    placeholders.put("online", Component.text(online ? "Online" : "Offline"));

    // make sure that the bridge is loaded before accessing any of the properties
    if (online) {
      placeholders.put(
        "online_players",
        Component.text(service.readProperty(BridgeDocProperties.ONLINE_COUNT)));
      placeholders.put(
        "max_players",
        Component.text(service.readProperty(BridgeDocProperties.MAX_PLAYERS)));
      placeholders.put("motd", ComponentFormats.JSON.toAdventure(service.readProperty(BridgeDocProperties.MOTD)));
      placeholders.put("extra", Component.text(service.readProperty(BridgeDocProperties.EXTRA)));
      placeholders.put("state", Component.text(service.readProperty(BridgeDocProperties.STATE)));
      placeholders.put("version", Component.text(service.readProperty(BridgeDocProperties.VERSION)));
    }
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
      && (matchesInGameString(ComponentFormats.PLAIN.fromAdventure(
        ComponentFormats.JSON.toAdventure(service.readProperty(BridgeDocProperties.MOTD))
      ))
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
      var loweredValue = StringUtil.toLower(value);
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

  public @NonNull AtomicReference<Component> motd() {
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
