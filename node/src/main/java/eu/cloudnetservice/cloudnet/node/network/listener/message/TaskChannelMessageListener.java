/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.network.listener.message;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.task.ServiceTaskAddEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.task.ServiceTaskRemoveEvent;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.node.provider.NodeServiceTaskProvider;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;

public final class TaskChannelMessageListener {

  private static final Type COL_TASKS = TypeToken.getParameterized(Collection.class, ServiceTask.class).getType();

  private final EventManager eventManager;
  private final NodeServiceTaskProvider taskProvider;

  public TaskChannelMessageListener(
    @NonNull EventManager eventManager,
    @NonNull NodeServiceTaskProvider taskProvider
  ) {
    this.eventManager = eventManager;
    this.taskProvider = taskProvider;
  }

  @EventListener
  public void handleChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // set tasks
        case "set_service_tasks" -> {
          Collection<ServiceTask> tasks = event.content().readObject(COL_TASKS);
          this.taskProvider.permanentServiceTasksSilently(tasks);
        }

        // add task
        case "add_service_task" -> {
          var task = event.content().readObject(ServiceTask.class);

          this.taskProvider.addPermanentServiceTaskSilently(task);
          this.eventManager.callEvent(new ServiceTaskAddEvent(task));
        }

        // remove task
        case "remove_service_task" -> {
          var task = event.content().readObject(ServiceTask.class);

          this.taskProvider.removePermanentServiceTaskSilently(task);
          this.eventManager.callEvent(new ServiceTaskRemoveEvent(task));
        }

        // none of our business
        default -> {
        }
      }
    }
  }
}
