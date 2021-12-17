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

package eu.cloudnetservice.cloudnet.ext.signs.node;

import static eu.cloudnetservice.cloudnet.ext.signs.node.util.SignEntryTaskSetup.addSetupQuestionIfNecessary;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.service.CloudServicePreProcessStartEvent;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupInitiateEvent;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.AbstractSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.util.SignEntryTaskSetup;
import eu.cloudnetservice.cloudnet.ext.signs.node.util.SignPluginInclusion;
import eu.cloudnetservice.cloudnet.ext.signs.platform.AbstractPlatformSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.platform.PlatformSignManagement;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class NodeSignsListener {

  protected final SignManagement signManagement;

  public NodeSignsListener(@NotNull SignManagement signManagement) {
    this.signManagement = signManagement;
  }

  @EventListener
  public void handleSetupInitialize(@NotNull SetupInitiateEvent event) {
    event.setup().entries().stream()
      .filter(entry -> entry.key().equals("taskEnvironment"))
      .findFirst()
      .ifPresent(entry -> entry.answerType().thenAccept(($, environment) -> addSetupQuestionIfNecessary(
        event.setup(),
        (ServiceEnvironmentType) environment)));
  }

  @EventListener
  public void handleSetupComplete(@NotNull SetupCompleteEvent event) {
    SignEntryTaskSetup.handleSetupComplete(
      event.setup(),
      this.signManagement.signsConfiguration(),
      this.signManagement);
  }

  @EventListener
  public void includePluginIfNecessary(@NotNull CloudServicePreProcessStartEvent event) {
    SignPluginInclusion.includePluginTo(event.service(), this.signManagement.signsConfiguration());
  }

  @EventListener
  public void handleChannelMessage(ChannelMessageReceiveEvent event) {
    if (event.channel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME)) {
      switch (event.message()) {
        // config request
        case AbstractPlatformSignManagement.REQUEST_CONFIG -> event.binaryResponse(
            DataBuf.empty().writeObject(this.signManagement.signsConfiguration()));

        // delete all signs
        case AbstractPlatformSignManagement.SIGN_ALL_DELETE -> {
          Collection<WorldPosition> positions = event.content().readObject(WorldPosition.COL_TYPE);
          for (var position : positions) {
            this.signManagement.deleteSign(position);
          }
        }
        // create a new sign
        case AbstractPlatformSignManagement.SIGN_CREATE -> this.signManagement.createSign(
          event.content().readObject(Sign.class));

        // delete an existing sign
        case AbstractPlatformSignManagement.SIGN_DELETE -> this.signManagement.deleteSign(
          event.content().readObject(WorldPosition.class));

        // delete all signs
        case AbstractPlatformSignManagement.SIGN_BULK_DELETE -> {
          var deleted = this.signManagement
            .deleteAllSigns(event.content().readString(), event.content().readNullable(DataBuf::readString));
          event.binaryResponse(DataBuf.empty().writeInt(deleted));
        }
        // set the sign config
        case AbstractPlatformSignManagement.SET_SIGN_CONFIG -> this.signManagement.signsConfiguration(
          event.content().readObject(SignsConfiguration.class));

        // get all signs of a group
        case PlatformSignManagement.SIGN_GET_SIGNS_BY_GROUPS -> {
          var signs = this.signManagement.signs(event.content().readObject(String[].class));
          event.binaryResponse(DataBuf.empty().writeObject(signs));
        }
        // set the sign configuration without a re-publish to the cluster
        case NodeSignManagement.NODE_TO_NODE_SET_SIGN_CONFIGURATION -> this.signManagement.handleInternalSignConfigUpdate(
          event.content().readObject(SignsConfiguration.class));
        default -> {
        }
      }
    }
  }
}
