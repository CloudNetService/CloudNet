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

package eu.cloudnetservice.node.event.service;

import eu.cloudnetservice.driver.event.Cancelable;
import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.service.CloudServiceManager;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class CloudServiceNodeSelectEvent extends Event implements Cancelable {

  private final CloudServiceManager serviceManager;
  private final ServiceConfiguration configuration;

  private boolean cancelled;
  private NodeServer nodeServer;

  public CloudServiceNodeSelectEvent(
    @NonNull CloudServiceManager serviceManager,
    @NonNull ServiceConfiguration configuration
  ) {
    this.serviceManager = serviceManager;
    this.configuration = configuration;
  }

  public @NonNull CloudServiceManager serviceManager() {
    return this.serviceManager;
  }

  public @NonNull ServiceConfiguration configuration() {
    return this.configuration;
  }

  public @Nullable NodeServer nodeServer() {
    return this.nodeServer;
  }

  public void nodeServer(@Nullable NodeServer server) {
    this.nodeServer = server;
  }

  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  @Override
  public void cancelled(boolean cancel) {
    this.cancelled = cancel;
  }
}
