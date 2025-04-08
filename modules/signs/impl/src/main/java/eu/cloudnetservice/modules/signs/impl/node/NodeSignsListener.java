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

package eu.cloudnetservice.modules.signs.impl.node;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.AbstractSignManagement;
import eu.cloudnetservice.modules.signs.impl.InternalSignManagement;
import eu.cloudnetservice.modules.signs.impl.node.util.SignEntryTaskSetup;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSignManagement;
import eu.cloudnetservice.node.impl.event.setup.SetupCompleteEvent;
import eu.cloudnetservice.node.impl.event.setup.SetupInitiateEvent;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;

@Singleton
public class NodeSignsListener {

  private static final Type STRING_COLLECTION = TypeFactory.parameterizedClass(Collection.class, String.class);

  private final InternalSignManagement signManagement;
  private final SignEntryTaskSetup entryTaskSetup;

  @Inject
  public NodeSignsListener(@NonNull InternalSignManagement signManagement, @NonNull SignEntryTaskSetup entryTaskSetup) {
    this.signManagement = signManagement;
    this.entryTaskSetup = entryTaskSetup;
  }

  @EventListener
  public void handleSetupInitialize(@NonNull SetupInitiateEvent event) {
    event.setup().entries().stream()
      .filter(entry -> entry.key().equals("taskEnvironment"))
      .findFirst()
      .ifPresent(entry -> entry.answerType().thenAccept(($, environment) ->
        this.entryTaskSetup.addSetupQuestionIfNecessary(event.setup(), (ServiceEnvironmentType) environment)));
  }

  @EventListener
  public void handleSetupComplete(@NonNull SetupCompleteEvent event) {
    this.entryTaskSetup.handleSetupComplete(
      event.setup(),
      this.signManagement.signsConfiguration(),
      this.signManagement);
  }

  @EventListener
  public void handleChannelMessage(ChannelMessageReceiveEvent event) {
    if (event.channel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME)) {
      switch (event.message()) {
        // config request
        case PlatformSignManagement.REQUEST_CONFIG -> event.binaryResponse(
          DataBuf.empty().writeObject(this.signManagement.signsConfiguration()));

        // delete all signs
        case PlatformSignManagement.SIGN_ALL_DELETE -> {
          Collection<WorldPosition> positions = event.content().readObject(WorldPosition.COL_TYPE);
          for (var position : positions) {
            this.signManagement.deleteSign(position);
          }
        }
        // create a new sign
        case PlatformSignManagement.SIGN_CREATE -> this.signManagement.createSign(
          event.content().readObject(Sign.class));

        // delete an existing sign
        case PlatformSignManagement.SIGN_DELETE -> this.signManagement.deleteSign(
          event.content().readObject(WorldPosition.class));

        // delete all signs
        case PlatformSignManagement.SIGN_BULK_DELETE -> {
          var deleted = this.signManagement
            .deleteAllSigns(event.content().readString(), event.content().readNullable(DataBuf::readString));
          event.binaryResponse(DataBuf.empty().writeInt(deleted));
        }
        // set the sign config
        case PlatformSignManagement.SET_SIGN_CONFIG -> this.signManagement.signsConfiguration(
          event.content().readObject(SignsConfiguration.class));

        // get all signs of a group
        case PlatformSignManagement.SIGN_GET_SIGNS_BY_GROUPS -> {
          var signs = this.signManagement.signs(event.content().readObject(STRING_COLLECTION));
          event.binaryResponse(DataBuf.empty().writeObject(signs));
        }
        // set the sign configuration without a re-publish to the cluster
        case NodeSignManagement.NODE_TO_NODE_SET_SIGN_CONFIGURATION ->
          this.signManagement.handleInternalSignConfigUpdate(
            event.content().readObject(SignsConfiguration.class));
        default -> {
        }
      }
    }
  }
}
