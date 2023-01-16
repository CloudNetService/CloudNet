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

package eu.cloudnetservice.driver.service.property;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents a property which can be written and read from a service info snapshot, for example the amount of online
 * players, the state of the service or other custom properties appended by a plugin. This simplifies accessing these
 * properties by making a common wrapper once.
 * <p>
 * The old code might look like this:
 * <pre>
 * {@code
 *  public final class GameListener implement Listener {
 *    public void handleJoin(PlayerLoginEvent event) {
 *      ServiceInfoSnapshot snapshot = this.manager.currentSnapshot();
 *      GameState state = snapshot.properties().get("state", GameState.class);
 *
 *      if (state == GameState.STARTED) {
 *        event.setResult(PlayerLoginEvent.Result.DENIED);
 *      }
 *    }
 *  }
 * }
 * </pre>
 * <p>
 * All the property read code can now be replaced by a simple static field like this:
 * <pre>
 * {@code
 *  public final class GameListener implement Listener {
 *    public static final ServiceProperty<GameState> STATE
 *      = JsonServiceProperty.createFromClass("state", GameState.class);
 *
 *    public void handleJoin(PlayerLoginEvent event) {
 *      GameState state = STATE.read(this.manager.currentSnapshot());
 *
 *      if (state == GameState.STARTED) {
 *        event.setResult(PlayerLoginEvent.Result.DENIED);
 *      }
 *    }
 *  }
 * }
 * </pre>
 *
 * @param <T> the type which gets read/written by this property.
 * @since 4.0
 */
public interface ServiceProperty<T> {

  /**
   * Reads the target property from the given service info snapshot.
   *
   * @param serviceInfoSnapshot the service snapshot to get the property from.
   * @return the value of this property wrapped in the given service info, null if not assigned.
   * @throws NullPointerException          if the given service snapshot is null.
   * @throws UnsupportedOperationException if reading from this property is not supported.
   */
  @Nullable T read(@NonNull ServiceInfoSnapshot serviceInfoSnapshot);

  /**
   * Reads the target property from the given service info snapshot.
   *
   * @param serviceInfoSnapshot the service snapshot to get the property from.
   * @param def                 the value to return if this property is not set in the given snapshot.
   * @return the value of this property wrapped in the given service info, the given default value if not assigned.
   * @throws NullPointerException          if the given service snapshot is null.
   * @throws UnsupportedOperationException if reading from this property is not supported.
   */
  default @UnknownNullability T readOr(@NonNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T def) {
    var value = this.read(serviceInfoSnapshot);
    return value == null ? def : value;
  }

  /**
   * Sets the given property value in the service info snapshot. This method will not automatically update the service
   * snapshot, meaning that an update might be necessary to make the change visible to all other services.
   *
   * @param serviceInfoSnapshot the service snapshot to write the given value to.
   * @param value               the actual value to write into the snapshot, can be null which writes a null value.
   * @throws NullPointerException          if the given service snapshot is null.
   * @throws UnsupportedOperationException if writing to this property is not supported.
   */
  void write(@NonNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T value);
}
