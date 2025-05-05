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

package eu.cloudnetservice.modules.signs.impl;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an abstract default implementation of the common sign management methods.
 */
public abstract class AbstractSignManagement implements InternalSignManagement {

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
  protected AbstractSignManagement(@Nullable SignsConfiguration signsConfiguration) {
    this.signsConfiguration = signsConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Sign signAt(@NonNull WorldPosition position) {
    return this.signs.get(position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteSign(@NonNull Sign sign) {
    this.deleteSign(sign.location());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int deleteAllSigns(@NonNull String group) {
    return this.deleteAllSigns(group, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Sign> signs() {
    return this.signs.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull SignsConfiguration signsConfiguration() {
    return this.signsConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void signsConfiguration(@NonNull SignsConfiguration signsConfiguration) {
    this.signsConfiguration = signsConfiguration;
  }

  @Override
  public void registerToServiceRegistry(@NonNull ServiceRegistry serviceRegistry) {
    serviceRegistry.registerProvider(SignManagement.class, "SignManagement", this);
  }

  @Override
  public void handleInternalSignCreate(@NonNull Sign sign) {
    this.signs.put(sign.location(), sign);
  }

  @Override
  public void handleInternalSignRemove(@NonNull WorldPosition position) {
    this.signs.remove(position);
  }

  @Override
  public void handleInternalSignConfigUpdate(@NonNull SignsConfiguration configuration) {
    this.signsConfiguration = configuration;
  }

  /**
   * Creates a channel message which is accepted by all sign network components handler.
   *
   * @param message the message of the channel message.
   * @return the channel message builder for further configuration.
   */
  protected @NonNull ChannelMessage.Builder channelMessage(@NonNull String message) {
    return ChannelMessage.builder()
      .channel(SIGN_CHANNEL_NAME)
      .message(message);
  }
}
