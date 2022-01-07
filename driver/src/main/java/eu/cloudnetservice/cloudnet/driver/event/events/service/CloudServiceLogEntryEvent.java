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

package eu.cloudnetservice.cloudnet.driver.event.events.service;

import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * An event being fired when a service prints out a message to stdout or stderr and the service logging is activated.
 *
 * @see SpecificCloudServiceProvider#toggleScreenEvents(ChannelMessageSender, String)
 * @since 4.0
 */
public final class CloudServiceLogEntryEvent extends CloudServiceEvent {

  private final String line;

  /**
   * Constructs a new cloud log entry service event.
   *
   * @param service the service info associated with this event.
   * @param line    the line printed out one of the std streams.
   * @throws NullPointerException if either the service or line is null.
   */
  public CloudServiceLogEntryEvent(@NonNull ServiceInfoSnapshot service, @NonNull String line) {
    super(service);
    this.line = line;
  }

  /**
   * Get the line which was printed to either stdout or stderr by the service.
   *
   * @return the line which was printed to either stdout or stderr.
   */
  public @NonNull String line() {
    return this.line;
  }
}
