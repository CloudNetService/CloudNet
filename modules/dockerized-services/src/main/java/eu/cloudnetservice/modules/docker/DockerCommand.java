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
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.Quoted;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.node.Node;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.modules.docker.config.DockerImage;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig.Builder;
import java.util.Set;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Description("Docker commands")
@CommandPermission("cloudnet.command.docker")
public class DockerCommand {

  @CommandMethod("docker task <task> image <repository> [tag]")
  public void handleImage(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("repository") @NonNull String repository,
    @Argument("tag") @Nullable String tag,
    @Flag("registry") @Quoted @Nullable String registry,
    @Flag("platform") @Quoted @Nullable String platform
  ) {
    this.updateTaskDockerConfig(
      task,
      builder -> builder.javaImage(new DockerImage(repository, tag, registry, platform)));
  }

  @CommandMethod("docker task <task> remove image")
  public void removeImagine(@NonNull CommandSource source, @Argument("task") @NonNull ServiceTask task) {
    this.updateTaskDockerConfig(task, builder -> builder.javaImage(null));
  }

  @CommandMethod("docker task <task> add bind <bind>")
  public void addBind(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateTaskDockerConfig(task, builder -> builder.addBind(bind));
  }

  @CommandMethod("docker task <task> clear binds")
  public void clearBinds(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, builder -> builder.binds(Set.of()));
  }

  @CommandMethod("docker task <task> remove bind <bind>")
  public void removeBind(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateTaskDockerConfig(task, builder -> builder.removeBind(bind));
  }

  @CommandMethod("docker task <task> add volume <volume>")
  public void addVolume(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateTaskDockerConfig(task, builder -> builder.addVolume(volume));
  }

  @CommandMethod("docker task <task> clear volumes")
  public void clearVolumes(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, builder -> builder.volumes(Set.of()));
  }

  @CommandMethod("docker task <task> remove volume <volume>")
  public void removeVolumes(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateTaskDockerConfig(task, builder -> builder.removeVolume(volume));
  }

  @CommandMethod("docker task <task> add port <port> [protocol]")
  public void addExposedPort(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    var exposedPort = new ExposedPort(port, protocol == null ? InternetProtocol.DEFAULT : protocol);
    this.updateTaskDockerConfig(task, builder -> builder.addExposedPort(exposedPort));
  }

  @CommandMethod("docker task <task> clear ports")
  public void clearExposedPorts(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, builder -> builder.exposedPorts(Set.of()));
  }

  @CommandMethod("docker task <task> remove <port> [protocol]")
  public void removeExposedPort(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    var exposedPort = protocol == null ? new ExposedPort(port) : new ExposedPort(port, protocol);
    this.updateTaskDockerConfig(task, builder -> builder.removeExposedPort(exposedPort));
  }

  private void updateTaskDockerConfig(
    @NonNull ServiceTask serviceTask,
    @NonNull UnaryOperator<Builder> modifier
  ) {
    // read the docker config from the task
    var property = modifier.apply(TaskDockerConfig.builder(serviceTask.properties().get(
      "dockerConfig",
      TaskDockerConfig.class,
      new TaskDockerConfig(null, Set.of(), Set.of(), Set.of()))));
    // rewrite the config and update it in the cluster
    var task = ServiceTask.builder(serviceTask)
      .properties(serviceTask.properties().append("dockerConfig", property.build()))
      .build();
    Node.instance().serviceTaskProvider().addServiceTask(task);
  }
}
