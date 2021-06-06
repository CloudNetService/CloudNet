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
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.util.Collection;

public class DriverServiceTaskListener extends CategorizedDriverAPIListener {

  public DriverServiceTaskListener() {
    super(DriverAPICategory.SERVICE_TASKS);

    super.registerHandler(DriverAPIRequestType.RELOAD_TASKS, (channel, packet, input) -> {
      this.provider().reload();
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.SET_PERMANENT_SERVICE_TASKS, (channel, packet, input) -> {
      this.provider().setPermanentServiceTasks(input.readObjectCollection(ServiceTask.class));
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.ADD_PERMANENT_SERVICE_TASK, (channel, packet, input) -> {
      boolean success = this.provider().addPermanentServiceTask(input.readObject(ServiceTask.class));
      return ProtocolBuffer.create().writeBoolean(success);
    });

    super.registerHandler(DriverAPIRequestType.REMOVE_PERMANENT_SERVICE_TASK, (channel, packet, input) -> {
      this.provider().removePermanentServiceTask(input.readString());
      return ProtocolBuffer.EMPTY;
    });

    super.registerHandler(DriverAPIRequestType.GET_PERMANENT_SERVICE_TASKS, (channel, packet, input) -> {
      Collection<ServiceTask> tasks = this.provider().getPermanentServiceTasks();
      return ProtocolBuffer.create().writeObjectCollection(tasks);
    });

    super.registerHandler(DriverAPIRequestType.GET_PERMANENT_SERVICE_TASK_BY_NAME, (channel, packet, input) -> {
      ServiceTask task = this.provider().getServiceTask(input.readString());
      return ProtocolBuffer.create().writeOptionalObject(task);
    });

    super.registerHandler(DriverAPIRequestType.IS_SERVICE_TASK_PRESENT, (channel, packet, input) -> {
      boolean present = this.provider().isServiceTaskPresent(input.readString());
      return ProtocolBuffer.create().writeBoolean(present);
    });

  }

  private ServiceTaskProvider provider() {
    return CloudNetDriver.getInstance().getServiceTaskProvider();
  }

}
