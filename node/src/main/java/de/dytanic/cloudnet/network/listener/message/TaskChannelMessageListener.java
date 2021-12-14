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

package de.dytanic.cloudnet.network.listener.message;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.driver.event.events.task.ServiceTaskRemoveEvent;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.provider.NodeServiceTaskProvider;
import java.lang.reflect.Type;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public final class TaskChannelMessageListener {

  private static final Type COL_TASKS = TypeToken.getParameterized(Collection.class, ServiceTask.class).getType();

  private final IEventManager eventManager;
  private final NodeServiceTaskProvider taskProvider;

  public TaskChannelMessageListener(
    @NotNull IEventManager eventManager,
    @NotNull NodeServiceTaskProvider taskProvider
  ) {
    this.eventManager = eventManager;
    this.taskProvider = taskProvider;
  }

  @EventListener
  public void handleChannelMessage(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL) && event.getMessage() != null) {
      switch (event.getMessage()) {
        // set tasks
        case "set_service_tasks" -> {
          Collection<ServiceTask> tasks = event.getContent().readObject(COL_TASKS);
          this.taskProvider.setPermanentServiceTasksSilently(tasks);
        }

        // add task
        case "add_service_task" -> {
          var task = event.getContent().readObject(ServiceTask.class);

          this.taskProvider.addPermanentServiceTaskSilently(task);
          this.eventManager.callEvent(new ServiceTaskAddEvent(task));
        }

        // remove task
        case "remove_service_task" -> {
          var task = event.getContent().readObject(ServiceTask.class);

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
