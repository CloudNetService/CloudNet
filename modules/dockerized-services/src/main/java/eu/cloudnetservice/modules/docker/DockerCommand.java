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

package eu.cloudnetservice.modules.docker;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.node.Node;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.modules.docker.config.DockerImage;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig.Builder;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Description("Docker commands")
@CommandPermission("cloudnet.command.docker")
public class DockerCommand {

  @Parser(name = "dockerTask", suggestions = "dockerTask")
  public @NonNull ServiceTask dockerTaskParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var task = Node.instance().serviceTaskProvider().serviceTask(input.remove());
    if (task == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-tasks-task-not-found"));
    }
    // only allow tasks with the docker config
    if (!task.properties().contains("docker")) {
      throw new ArgumentNotAvailableException(I18n.trans("module-docker-command-task-no-entry", task.name()));
    }
    return task;
  }

  @Suggestions("dockerTask")
  public @NonNull List<String> suggestDockerTasks(@NonNull CommandContext<?> $, @NonNull String input) {
    return Node.instance().serviceTaskProvider().serviceTasks()
      .stream()
      .filter(serviceTask -> serviceTask.properties().contains("docker"))
      .map(Nameable::name)
      .toList();
  }

  @CommandMethod("docker task <task> image <image>")
  public void handleImage(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task,
    @Argument("image") @NonNull DockerImage image
  ) {
    this.updateDocker(task, builder -> builder.javaImage(image));
  }

  @CommandMethod("docker task <task> add bind <bind>")
  public void addBind(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateDocker(task, builder -> builder.addBind(bind));
  }

  @CommandMethod("docker task <task> clear binds")
  public void clearBinds(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task
  ) {
    this.updateDocker(task, builder -> builder.binds(Set.of()));
  }

  @CommandMethod("docker task <task> remove bind <bind>")
  public void removeBind(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateDocker(task, builder -> builder.removeBind(bind));
  }

  @CommandMethod("docker task <task> add volume <volume>")
  public void addVolume(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateDocker(task, builder -> builder.addVolume(volume));
  }

  @CommandMethod("docker task <task> clear volumes")
  public void clearVolumes(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task
  ) {
    this.updateDocker(task, builder -> builder.volumes(Set.of()));
  }

  @CommandMethod("docker task <task> remove volume <volume>")
  public void removeVolumes(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateDocker(task, builder -> builder.removeVolume(volume));
  }

  @CommandMethod("docker task <task> add port <port> [protocol]")
  public void addExposedPort(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    var exposedPort = protocol == null ? new ExposedPort(port) : new ExposedPort(port, protocol);
    this.updateDocker(task, builder -> builder.addExposedPort(exposedPort));
  }

  @CommandMethod("docker task <task> clear ports")
  public void clearExposedPorts(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task
  ) {
    this.updateDocker(task, builder -> builder.exposedPorts(Set.of()));
  }

  @CommandMethod("docker task <task> remove <port> [protocol]")
  public void removeExposedPort(
    @NonNull CommandSource source,
    @Argument(value = "task", parserName = "dockerTask") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    var exposedPort = protocol == null ? new ExposedPort(port) : new ExposedPort(port, protocol);
    this.updateDocker(task, builder -> builder.removeExposedPort(exposedPort));
  }

  private void updateDocker(
    @NonNull ServiceTask serviceTask,
    @NonNull Function<Builder, Builder> modifier
  ) {
    // read the docker config from the task
    var property = serviceTask.properties().get("docker", TaskDockerConfig.class);
    // rewrite the config and update it in the cluster
    var task = ServiceTask
      .builder(serviceTask)
      .properties(serviceTask.properties()
        .append("docker", modifier.apply(TaskDockerConfig.builder(property)).build()))
      .build();
    Node.instance().serviceTaskProvider().addServiceTask(task);
  }

}
