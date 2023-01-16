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

package eu.cloudnetservice.modules.bridge;

import static eu.cloudnetservice.driver.service.property.JsonServiceProperty.createFromClass;
import static eu.cloudnetservice.driver.service.property.JsonServiceProperty.createFromType;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.property.FunctionalServiceProperty;
import eu.cloudnetservice.driver.service.property.ServiceProperty;
import eu.cloudnetservice.driver.service.property.TransformingServiceProperty;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import java.util.Collection;
import lombok.NonNull;

/**
 * The bridge service properties are there to read any properties set by the bridge module. It is important to note that
 * not all values are set by default and most values cannot be overridden. For these values to be set, the bridge must
 * run as a plugin on the respective service.
 *
 * @since 4.0
 */
public final class BridgeServiceProperties {

  /**
   * This service property reads the online player count of any given {@link ServiceInfoSnapshot}. The property is only
   * updated after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final ServiceProperty<Integer> ONLINE_COUNT = createFromClass("Online-Count", Integer.class)
    .forbidModification();
  /**
   * This service property reads the max player count of any given {@link ServiceInfoSnapshot}. The property is only
   * updated after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final ServiceProperty<Integer> MAX_PLAYERS = createFromClass("Max-Players", Integer.class)
    .forbidModification();
  /**
   * This service property reads the version of any given {@link ServiceInfoSnapshot}. The property is only updated
   * after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final ServiceProperty<String> VERSION = createFromClass("Version", String.class).forbidModification();
  /**
   * This service property reads the motd of any given {@link ServiceInfoSnapshot}. The property is only updated after
   * the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   * <p>
   * Setting the state is possible using {@link BridgeServiceHelper#motd()} on the service itself.
   */
  public static final ServiceProperty<String> MOTD = createFromClass("Motd", String.class).forbidModification();
  /**
   * This service property reads the extra value of any given {@link ServiceInfoSnapshot}. The property is only updated
   * after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   * <p>
   * Setting the state is possible using {@link BridgeServiceHelper#extra()} on the service itself.
   */
  public static final ServiceProperty<String> EXTRA = createFromClass("Extra", String.class).forbidModification();
  /**
   * This service property reads the state of any given {@link ServiceInfoSnapshot}. The property is only updated after
   * the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   * <p>
   * Setting the state is possible using {@link BridgeServiceHelper#state()} on the service itself.
   */
  public static final ServiceProperty<String> STATE = createFromClass("State", String.class).forbidModification();
  /**
   * This service property reads the online property of any given {@link ServiceInfoSnapshot}. This property is always
   * true after the bridge plugin was enabled. The property is only updated after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final ServiceProperty<Boolean> IS_ONLINE = createFromClass("Online", Boolean.class)
    .forbidModification();
  /**
   * This service property checks if a service is considered being in-game. This is only the case if all following
   * conditions apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is connected</li>
   *   <li>the service is marked as online {@link #IS_ONLINE}</li>
   *   <li>the motd, the state or the extra matches any value representing the in-game state {@link #matchesInGameString(String)}</li>
   * </ul>
   * Note: Writing to this property is not allowed it results in an {@link UnsupportedOperationException}.
   * The property is only updated after the service itself was updated.
   */
  public static final ServiceProperty<Boolean> IS_IN_GAME = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::inGameService);
  /**
   * This service property checks if a service is considered starting. This is only the case if all following conditions
   * apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is <strong>NOT</strong> marked as online {@link #IS_ONLINE}</li>
   * </ul>
   * Note: Writing to this property is not allowed it results in an {@link UnsupportedOperationException}.
   * The property is only updated after the service itself was updated.
   */
  public static final ServiceProperty<Boolean> IS_STARTING = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::startingService);
  /**
   * This service property checks if a service is considered being empty. This is only the case if all following
   * conditions apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is connected</li>
   *   <li>the service is marked as online {@link #IS_ONLINE}</li>
   *   <li>the service has a present online count and it is 0</li>
   * </ul>
   * Note: Writing to this property is not allowed it results in an {@link UnsupportedOperationException}.
   * The property is only updated after the service itself was updated.
   */
  public static final ServiceProperty<Boolean> IS_EMPTY = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::emptyService);
  /**
   * This service property checks if a service is considered being full. This is only the case if all following
   * conditions apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is connected</li>
   *   <li>the service is marked as online {@link #IS_ONLINE}</li>
   *   <li>the service has both an online and max player count</li>
   *   <li>the online count is equal or higher than the max player count</li>
   * </ul>
   * Note: Writing to this property is not allowed it results in an {@link UnsupportedOperationException}.
   * The property is only updated after the service itself was updated.
   */
  public static final ServiceProperty<Boolean> IS_FULL = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::fullService);
  /**
   * This service property allows accessing all players that are connected to the given service. The property is only
   * updated after the service itself was updated.
   * <p>
   * Note: Writing to this property is not allowed it results in an {@link UnsupportedOperationException}.
   */
  public static final ServiceProperty<Collection<ServicePlayer>> PLAYERS = TransformingServiceProperty
    .<Collection<JsonDocument>, Collection<ServicePlayer>>wrap(
      createFromType("Players", new TypeToken<Collection<JsonDocument>>() {
      }.getType()))
    .modifyGet(($, documents) -> documents.stream().map(ServicePlayer::new).toList());

  private BridgeServiceProperties() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if the given service is empty. This is only the case if all following conditions apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is connected</li>
   *   <li>the service is marked as online {@link #IS_ONLINE}</li>
   *   <li>the service has a present online count as it is 0</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is empty, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  private static boolean emptyService(@NonNull ServiceInfoSnapshot service) {
    return service.connected()
      && service.propertyOr(IS_ONLINE, false)
      && service.propertyOr(ONLINE_COUNT, -1) == 0;
  }

  /**
   * Checks if the given service is full. This is only the case if all following conditions apply:
   * <ul>
   *   <li>the lifecycle is running</li>
   *   <li>the service is connected</li>
   *   <li>the service is marked as online {@link #IS_ONLINE}</li>
   *   <li>the service has both an online and max player count</li>
   *   <li>the online count is equal or higher than the max player count</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is full, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  private static boolean fullService(@NonNull ServiceInfoSnapshot service) {
    return service.connected()
      && service.propertyOr(IS_ONLINE, false)
      && service.propertyOr(ONLINE_COUNT, -1) >= service.propertyOr(MAX_PLAYERS, 0);
  }

  /**
   * Checks if the given service is starting. This is only the case if all following conditions apply:
   * <ul>
   *  <li>the lifecycle is running</li>
   *  <li>the service is <strong>NOT</strong> marked as online {@link #IS_ONLINE}</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is starting, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  private static boolean startingService(@NonNull ServiceInfoSnapshot service) {
    return service.lifeCycle() == ServiceLifeCycle.RUNNING && !service.propertyOr(IS_ONLINE, false);
  }

  /**
   * Checks if the given service is in-game. This is only the case if all following conditions apply:
   *  <ul>
   *    <li>the lifecycle is running</li>
   *    <li>the service is connected</li>
   *    <li>the service is marked as online {@link #IS_ONLINE}</li>
   *    <li>the motd, the state or the extra matches any value representing the in-game state {@link #matchesInGameString(String)}</li>
   * </ul>
   *
   * @param service the service to check.
   * @return true if the service is in-game, false otherwise.
   * @throws NullPointerException if the given service is null.
   */
  private static boolean inGameService(@NonNull ServiceInfoSnapshot service) {
    return service.lifeCycle() == ServiceLifeCycle.RUNNING && service.connected()
      && service.propertyOr(IS_ONLINE, false)
      && (matchesInGameString(service.propertyOr(MOTD, ""))
      || matchesInGameString(service.propertyOr(EXTRA, ""))
      || matchesInGameString(service.propertyOr(STATE, "")));
  }

  /**
   * Checks if the given value matches either {@code ingame}, {@code running} or {@code playing}.
   *
   * @param value the value to check against.
   * @return true if the value contains an in-game indicator, false otherwise.
   * @throws NullPointerException if the given value is null.
   */
  private static boolean matchesInGameString(@NonNull String value) {
    value = StringUtil.toLower(value);
    return value.contains("ingame") || value.contains("running") || value.contains("playing");
  }
}
