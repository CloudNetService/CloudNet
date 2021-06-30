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

package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.ServiceDriverAPIResponse;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import java.util.UUID;
import java.util.function.BiConsumer;

public class DriverSpecificServiceListener extends CategorizedDriverAPIListener {

  public DriverSpecificServiceListener() {
    super(DriverAPICategory.SPECIFIC_CLOUD_SERVICE);

    super.registerHandler(DriverAPIRequestType.IS_CLOUD_SERVICE_VALID,
      (channel, packet, input) -> this.provider(packet, (buffer, provider) -> {
      }));
    super.registerHandler(DriverAPIRequestType.FORCE_UPDATE_SERVICE,
      (channel, packet, input) -> this.provider(packet, (buffer, provider) -> buffer
        .writeEnumConstant(ServiceDriverAPIResponse.SUCCESS)
        .writeOptionalObject(provider.forceUpdateServiceInfo())
      ));

    super.registerHandler(DriverAPIRequestType.SET_CLOUD_SERVICE_LIFE_CYCLE, (channel, packet, input) -> this
      .provider(packet,
        (buffer, provider) -> provider.setCloudServiceLifeCycle(input.readEnumConstant(ServiceLifeCycle.class))));
    super.registerHandler(DriverAPIRequestType.RESTART_CLOUD_SERVICE,
      (channel, packet, input) -> this.provider(packet, (buffer, provider) -> provider.restart()));
    super.registerHandler(DriverAPIRequestType.KILL_CLOUD_SERVICE,
      (channel, packet, input) -> this.provider(packet, (buffer, provider) -> provider.kill()));

    super.registerHandler(DriverAPIRequestType.INCLUDE_WAITING_TEMPLATES_ON_CLOUD_SERVICE,
      (channel, packet, input) -> this
        .provider(packet, (buffer, provider) -> provider.includeWaitingServiceTemplates()));
    super.registerHandler(DriverAPIRequestType.INCLUDE_WAITING_INCLUSIONS_ON_CLOUD_SERVICE,
      (channel, packet, input) -> this
        .provider(packet, (buffer, provider) -> provider.includeWaitingServiceInclusions()));

    super.registerHandler(DriverAPIRequestType.DEPLOY_RESOURCES_ON_CLOUD_SERVICE, (channel, packet, input) -> this
      .provider(packet, (buffer, provider) -> provider.deployResources(input.readBoolean())));

    super.registerHandler(DriverAPIRequestType.ADD_SERVICE_TEMPLATE_TO_CLOUD_SERVICE, (channel, packet, input) -> this
      .provider(packet, (buffer, provider) -> provider.addServiceTemplate(input.readObject(ServiceTemplate.class))));
    super.registerHandler(DriverAPIRequestType.ADD_SERVICE_DEPLOYMENT_TO_CLOUD_SERVICE, (channel, packet, input) -> this
      .provider(packet,
        (buffer, provider) -> provider.addServiceDeployment(input.readObject(ServiceDeployment.class))));
    super.registerHandler(DriverAPIRequestType.ADD_SERVICE_REMOTE_INCLUSION_TO_CLOUD_SERVICE,
      (channel, packet, input) -> this.provider(packet,
        (buffer, provider) -> provider.addServiceRemoteInclusion(input.readObject(ServiceRemoteInclusion.class))));

    super.registerHandler(DriverAPIRequestType.RUN_COMMAND_ON_CLOUD_SERVICE,
      (channel, packet, input) -> this.provider(packet, (buffer, provider) -> provider.runCommand(input.readString())));
    super.registerHandler(DriverAPIRequestType.GET_CACHED_LOG_MESSAGES_FROM_CLOUD_SERVICE,
      (channel, packet, input) -> this.provider(packet, (buffer, provider) -> {
        Queue<String> messages = provider.getCachedLogMessages();
        buffer.writeStringCollection(messages);
      }));

  }

  private ProtocolBuffer provider(IPacket packet, BiConsumer<ProtocolBuffer, SpecificCloudServiceProvider> consumer) {
    UUID uniqueId = packet.getBuffer().readOptionalUUID();
    String name = packet.getBuffer().readOptionalString();
    SpecificCloudServiceProvider provider = null;
    if (uniqueId != null) {
      provider = CloudNetDriver.getInstance().getCloudServiceProvider(uniqueId);
    } else if (name != null) {
      provider = CloudNetDriver.getInstance().getCloudServiceProvider(name);
    }

    ProtocolBuffer buffer = ProtocolBuffer.create();

    buffer.writeEnumConstant(provider != null && provider.isValid() ? ServiceDriverAPIResponse.SUCCESS
      : ServiceDriverAPIResponse.SERVICE_NOT_FOUND);

    if (provider != null && provider.isValid()) {
      consumer.accept(buffer, provider);
    }

    return buffer;
  }

}
