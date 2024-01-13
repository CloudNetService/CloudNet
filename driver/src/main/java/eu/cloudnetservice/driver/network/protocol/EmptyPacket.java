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

package eu.cloudnetservice.driver.network.protocol;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.time.Instant;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a packet with no content in it, throwing an exception when trying to construct a response for or trying to
 * read from. The exception is intentionally placed, so that unexpected packets will lead the system to explode rather
 * than throwing unexpected exceptions which no real way to understand what happened.
 *
 * @since 4.0
 */
final class EmptyPacket implements Packet {

  public static final EmptyPacket INSTANCE = new EmptyPacket();

  /**
   * Private no-args constructor which is not throwing an exception to allow a single instance creation of the class.
   */
  private EmptyPacket() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Packet constructResponse(@NonNull DataBuf content) {
    throw new UnsupportedOperationException("Unable to construct response for empty packet");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable UUID uniqueId() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void uniqueId(@Nullable UUID uniqueId) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int channel() {
    return -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean prioritized() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readable() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf content() {
    throw new UnsupportedOperationException("Empty packet has no buffer.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Instant creation() {
    return Instant.EPOCH;
  }
}
