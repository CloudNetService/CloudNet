/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.signs.configuration;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class SignConfigurationProvider {

  private static volatile SignConfiguration loadedConfiguration;

  private SignConfigurationProvider() {
    throw new UnsupportedOperationException();
  }

  public static void setLocal(SignConfiguration signConfiguration) {
    Preconditions.checkNotNull(signConfiguration);

    loadedConfiguration = signConfiguration;
  }

  public static SignConfiguration load() {
    if (loadedConfiguration == null) {
      loadedConfiguration = load0();
    }

    return loadedConfiguration;
  }

  private static SignConfiguration load0() {
    ChannelMessage response = ChannelMessage.builder()
      .channel(SignConstants.SIGN_CHANNEL_NAME)
      .message(SignConstants.SIGN_CHANNEL_GET_SIGNS_CONFIGURATION)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    return response == null ? null : response.getJson().get("signConfiguration", SignConfiguration.TYPE);
  }

}
