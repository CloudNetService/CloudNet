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

package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an abstract default implementation of the common sign management methods.
 */
public abstract class AbstractSignManagement implements SignManagement {

  public static final String SIGN_CHANNEL_NAME = "internal_sign_channel";

  protected static final String SIGN_CREATED = "signs_sign_created";
  protected static final String SIGN_DELETED = "signs_sign_deleted";
  protected static final String SIGN_BULK_DELETE = "signs_sign_bulk_deleted";
  protected static final String SIGN_CONFIGURATION_UPDATE = "signs_sign_config_update";

  protected final Map<WorldPosition, Sign> signs = new ConcurrentHashMap<>();
  protected SignsConfiguration signsConfiguration;

  /**
   * Constructs a new sign management.
   *
   * @param signsConfiguration the sign configuration to use.
   */
  protected AbstractSignManagement(SignsConfiguration signsConfiguration) {
    this.signsConfiguration = signsConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Sign getSignAt(@NotNull WorldPosition position) {
    return this.signs.get(position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteSign(@NotNull Sign sign) {
    this.deleteSign(sign.getLocation());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int deleteAllSigns(@NotNull String group) {
    return this.deleteAllSigns(group, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<Sign> getSigns() {
    return this.signs.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SignsConfiguration getSignsConfiguration() {
    return this.signsConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSignsConfiguration(@NotNull SignsConfiguration signsConfiguration) {
    this.signsConfiguration = signsConfiguration;
  }

  @Override
  public void registerToServiceRegistry() {
    CloudNetDriver.instance().servicesRegistry().registerService(SignManagement.class, "SignManagement", this);
  }

  @Override
  public void unregisterFromServiceRegistry() {
    CloudNetDriver.instance().servicesRegistry().unregisterService(SignManagement.class, "SignManagement");
  }

  @Override
  public void handleInternalSignCreate(@NotNull Sign sign) {
    this.signs.put(sign.getLocation(), sign);
  }

  @Override
  public void handleInternalSignRemove(@NotNull WorldPosition position) {
    this.signs.remove(position);
  }

  @Override
  public void handleInternalSignConfigUpdate(@NotNull SignsConfiguration configuration) {
    this.signsConfiguration = configuration;
  }

  /**
   * Creates a channel message which is accepted by all sign network components handler.
   *
   * @param message the message of the channel message.
   * @return the channel message builder for further configuration.
   */
  protected ChannelMessage.Builder channelMessage(@NotNull String message) {
    return ChannelMessage.builder()
      .channel(SIGN_CHANNEL_NAME)
      .message(message);
  }
}
