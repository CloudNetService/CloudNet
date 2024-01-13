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

package eu.cloudnetservice.driver.provider.defaults;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract helper implementation of the cloud messenger which can be shared over all further implementations.
 *
 * @since 4.0
 */
public abstract class DefaultMessenger implements CloudMessenger {

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return Iterables.getFirst(this.sendChannelMessageQuery(channelMessage), null);
  }
}
