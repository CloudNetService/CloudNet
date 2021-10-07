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

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.AbstractSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.util.SignEntryTaskSetup;
import eu.cloudnetservice.cloudnet.ext.signs.node.util.SignPluginInclusion;
import eu.cloudnetservice.cloudnet.ext.signs.service.AbstractServiceSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import java.util.Collection;

public class NodeSignsListener {

  protected final SignManagement signManagement;

  public NodeSignsListener(SignManagement signManagement) {
    this.signManagement = signManagement;
  }

  @EventListener
  public void handleNodeAuthSuccess(NetworkChannelAuthClusterNodeSuccessEvent event) {
    event.getNode().sendCustomChannelMessage(ChannelMessage.builder()
      .channel(AbstractSignManagement.SIGN_CHANNEL_NAME)
      .message(NodeSignManagement.NODE_TO_NODE_SET_SIGN_CONFIGURATION)
      .buffer(DataBuf.empty().writeObject(this.signManagement.getSignsConfiguration()))
      .build());
  }

  @EventListener
  public void includePluginIfNecessary(CloudServicePreStartEvent event) {
    SignPluginInclusion.includePluginTo(event.getCloudService(), this.signManagement.getSignsConfiguration());
  }

  @EventListener
  public void handleTaskSetupResponse(SetupResponseEvent event) {
    if (event.getSetup().getName().equals("TaskSetup") && event.getResponse() instanceof ServiceEnvironmentType) {
      SignEntryTaskSetup.addSetupQuestionIfNecessary(event.getSetup(), (ServiceEnvironmentType) event.getResponse());
    }
  }

  @EventListener
  public void handleSetupComplete(SetupCompleteEvent event) {
    SignEntryTaskSetup
      .handleSetupComplete(event.getSetup(), this.signManagement.getSignsConfiguration(), this.signManagement);
  }

  @EventListener
  public void handleChannelMessage(ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME) && event.getMessage() != null) {
      switch (event.getMessage()) {
        case AbstractServiceSignManagement.REQUEST_CONFIG:
          event.setBinaryResponse(DataBuf.empty().writeObject(this.signManagement.getSignsConfiguration()));
          break;
        case AbstractServiceSignManagement.SIGN_ALL_DELETE:
          Collection<WorldPosition> positions = DefaultObjectMapper.DEFAULT_MAPPER.readObject(event.getContent(),
            WorldPosition.COLLECTION_TYPE);
          for (WorldPosition position : positions) {
            this.signManagement.deleteSign(position);
          }
          break;
        case AbstractServiceSignManagement.SIGN_CREATE:
          this.signManagement.createSign(event.getContent().readObject(Sign.class));
          break;
        case AbstractServiceSignManagement.SIGN_DELETE:
          this.signManagement.deleteSign(event.getContent().readObject(WorldPosition.class));
          break;
        case AbstractServiceSignManagement.SIGN_BULK_DELETE:
          int deleted = this.signManagement
            .deleteAllSigns(event.getContent().readString(),
              event.getContent().readNullable(DataBuf::readString));
          event.setBinaryResponse(DataBuf.empty().writeInt(deleted));
          break;
        case AbstractServiceSignManagement.SET_SIGN_CONFIG:
          this.signManagement.setSignsConfiguration(event.getContent().readObject(SignsConfiguration.class));
          break;
        case ServiceSignManagement.SIGN_GET_SIGNS_BY_GROUPS:
          Collection<Sign> signs = this.signManagement.getSigns(event.getContent().readObject(String[].class));
          event.setBinaryResponse(DataBuf.empty().writeObject(signs));
          break;
        case NodeSignManagement.NODE_TO_NODE_SET_SIGN_CONFIGURATION:
          this.signManagement.handleInternalSignConfigUpdate(event.getContent().readObject(SignsConfiguration.class));
          break;
        default:
          break;
      }
    }
  }
}
