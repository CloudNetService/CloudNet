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

package eu.cloudnetservice.driver.impl.network.netty;

import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;
import io.netty5.channel.ChannelOption;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;

/**
 * A custom channel initializer implementation which activates option in the given channel during the initialization if
 * they are supported by the implementation.
 *
 * @since 4.0
 */
public class NettyOptionSettingChannelInitializer extends ChannelInitializer<Channel> {

  private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void initChannel(@NonNull Channel channel) throws Exception {
    // set the channel options in the channel if they are supported
    // this operation is not thread safe as we assume that all options are put into the map at this point
    for (var optionEntry : this.options.entrySet()) {
      var option = (ChannelOption<Object>) optionEntry.getKey();
      if (channel.isOptionSupported(option)) {
        channel.setOption(option, optionEntry.getValue());
      }
    }

    // do further initialization to the channel
    this.doInitChannel(channel);
  }

  /**
   * Adds an option which should be activated in channels initialized by this initializer if it is supported.
   *
   * @param option the option to activate if supported.
   * @param value  the value of the option.
   * @param <T>    the type of the option value.
   * @return this initializer instance, for chaining.
   * @throws NullPointerException if the given option or value is null.
   */
  public @NonNull <T> NettyOptionSettingChannelInitializer option(@NonNull ChannelOption<T> option, @NonNull T value) {
    this.options.put(option, value);
    return this;
  }

  /**
   * Method that gets called when the actual channel initialization finished and all requested channel options were
   * activated in the channel. This channel removes the need for overriding classes to call {@code super.initChannel} in
   * order to get all channel options activated.
   *
   * @param channel the channel to initialize.
   * @throws Exception thrown if an error occurs during initialization.
   */
  protected void doInitChannel(@NonNull Channel channel) throws Exception {
  }
}
