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
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import java.util.Collection;

public class DriverGroupListener extends CategorizedDriverAPIListener {

  public DriverGroupListener() {
    super(DriverAPICategory.GROUP_CONFIGURATIONS);

    super.registerHandler(DriverAPIRequestType.RELOAD_GROUPS, (channel, packet, input) -> {
      this.provider().reload();
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.SET_GROUP_CONFIGURATIONS, (channel, packet, input) -> {
      this.provider().setGroupConfigurations(input.readObjectCollection(GroupConfiguration.class));
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.ADD_GROUP_CONFIGURATION, (channel, packet, input) -> {
      this.provider().addGroupConfiguration(input.readObject(GroupConfiguration.class));
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.REMOVE_GROUP_CONFIGURATION, (channel, packet, input) -> {
      this.provider().removeGroupConfiguration(input.readString());
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.GET_GROUP_CONFIGURATIONS, (channel, packet, input) -> {
      Collection<GroupConfiguration> groups = this.provider().getGroupConfigurations();
      return ProtocolBuffer.create().writeObjectCollection(groups);
    });

    super.registerHandler(DriverAPIRequestType.GET_GROUP_CONFIGURATION_BY_NAME, (channel, packet, input) -> {
      GroupConfiguration group = this.provider().getGroupConfiguration(input.readString());
      return ProtocolBuffer.create().writeOptionalObject(group);
    });

    super.registerHandler(DriverAPIRequestType.IS_GROUP_CONFIGURATION_PRESENT, (channel, packet, input) -> {
      boolean present = this.provider().isGroupConfigurationPresent(input.readString());
      return ProtocolBuffer.create().writeBoolean(present);
    });

  }

  private GroupConfigurationProvider provider() {
    return CloudNetDriver.getInstance().getGroupConfigurationProvider();
  }
}
