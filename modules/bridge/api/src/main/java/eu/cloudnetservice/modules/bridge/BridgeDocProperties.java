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

import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import io.leangen.geantyref.TypeFactory;
import java.util.Collection;

/**
 * The bridge service properties are there to read any properties set by the bridge module. It is important to note that
 * not all values are set by default and most values cannot be overridden. For these values to be set, the bridge must
 * run as a plugin on the respective service.
 *
 * @since 4.0
 */
public final class BridgeDocProperties {

  /**
   * This property holds the required permission that a player must have to join a specific service. If the property is
   * not set, the player is unable to join. This property defaults to {@code null} with indicates that no permission is
   * required to join the service.
   * <p>
   * Note: this property only has effect when applied to the associated service task of a service.
   */
  public static final DocProperty<String> REQUIRED_PERMISSION =
    DocProperty.property("requiredPermission", String.class);
  /**
   * This service property reads the online player count of any given {@link ServiceInfoSnapshot}. The property is only
   * updated after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final DocProperty<Integer> ONLINE_COUNT = DocProperty.property("Online-Count", Integer.class)
    .asReadOnly()
    .withDefault(0);
  /**
   * This service property reads the max player count of any given {@link ServiceInfoSnapshot}. The property is only
   * updated after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final DocProperty<Integer> MAX_PLAYERS = DocProperty.property("Max-Players", Integer.class)
    .asReadOnly()
    .withDefault(-1);
  /**
   * This service property reads the version of any given {@link ServiceInfoSnapshot}. The property is only updated
   * after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final DocProperty<String> VERSION = DocProperty.property("Version", String.class).asReadOnly();
  /**
   * This service property reads the motd of any given {@link ServiceInfoSnapshot}. The property is only updated after
   * the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   * <p>
   * Setting the state is possible using {@link BridgeServiceHelper#motd()} on the service itself.
   */
  public static final DocProperty<String> MOTD = DocProperty.property("Motd", String.class).asReadOnly();
  /**
   * This service property reads the extra value of any given {@link ServiceInfoSnapshot}. The property is only updated
   * after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   * <p>
   * Setting the state is possible using {@link BridgeServiceHelper#extra()} on the service itself.
   */
  public static final DocProperty<String> EXTRA = DocProperty.property("Extra", String.class).asReadOnly();
  /**
   * This service property reads the state of any given {@link ServiceInfoSnapshot}. The property is only updated after
   * the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   * <p>
   * Setting the state is possible using {@link BridgeServiceHelper#state()} on the service itself.
   */
  public static final DocProperty<String> STATE = DocProperty.property("State", String.class).asReadOnly();
  /**
   * This service property reads the online property of any given {@link ServiceInfoSnapshot}. This property is always
   * true after the bridge plugin was enabled. The property is only updated after the service itself was updated.
   * <p>
   * Note: This property is not modifiable, modifying it results in an {@link UnsupportedOperationException}.
   */
  public static final DocProperty<Boolean> IS_ONLINE = DocProperty.property("Online", Boolean.class)
    .asReadOnly()
    .withDefault(false);
  /**
   * This service property allows accessing all players that are connected to the given service. The property is only
   * updated after the service itself was updated.
   * <p>
   * Note: Writing to this property is not allowed it results in an {@link UnsupportedOperationException}.
   */
  public static final DocProperty<Collection<ServicePlayer>> PLAYERS = DocProperty.<Collection<ServicePlayer>>genericProperty(
      "Players",
      TypeFactory.parameterizedClass(Collection.class, ServicePlayer.class))
    .asReadOnly();

  private BridgeDocProperties() {
    throw new UnsupportedOperationException();
  }
}
