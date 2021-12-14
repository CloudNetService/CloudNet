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
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.event.service.CloudServicePreLifecycleEvent;
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
    event.getSetup().getEntries().stream()
      .filter(entry -> entry.getKey().equals("taskEnvironment"))
      .findFirst()
      .ifPresent(entry -> entry.getAnswerType().thenAccept(($, environment) -> addSetupQuestionIfNecessary(
        event.getSetup(),
        (ServiceEnvironmentType) environment)));
  }

  @EventListener
  public void handleSetupComplete(@NotNull SetupCompleteEvent event) {
    SignEntryTaskSetup.handleSetupComplete(
      event.getSetup(),
      this.signManagement.getSignsConfiguration(),
      this.signManagement);
  }

  @EventListener
  public void includePluginIfNecessary(@NotNull CloudServicePreLifecycleEvent event) {
    if (event.getTargetLifecycle() == ServiceLifeCycle.RUNNING) {
      SignPluginInclusion.includePluginTo(event.getService(), this.signManagement.getSignsConfiguration());
    }
  }

  @EventListener
  public void handleChannelMessage(ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME)) {
      switch (event.getMessage()) {
        // config request
        case AbstractPlatformSignManagement.REQUEST_CONFIG -> event.setBinaryResponse(
            DataBuf.empty().writeObject(this.signManagement.getSignsConfiguration()));

        // delete all signs
        case AbstractPlatformSignManagement.SIGN_ALL_DELETE -> {
          Collection<WorldPosition> positions = event.getContent().readObject(WorldPosition.COL_TYPE);
          for (var position : positions) {
            this.signManagement.deleteSign(position);
          }
        }
        // create a new sign
        case AbstractPlatformSignManagement.SIGN_CREATE -> this.signManagement.createSign(
            event.getContent().readObject(Sign.class));

        // delete an existing sign
        case AbstractPlatformSignManagement.SIGN_DELETE -> this.signManagement.deleteSign(
            event.getContent().readObject(WorldPosition.class));

        // delete all signs
        case AbstractPlatformSignManagement.SIGN_BULK_DELETE -> {
          var deleted = this.signManagement
              .deleteAllSigns(event.getContent().readString(), event.getContent().readNullable(DataBuf::readString));
          event.setBinaryResponse(DataBuf.empty().writeInt(deleted));
        }
        // set the sign config
        case AbstractPlatformSignManagement.SET_SIGN_CONFIG -> this.signManagement.setSignsConfiguration(
            event.getContent().readObject(SignsConfiguration.class));

        // get all signs of a group
        case PlatformSignManagement.SIGN_GET_SIGNS_BY_GROUPS -> {
          var signs = this.signManagement.getSigns(event.getContent().readObject(String[].class));
          event.setBinaryResponse(DataBuf.empty().writeObject(signs));
        }
        // set the sign configuration without a re-publish to the cluster
        case NodeSignManagement.NODE_TO_NODE_SET_SIGN_CONFIGURATION -> this.signManagement.handleInternalSignConfigUpdate(
            event.getContent().readObject(SignsConfiguration.class));
        default -> {
        }
      }
    }
  }
}
