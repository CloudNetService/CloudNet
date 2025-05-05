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

package eu.cloudnetservice.node.impl.command.source;

import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The driver command source represents a message receiving object for the driver api. All messages regarding command
 * execution and command parsing are sent to the command source, which captures all of them to send them over the
 * network, see {@link ClusterNodeProvider#sendCommandLine(String)}
 *
 * @see CommandSource
 * @since 4.0
 */
public class DriverCommandSource implements CommandSource {

  private final Collection<String> messages = new ArrayList<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return "Driver";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NonNull String message) {
    this.messages.add(message);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NonNull String... messages) {
    this.messages.addAll(Arrays.asList(messages));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NonNull Collection<String> messages) {
    this.messages.addAll(messages);
  }

  /**
   * @param permission the permission to check for
   * @return always true as the driver is allowed to execute every command
   * @throws NullPointerException if permission is null.
   */
  @Override
  public boolean checkPermission(@NonNull String permission) {
    return true;
  }

  /**
   * @return all captured messages for the driver
   */
  public @NonNull Collection<String> messages() {
    return this.messages;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DriverCommandSource that)) {
      return false;
    }
    return that.name().equals(this.name());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.name());
  }
}
