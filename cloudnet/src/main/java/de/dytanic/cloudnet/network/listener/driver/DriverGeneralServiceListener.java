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
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import java.util.UUID;

public class DriverGeneralServiceListener extends CategorizedDriverAPIListener {

  public DriverGeneralServiceListener() {
    super(DriverAPICategory.GENERAL_CLOUD_SERVICES);

    super.registerHandler(DriverAPIRequestType.GET_SERVICES_AS_UNIQUE_ID, (channel, packet, input) -> {
      Collection<UUID> uuids = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesAsUniqueId();
      return ProtocolBuffer.create().writeUUIDCollection(uuids);
    });

    super.registerHandler(DriverAPIRequestType.GET_SERVICES_COUNT, (channel, packet, input) -> {
      int count = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesCount();
      return ProtocolBuffer.create().writeInt(count);
    });

    super.registerHandler(DriverAPIRequestType.GET_SERVICES_COUNT_BY_GROUP, (channel, packet, input) -> {
      int count = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesCountByGroup(input.readString());
      return ProtocolBuffer.create().writeInt(count);
    });

    super.registerHandler(DriverAPIRequestType.GET_SERVICES_COUNT_BY_TASK, (channel, packet, input) -> {
      int count = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesCountByTask(input.readString());
      return ProtocolBuffer.create().writeInt(count);
    });

    super.registerHandler(DriverAPIRequestType.GET_CLOUD_SERVICE_BY_NAME, (channel, packet, input) -> {
      ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceProvider()
        .getCloudServiceByName(input.readString());
      return ProtocolBuffer.create().writeOptionalObject(snapshot);
    });

    super.registerHandler(DriverAPIRequestType.GET_CLOUD_SERVICES, (channel, packet, input) -> {
      Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider()
        .getCloudServices();
      return ProtocolBuffer.create().writeObjectCollection(snapshots);
    });

    super.registerHandler(DriverAPIRequestType.GET_STARTED_CLOUD_SERVICES, (channel, packet, input) -> {
      Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider()
        .getStartedCloudServices();
      return ProtocolBuffer.create().writeObjectCollection(snapshots);
    });

    super.registerHandler(DriverAPIRequestType.GET_CLOUD_SERVICES_BY_SERVICE_TASK, (channel, packet, input) -> {
      Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider()
        .getCloudServices(input.readString());
      return ProtocolBuffer.create().writeObjectCollection(snapshots);
    });

    super.registerHandler(DriverAPIRequestType.GET_CLOUD_SERVICES_BY_GROUP, (channel, packet, input) -> {
      Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider()
        .getCloudServicesByGroup(input.readString());
      return ProtocolBuffer.create().writeObjectCollection(snapshots);
    });

    super.registerHandler(DriverAPIRequestType.GET_CLOUD_SERVICE_BY_UNIQUE_ID, (channel, packet, input) -> {
      ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceProvider()
        .getCloudService(input.readUUID());
      return ProtocolBuffer.create().writeOptionalObject(snapshot);
    });

    super.registerHandler(DriverAPIRequestType.GET_CLOUD_SERVICES_BY_ENVIRONMENT, (channel, packet, buffer) -> {
      ServiceEnvironmentType environment = buffer.readEnumConstant(ServiceEnvironmentType.class);
      Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider()
        .getCloudServices(environment);
      return ProtocolBuffer.create().writeObjectCollection(snapshots);
    });

  }
}
