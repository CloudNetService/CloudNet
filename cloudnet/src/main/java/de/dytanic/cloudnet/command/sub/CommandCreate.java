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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.Range;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;

public class CommandCreate {

  @CommandMethod("create by <task> <amount>")
  public void createByTask(
    CommandSource source,
    @Argument("task") ServiceTask task,
    @Argument("amount") @Range(min = "1") int amount,
    @Flag("start") boolean startService
  ) {
    ServiceConfiguration configuration = ServiceConfiguration.builder(task).build();
    for (int i = 0; i < amount; i++) {
      ServiceInfoSnapshot service = configuration.createNewService();
      if (service != null && startService) {
        service.provider().start();
      }
    }
  }

}
