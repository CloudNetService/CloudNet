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

import static eu.cloudnetservice.driver.service.property.JsonServiceProperty.createFromClass;
import static eu.cloudnetservice.driver.service.property.JsonServiceProperty.createFromType;

import com.google.gson.reflect.TypeToken;
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
 * Properties in ServiceInfos by the bridge module.
 */
public final class BridgeServiceProperties {

  /**
   * Property to get the online count of a service.
   */
  public static final ServiceProperty<Integer> ONLINE_COUNT = createFromClass("Online-Count", Integer.class)
    .forbidModification();
  /**
   * Property to get the max players of a service.
   */
  public static final ServiceProperty<Integer> MAX_PLAYERS = createFromClass("Max-Players", Integer.class)
    .forbidModification();
  /**
   * Property to get the Bukkit/Bungee/Nukkit/Velocity version of a service.
   */
  public static final ServiceProperty<String> VERSION = createFromClass("Version", String.class).forbidModification();
  /**
   * Property to get the Motd of a service.
   */
  public static final ServiceProperty<String> MOTD = createFromClass("Motd", String.class).forbidModification();
  /**
   * Property to get the Extra of a service.
   */
  public static final ServiceProperty<String> EXTRA = createFromClass("Extra", String.class).forbidModification();
  /**
   * Property to get the State of a service.
   */
  public static final ServiceProperty<String> STATE = createFromClass("State", String.class).forbidModification();
  /**
   * Property to check whether a service is online or not.
   */
  public static final ServiceProperty<Boolean> IS_ONLINE = createFromClass("Online", Boolean.class)
    .forbidModification();
  /**
   * Property to check whether a service is in game or not.
   */
  public static final ServiceProperty<Boolean> IS_IN_GAME = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::inGameService);
  /**
   * Property to check whether a service is starting or not.
   */
  public static final ServiceProperty<Boolean> IS_STARTING = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::startingService);
  /**
   * Property to check whether a service is empty (no players) or not.
   */
  public static final ServiceProperty<Boolean> IS_EMPTY = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::emptyService);
  /**
   * Property to check whether a service is full (online count &gt;= max players) or not.
   */
  public static final ServiceProperty<Boolean> IS_FULL = FunctionalServiceProperty.<Boolean>create()
    .reader(BridgeServiceProperties::fullService);
  /**
   * Property to get all online players on a service.
   */
  public static final ServiceProperty<Collection<ServicePlayer>> PLAYERS = TransformingServiceProperty
    .<Collection<JsonDocument>, Collection<ServicePlayer>>wrap(
      createFromType("Players", new TypeToken<Collection<JsonDocument>>() {
      }.getType()))
    .modifyGet(($, documents) -> documents.stream().map(ServicePlayer::new).toList());

  private BridgeServiceProperties() {
    throw new UnsupportedOperationException();
  }

  private static boolean emptyService(@NonNull ServiceInfoSnapshot service) {
    return service.connected()
      && service.property(IS_ONLINE).orElse(false)
      && service.property(ONLINE_COUNT).isPresent()
      && service.property(ONLINE_COUNT).orElse(0) == 0;
  }

  private static boolean fullService(@NonNull ServiceInfoSnapshot service) {
    return service.connected()
      && service.property(IS_ONLINE).orElse(false)
      && service.property(ONLINE_COUNT).isPresent()
      && service.property(MAX_PLAYERS).isPresent()
      && service.property(ONLINE_COUNT).orElse(0) >= service.property(MAX_PLAYERS).orElse(0);
  }

  private static boolean startingService(@NonNull ServiceInfoSnapshot service) {
    return service.lifeCycle() == ServiceLifeCycle.RUNNING && !service.property(IS_ONLINE).orElse(false);
  }

  private static boolean inGameService(@NonNull ServiceInfoSnapshot service) {
    return service.lifeCycle() == ServiceLifeCycle.RUNNING && service.connected()
      && service.property(IS_ONLINE).orElse(false)
      && (service.property(MOTD).map(BridgeServiceProperties::matchesInGameString).orElse(false) ||
      service.property(EXTRA).map(BridgeServiceProperties::matchesInGameString).orElse(false) ||
      service.property(STATE).map(BridgeServiceProperties::matchesInGameString).orElse(false));
  }

  private static boolean matchesInGameString(@NonNull String value) {
    value = value.toLowerCase();
    return value.contains("ingame") || value.contains("running") || value.contains("playing");
  }
}
