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

import static de.dytanic.cloudnet.driver.service.property.DefaultJsonServiceProperty.createFromClass;
import static de.dytanic.cloudnet.driver.service.property.DefaultJsonServiceProperty.createFromType;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.property.DefaultFunctionalServiceProperty;
import de.dytanic.cloudnet.driver.service.property.DefaultModifiableServiceProperty;
import de.dytanic.cloudnet.driver.service.property.ServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Properties in ServiceInfos by the bridge module.
 */
public class BridgeServiceProperty {

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
  public static final ServiceProperty<Boolean> IS_IN_GAME = DefaultFunctionalServiceProperty.<Boolean>create()
    .get(BridgeServiceProperty::isInGameService);
  /**
   * Property to check whether a service is starting or not.
   */
  public static final ServiceProperty<Boolean> IS_STARTING = DefaultFunctionalServiceProperty.<Boolean>create()
    .get(BridgeServiceProperty::isStartingService);
  /**
   * Property to check whether a service is empty (no players) or not.
   */
  public static final ServiceProperty<Boolean> IS_EMPTY = DefaultFunctionalServiceProperty.<Boolean>create()
    .get(BridgeServiceProperty::isEmptyService);
  /**
   * Property to check whether a service is full (online count &gt;= max players) or not.
   */
  public static final ServiceProperty<Boolean> IS_FULL = DefaultFunctionalServiceProperty.<Boolean>create()
    .get(BridgeServiceProperty::isFullService);
  private static final TypeToken<Collection<JsonDocument>> DOCUMENT_COLLECTION_TYPE = new TypeToken<Collection<JsonDocument>>() {
  };
  /**
   * Property to get all online players on a service.
   */
  public static final ServiceProperty<Collection<ServicePlayer>> PLAYERS = DefaultModifiableServiceProperty
    .<Collection<JsonDocument>, Collection<ServicePlayer>>wrap(
      createFromType("Players", DOCUMENT_COLLECTION_TYPE.getType()))
    .modifyGet(
      (serviceInfoSnapshot, documents) -> documents.stream().map(ServicePlayer::new).collect(Collectors.toList()));
  private static final TypeToken<Collection<PluginInfo>> PLUGIN_INFO_COLLECTION_TYPE = new TypeToken<Collection<PluginInfo>>() {
  };
  /**
   * Property to get all installed plugins on a service.
   */
  public static final ServiceProperty<Collection<PluginInfo>> PLUGINS = createFromType("Plugins",
    PLUGIN_INFO_COLLECTION_TYPE.getType(), true);

  private static boolean isEmptyService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.isConnected() &&
      serviceInfoSnapshot.getProperty(IS_ONLINE).orElse(false) &&
      serviceInfoSnapshot.getProperty(ONLINE_COUNT).isPresent() &&
      serviceInfoSnapshot.getProperty(ONLINE_COUNT).orElse(0) == 0;
  }

  private static boolean isFullService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.isConnected() &&
      serviceInfoSnapshot.getProperty(IS_ONLINE).orElse(false) &&
      serviceInfoSnapshot.getProperty(ONLINE_COUNT).isPresent() &&
      serviceInfoSnapshot.getProperty(MAX_PLAYERS).isPresent() &&
      serviceInfoSnapshot.getProperty(ONLINE_COUNT).orElse(0) >=
        serviceInfoSnapshot.getProperty(MAX_PLAYERS).orElse(0);
  }

  private static boolean isStartingService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING && !serviceInfoSnapshot.getProperty(IS_ONLINE)
      .orElse(false);
  }

  private static boolean isInGameService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING && serviceInfoSnapshot.isConnected() &&
      serviceInfoSnapshot.getProperty(IS_ONLINE).orElse(false) &&
      (
        serviceInfoSnapshot.getProperty(MOTD).map(BridgeServiceProperty::matchesInGameString).orElse(false) ||
          serviceInfoSnapshot.getProperty(EXTRA).map(BridgeServiceProperty::matchesInGameString).orElse(false) ||
          serviceInfoSnapshot.getProperty(STATE).map(BridgeServiceProperty::matchesInGameString).orElse(false)
      );
  }

  private static boolean matchesInGameString(String text) {
    text = text.toLowerCase();
    return text.contains("ingame") || text.contains("running") || text.contains("playing");
  }
}
