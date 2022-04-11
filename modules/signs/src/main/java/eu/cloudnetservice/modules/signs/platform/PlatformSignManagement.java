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

package eu.cloudnetservice.modules.signs.platform;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.AbstractSignManagement;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.modules.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract sign management shared between the platform implementations.
 *
 * @param <T> the type of the platform dependant sign extend.
 */
public abstract class PlatformSignManagement<T> extends AbstractSignManagement implements SignManagement {

  public static final String SIGN_GET_SIGNS_BY_GROUPS = "signs_get_signs_by_groups";

  /**
   * Constructs a new platform sign management.
   *
   * @param signsConfiguration the sign configuration to use.
   */
  protected PlatformSignManagement(SignsConfiguration signsConfiguration) {
    super(signsConfiguration);
    // get the signs for the current group
    var groups = Wrapper.instance().serviceConfiguration().groups();
    for (var sign : this.signs(groups)) {
      this.signs.put(sign.location(), sign);
    }
  }

  /**
   * Adds a new service to the signs.
   *
   * @param snapshot the service to handle
   */
  public abstract void handleServiceAdd(@NonNull ServiceInfoSnapshot snapshot);

  /**
   * Updates the service on the signs.
   *
   * @param snapshot the service to handle
   */
  public abstract void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot);

  /**
   * Removes the service from the signs.
   *
   * @param snapshot the service to handle
   */
  public abstract void handleServiceRemove(@NonNull ServiceInfoSnapshot snapshot);

  /**
   * Get the sign at the given platform sign extend location.
   *
   * @param t the sign type extend
   * @return The sign at the given location or null if there is no sign at the given location.
   * @see #signAt(WorldPosition)
   */
  public abstract @Nullable Sign signAt(@NonNull T t, @NonNull String group);

  /**
   * Creates a sign at the given platform sign extend location.
   *
   * @param t     the sign type extend.
   * @param group the group the sign is targeting.
   * @return the created sign or null if the sign couldn't be created.
   * @see #createSign(Object, String, String)
   */
  public abstract @Nullable Sign createSign(@NonNull T t, @NonNull String group);

  /**
   * Creates a sign at the given platform sign extend location.
   *
   * @param t            the sign type extend.
   * @param group        the group the sign is targeting.
   * @param templatePath the template path the sign is targeting or null if none.
   * @return the created sign or null if the sign couldn't be created.
   */
  public abstract @Nullable Sign createSign(@NonNull T t, @NonNull String group, @Nullable String templatePath);

  /**
   * Deletes the sign at the given platform sign extend location.
   *
   * @param t the sign type extend
   * @see #deleteSign(WorldPosition)
   */
  public abstract void deleteSign(@NonNull T t, @NonNull String group);

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
  public abstract boolean canConnect(@NonNull Sign sign, @NonNull Function<String, Boolean> permissionChecker);

  @Internal
  public abstract void initialize();

  @Internal
  public abstract void initialize(@NonNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking);

  @Internal
  protected abstract void startKnockbackTask();

  /**
   * Get the signs of all groups the wrapper belongs to.
   *
   * @return the signs of all groups the wrapper belongs to.
   */
  @Override
  public @NonNull Collection<Sign> signs() {
    return super.signs();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Sign> signs(@NonNull Collection<String> groups) {
    var response = this.channelMessage(SIGN_GET_SIGNS_BY_GROUPS)
      .buffer(DataBuf.empty().writeObject(groups))
      .build()
      .sendSingleQuery();
    return response == null ? Collections.emptySet() : response.content().readObject(Sign.COLLECTION_TYPE);
  }

  @Override
  public void handleInternalSignCreate(@NonNull Sign sign) {
    if (Wrapper.instance().serviceConfiguration().groups().contains(sign.location().group())) {
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
  protected @NonNull ChannelMessage.Builder channelMessage(@NonNull String message) {
    return super.channelMessage(message).target(ChannelMessageTarget.Type.NODE, Wrapper.instance().nodeUniqueId());
  }

  /**
   * Get a sign configuration entry from the sign configuration which targets a group the wrapper belongs to.
   *
   * @return a sign configuration entry from the sign configuration which targets a group the wrapper belongs to.
   */
  public @Nullable SignConfigurationEntry applicableSignConfigurationEntry() {
    for (var entry : this.signsConfiguration.entries()) {
      if (Wrapper.instance().serviceConfiguration().groups().contains(entry.targetGroup())) {
        return entry;
      }
    }
    return null;
  }
}
