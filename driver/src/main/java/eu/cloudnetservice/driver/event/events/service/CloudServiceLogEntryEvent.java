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

package eu.cloudnetservice.driver.event.events.service;

import eu.cloudnetservice.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * An event being fired when a service prints out a message to stdout or stderr and the service logging is activated.
 *
 * @see SpecificCloudServiceProvider#toggleScreenEvents(ChannelMessageSender, String)
 * @since 4.0
 */
public final class CloudServiceLogEntryEvent extends CloudServiceEvent {

  private final String line;
  private final StreamType streamType;

  /**
   * Constructs a new cloud log entry service event.
   *
   * @param info the service info associated with this event.
   * @param line the line printed out one of the std streams.
   * @param type the type of stream from which the line comes.
   * @throws NullPointerException if either the service, line or stream type is null.
   */
  public CloudServiceLogEntryEvent(@NonNull ServiceInfoSnapshot info, @NonNull String line, @NonNull StreamType type) {
    super(info);
    this.line = line;
    this.streamType = type;
  }

  /**
   * Get the line which was printed to either stdout or stderr by the service.
   *
   * @return the line which was printed to either stdout or stderr.
   */
  public @NonNull String line() {
    return this.line;
  }

  /**
   * Get the type of stream from which the log line is coming.
   *
   * @return the type of stream from which the log line is coming.
   */
  public @NonNull StreamType streamType() {
    return this.streamType;
  }

  /**
   * Represents a type of std stream.
   */
  public enum StreamType {

    /**
     * Represents the standard output stream. (In java accessed by using {@code System.out}).
     */
    STDOUT,
    /**
     * Represents the error output stream. (In java accessed by using {@code System.err}).
     */
    STDERR
  }
}
