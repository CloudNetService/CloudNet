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

package eu.cloudnetservice.driver.impl.channel;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of a {@link CloudMessenger} which implements all sharable methods.
 *
 * @since 4.0
 */
public abstract class BaseCloudMessenger implements CloudMessenger {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    var done = new AtomicBoolean();
    return this.sendChannelMessageQueryAsync(channelMessage)
      .thenApply(responses -> {
        // it might be that the timeout defined in the next step was already hit, therefore
        // this method already returned to the caller before a response is received. in this
        // case, we release the response we got immediately as it would leak otherwise
        var didComplete = done.compareAndSet(false, true);
        if (didComplete) {
          return responses;
        } else {
          responses.forEach(ChannelMessage::close);
          throw new IllegalStateException("received responses after downstream already completed");
        }
      })
      // hack: get a new incomplete future here so that the previous thenApply step runs as well.
      // the following orTimeout completes the future it's called on, so the releasing step would never run
      .thenApply(Function.identity())
      .orTimeout(SYNC_CHANNEL_MESSAGE_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      .whenComplete((_, _) -> done.set(true))
      .join();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    var done = new AtomicBoolean();
    return this.sendSingleChannelMessageQueryAsync(channelMessage)
      .thenApply(response -> {
        // it might be that the timeout defined in the next step was already hit, therefore
        // this method already returned to the caller before a response is received. in this
        // case, we release the response we got immediately as it would leak otherwise
        var didComplete = done.compareAndSet(false, true);
        if (didComplete) {
          return response;
        } else {
          response.close();
          throw new IllegalStateException("received response after downstream already completed");
        }
      })
      // hack: get a new incomplete future here so that the previous thenApply step runs as well.
      // the following orTimeout completes the future it's called on, so the releasing step would never run
      .thenApply(Function.identity())
      .orTimeout(SYNC_CHANNEL_MESSAGE_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      .whenComplete((_, _) -> done.set(true))
      .join();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Void> sendChannelMessageAsync(@NonNull ChannelMessage channelMessage) {
    return TaskUtil.runAsync(() -> this.sendChannelMessage(channelMessage));
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public CompletableFuture<ChannelMessage> sendSingleChannelMessageQueryAsync(@NonNull ChannelMessage channelMessage) {
    return this.sendChannelMessageQueryAsync(channelMessage).thenApply(responses -> {
      var responseCount = responses.size();
      return switch (responseCount) {
        case 0 -> null;
        case 1 -> Iterables.getOnlyElement(responses);
        default -> {
          // there were more than one response, so we need to close the
          // other responses to prevent leaking their content
          var responsesArray = responses.toArray(ChannelMessage[]::new);
          for (var index = 1; index < responsesArray.length; index++) {
            var response = responsesArray[index];
            response.close();
          }

          yield responsesArray[0];
        }
      };
    });
  }
}
