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

package eu.cloudnetservice.node.impl.service.defaults;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceCreateRetryConfiguration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

final class ServiceCreateRetryTracker implements Runnable {

  private final UUID creationId;
  private final ServiceConfiguration configuration;
  private final ScheduledExecutorService retryExecutor;
  private final CloudServiceFactory cloudServiceFactory;
  private final ServiceCreateRetryConfiguration retryConfiguration;

  private int retries = 0;

  public ServiceCreateRetryTracker(
    @NonNull ServiceConfiguration configuration,
    @NonNull ScheduledExecutorService retryExecutor,
    @NonNull CloudServiceFactory requestingFactory,
    @NonNull ServiceCreateRetryConfiguration retryConfiguration
  ) {
    this.configuration = configuration;
    this.retryExecutor = retryExecutor;
    this.cloudServiceFactory = requestingFactory;
    this.retryConfiguration = retryConfiguration;

    this.creationId = UUID.randomUUID();
  }

  @Override
  public void run() {
    // try to create the service - the result can only fail or succeed, we removed the retry configuration previously
    var createResult = this.cloudServiceFactory.createCloudService(this.configuration);
    Preconditions.checkArgument(createResult.state() != ServiceCreateResult.State.DEFERRED);

    if (createResult == ServiceCreateResult.FAILED) {
      // increase the retry count and check if we should still go on with retrying to create the service
      this.retries++;
      if (this.shouldRetry()) {
        this.retryExecutor.schedule(this, this.nextRetryDelay(), TimeUnit.MILLISECONDS);
        return;
      }
    }

    // ok, we reached the maximum retry count or the creation was successful - time to notify all listeners
    this.notifyListeners(createResult);
  }

  public @NonNull UUID creationId() {
    return this.creationId;
  }

  public long nextRetryDelay() {
    // returns either the current backoff delay based on the retries or the last added backoff delay
    var backoffStrategy = this.retryConfiguration.backoffStrategy();
    return backoffStrategy.size() > this.retries
      ? backoffStrategy.get(this.retries)
      : backoffStrategy.get(backoffStrategy.size() - 1);
  }

  private boolean shouldRetry() {
    return this.retries < this.retryConfiguration.maxRetries();
  }

  private void notifyListeners(@NonNull ServiceCreateResult createResult) {
    // get all event listeners for the creation state, and notify them if there are any
    var eventListeners = this.retryConfiguration.eventReceivers().get(createResult.state());
    if (eventListeners != null && !eventListeners.isEmpty()) {
      // build the base message and add all channel message targets
      var messageBuilder = ChannelMessage.builder()
        .message("deferred_service_event")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeUniqueId(this.creationId).writeObject(createResult));
      eventListeners.forEach(messageBuilder::target);

      // build the message and send it out
      messageBuilder.build().send();
    }
  }
}
