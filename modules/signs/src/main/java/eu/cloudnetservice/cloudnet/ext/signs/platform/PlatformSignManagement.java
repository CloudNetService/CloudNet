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

package eu.cloudnetservice.cloudnet.ext.signs.platform;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.signs.AbstractSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract sign management shared between the platform implementations.
 *
 * @param <T> the type of the platform dependant sign extend.
 */
public abstract class PlatformSignManagement<T> extends AbstractSignManagement implements SignManagement {

  public static final String SIGN_GET_SIGNS_BY_GROUPS = "signs_get_signs_by_groups";

  /**
   * {@inheritDoc}
   */
  protected PlatformSignManagement(SignsConfiguration signsConfiguration) {
    super(signsConfiguration);
    // get the signs for the current group
    var groups = Wrapper.getInstance().serviceConfiguration().groups().toArray(new String[0]);
    for (var sign : this.signs(groups)) {
      this.signs.put(sign.location(), sign);
    }
  }

  /**
   * Adds a new service to the signs.
   *
   * @param snapshot the service to handle
   */
  public abstract void handleServiceAdd(@NotNull ServiceInfoSnapshot snapshot);

  /**
   * Updates the service on the signs.
   *
   * @param snapshot the service to handle
   */
  public abstract void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot);

  /**
   * Removes the service from the signs.
   *
   * @param snapshot the service to handle
   */
  public abstract void handleServiceRemove(@NotNull ServiceInfoSnapshot snapshot);

  /**
   * Get the sign at the given platform sign extend location.
   *
   * @param t the sign type extend
   * @return The sign at the given location or null if there is no sign at the given location.
   * @see #signAt(WorldPosition)
   */
  public abstract @Nullable Sign signAt(@NotNull T t);

  /**
   * Creates a sign at the given platform sign extend location.
   *
   * @param t     the sign type extend.
   * @param group the group the sign is targeting.
   * @return the created sign or null if the sign couldn't be created.
   * @see #createSign(Object, String, String)
   */
  public abstract @Nullable Sign createSign(@NotNull T t, @NotNull String group);

  /**
   * Creates a sign at the given platform sign extend location.
   *
   * @param t            the sign type extend.
   * @param group        the group the sign is targeting.
   * @param templatePath the template path the sign is targeting or null if none.
   * @return the created sign or null if the sign couldn't be created.
   */
  public abstract @Nullable Sign createSign(@NotNull T t, @NotNull String group, @Nullable String templatePath);

  /**
   * Deletes the sign at the given platform sign extend location.
   *
   * @param t the sign type extend
   * @see #deleteSign(WorldPosition)
   */
  public abstract void deleteSign(@NotNull T t);

  /**
   * Removes all signs where there is no sign block at this position
   *
   * @return the amount of removed signs
   */
  public abstract int removeMissingSigns();

  /**
   * Checks if the given permissible can connect to the sign.
   *
   * @param sign              the sign to check.
   * @param permissionChecker a function which checks if the supplied string is set as a permission.
   * @return true if the permissible can connect using the sign, false otherwise
   */
  public abstract boolean canConnect(@NotNull Sign sign, @NotNull Function<String, Boolean> permissionChecker);

  @Internal
  public abstract void initialize();

  @Internal
  public abstract void initialize(@NotNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking);

  @Internal
  protected abstract void startKnockbackTask();

  /**
   * Get the signs of all groups the wrapper belongs to.
   *
   * @return the signs of all groups the wrapper belongs to.
   */
  @Override
  public @NotNull Collection<Sign> signs() {
    return super.signs();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<Sign> signs(@NotNull String[] groups) {
    var response = this.channelMessage(SIGN_GET_SIGNS_BY_GROUPS)
      .buffer(DataBuf.empty().writeObject(groups))
      .build()
      .sendSingleQuery();
    return response == null ? Collections.emptySet() : response.content().readObject(Sign.COLLECTION_TYPE);
  }

  @Override
  public void handleInternalSignCreate(@NotNull Sign sign) {
    if (Wrapper.getInstance().serviceConfiguration().groups().contains(sign.location().group())) {
      super.handleInternalSignCreate(sign);
    }
  }

  /**
   * Creates a channel message which is targeting the node the wrapper was started by.
   *
   * @param message the message of the channel message.
   * @return the channel message builder for further configuration.
   */
  @Override
  protected ChannelMessage.Builder channelMessage(@NotNull String message) {
    return super.channelMessage(message)
      .target(ChannelMessageTarget.Type.NODE, Wrapper.getInstance().nodeUniqueId());
  }

  /**
   * Get a sign configuration entry from the sign configuration which targets a group the wrapper belongs to.
   *
   * @return a sign configuration entry from the sign configuration which targets a group the wrapper belongs to.
   */
  public @Nullable SignConfigurationEntry applicableSignConfigurationEntry() {
    for (var entry : this.signsConfiguration.configurationEntries()) {
      if (Wrapper.getInstance().serviceConfiguration().groups().contains(entry.targetGroup())) {
        return entry;
      }
    }
    return null;
  }
}
