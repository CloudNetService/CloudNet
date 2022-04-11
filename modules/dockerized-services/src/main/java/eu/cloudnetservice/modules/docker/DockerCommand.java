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
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import eu.cloudnetservice.modules.docker.config.DockerImage;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig.Builder;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandPermission("cloudnet.command.docker")
@Description("Administration of the docker module configuration")
public record DockerCommand(@NonNull DockerizedServicesModule module) {

  @CommandMethod("docker task <task> image <repository> [tag]")
  public void setImage(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("repository") @NonNull String repository,
    @Argument("tag") @Nullable String tag,
    @Flag("registry") @Quoted @Nullable String registry,
    @Flag("platform") @Quoted @Nullable String platform
  ) {
    this.updateTaskDockerConfig(
      task,
      ($, builder) -> builder.javaImage(new DockerImage(repository, tag, registry, platform)));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "javaImage",
      task.name(),
      String.format("%s:%s", repository, tag)));
  }

  @CommandMethod("docker task <task> remove image")
  public void removeImage(@NonNull CommandSource source, @Argument("task") @NonNull ServiceTask task) {
    this.updateTaskDockerConfig(task, ($, builder) -> builder.javaImage(null));
    source.sendMessage(I18n.trans("command-tasks-set-property-success", "javaImage", task.name(), "null"));
  }

  @CommandMethod("docker task <task> add bind <bind>")
  public void addBind(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateTaskDockerConfig(task, ($, builder) -> builder.addBind(bind));
    source.sendMessage(I18n.trans("command-tasks-add-collection-property", "bind", bind, task.name()));
  }

  @CommandMethod("docker task <task> clear binds")
  public void clearBinds(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, ($, builder) -> builder.binds(Set.of()));
    source.sendMessage(I18n.trans("command-tasks-clear-property", "binds", task.name()));
  }

  @CommandMethod("docker task <task> remove bind <bind>")
  public void removeBind(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateTaskDockerConfig(task, (config, builder) -> builder.binds(config.binds().stream()
      .filter(entry -> !entry.equals(bind))
      .collect(Collectors.toSet())));
    source.sendMessage(I18n.trans("command-tasks-remove-collection-property", "bind", bind, task.name()));
  }

  @CommandMethod("docker task <task> add volume <volume>")
  public void addVolume(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateTaskDockerConfig(task, ($, builder) -> builder.addVolume(volume));
    source.sendMessage(I18n.trans("command-tasks-add-collection-property", "volume", volume, task.name()));
  }

  @CommandMethod("docker task <task> clear volumes")
  public void clearVolumes(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, ($, builder) -> builder.volumes(Set.of()));
    source.sendMessage(I18n.trans("command-tasks-clear-property", "volumes", task.name()));
  }

  @CommandMethod("docker task <task> remove volume <volume>")
  public void removeVolumes(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateTaskDockerConfig(task, (config, builder) -> builder.volumes(config.volumes().stream()
      .filter(entry -> !entry.equals(volume))
      .collect(Collectors.toSet())));
    source.sendMessage(I18n.trans("command-tasks-remove-collection-property", "volume", volume, task.name()));
  }

  @CommandMethod("docker task <task> add port <port> [protocol]")
  public void addExposedPort(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    var exposedPort = new ExposedPort(port, protocol == null ? InternetProtocol.DEFAULT : protocol);
    this.updateTaskDockerConfig(task, ($, builder) -> builder.addExposedPort(exposedPort));
    source.sendMessage(I18n.trans("command-tasks-add-collection-property", "exposedPort", exposedPort, task.name()));
  }

  @CommandMethod("docker task <task> clear ports")
  public void clearExposedPorts(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, ($, builder) -> builder.exposedPorts(Set.of()));
    source.sendMessage(I18n.trans("command-tasks-clear-property", "exposedPorts", task.name()));
  }

  @CommandMethod("docker task <task> remove port <port> [protocol]")
  public void removeExposedPort(
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    this.updateTaskDockerConfig(task, (config, builder) -> builder.exposedPorts(config.exposedPorts().stream()
      .filter(entry -> entry.getPort() != port && (protocol == null || !protocol.equals(entry.getProtocol())))
      .collect(Collectors.toSet())));
    source.sendMessage(I18n.trans("command-tasks-remove-collection-property", "exposedPort", port, task.name()));
  }

  @CommandMethod("docker config network <network>")
  public void setNetwork(@NonNull CommandSource source, @Argument("network") @NonNull String network) {
    this.updateDockerConfig(($, builder) -> builder.network(network));
    source.sendMessage(I18n.trans("module-docker-command-set-success", "network", network));
  }

  @CommandMethod("docker config image <repository> [tag]")
  public void setImage(
    @NonNull CommandSource source,
    @Argument("repository") @NonNull String repository,
    @Argument("tag") @Nullable String tag,
    @Flag("registry") @Quoted @Nullable String registry,
    @Flag("platform") @Quoted @Nullable String platform
  ) {
    this.updateDockerConfig(($, builder) -> builder.javaImage(new DockerImage(repository, tag, registry, platform)));
    source.sendMessage(I18n.trans(
      "module-docker-command-set-success",
      "javaImage",
      String.format("%s:%s", repository, tag)));
  }

  @CommandMethod("docker config registry <registry>")
  public void setRegistry(
    @NonNull CommandSource source,
    @Argument("registry") @NonNull String registry,
    @Flag("user") @Quoted @Nullable String user,
    @Flag("email") @Quoted @Nullable String email,
    @Flag("password") @Quoted @Nullable String password
  ) {
    this.updateDockerConfig(($, builder) -> builder
      .registryUrl(registry)
      .registryUsername(user)
      .registryEmail(email)
      .registryPassword(password));
    source.sendMessage(I18n.trans("module-docker-command-set-success", "registry", registry));
  }

  @CommandMethod("docker config remove registry")
  public void removeRegistry(@NonNull CommandSource source) {
    this.updateDockerConfig(($, builder) -> builder
      .registryUrl(null)
      .registryUsername(null)
      .registryEmail(null)
      .registryPassword(null));
    source.sendMessage(I18n.trans("module-docker-command-remove-success", "registry"));
  }

  @CommandMethod("docker config user <user>")
  public void setUser(@NonNull CommandSource source, @Argument("user") @Greedy @NonNull String user) {
    this.updateDockerConfig(($, builder) -> builder.user(user));
    source.sendMessage(I18n.trans("module-docker-command-set-success", "user", user));
  }

  @CommandMethod("docker config remove user")
  public void removeUser(@NonNull CommandSource source) {
    this.updateDockerConfig(($, builder) -> builder.user(null));
    source.sendMessage(I18n.trans("module-docker-command-remove-success", "user"));
  }

  @CommandMethod("docker config add bind <bind>")
  public void addBind(
    @NonNull CommandSource source,
    @Argument("bind") String bind
  ) {
    this.updateDockerConfig(($, builder) -> builder.addBind(bind));
    source.sendMessage(I18n.trans("module-docker-command-add-collection-property", "bind", bind));
  }

  @CommandMethod("docker config clear binds")
  public void clearBinds(@NonNull CommandSource source) {
    this.updateDockerConfig(($, builder) -> builder.binds(Set.of()));
    source.sendMessage(I18n.trans("module-docker-command-clear-collection-property", "binds"));
  }

  @CommandMethod("docker config remove bind <bind>")
  public void removeBind(@NonNull CommandSource source, @Argument("bind") String bind) {
    this.updateDockerConfig((config, builder) -> builder.binds(config.binds().stream()
      .filter(entry -> !entry.equals(bind))
      .collect(Collectors.toSet())));
    source.sendMessage(I18n.trans("module-docker-command-remove-collection-property", "bind", bind));
  }

  @CommandMethod("docker config add volume <volume>")
  public void addVolume(@NonNull CommandSource source, @Argument("volume") String volume) {
    this.updateDockerConfig(($, builder) -> builder.addVolume(volume));
    source.sendMessage(I18n.trans("module-docker-command-add-collection-property", "volume", volume));
  }

  @CommandMethod("docker config clear volumes")
  public void clearVolumes(@NonNull CommandSource source) {
    this.updateDockerConfig(($, builder) -> builder.volumes(Set.of()));
    source.sendMessage(I18n.trans("module-docker-command-clear-collection-property", "volumes"));
  }

  @CommandMethod("docker config remove volume <volume>")
  public void removeVolumes(@NonNull CommandSource source, @Argument("volume") String volume) {
    this.updateDockerConfig((config, builder) -> builder.volumes(config.volumes().stream()
      .filter(entry -> !entry.equals(volume))
      .collect(Collectors.toSet())));
    source.sendMessage(I18n.trans("module-docker-command-remove-collection-property", "volume", volume));
  }

  @CommandMethod("docker config add port <port> [protocol]")
  public void addExposedPort(
    @NonNull CommandSource source,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    var exposedPort = new ExposedPort(port, protocol == null ? InternetProtocol.DEFAULT : protocol);
    this.updateDockerConfig(($, builder) -> builder.addExposedPort(exposedPort));
    source.sendMessage(I18n.trans("module-docker-command-add-collection-property", "exposedPort", exposedPort));
  }

  @CommandMethod("docker config clear ports")
  public void clearExposedPorts(@NonNull CommandSource source) {
    this.updateDockerConfig(($, builder) -> builder.exposedPorts(Set.of()));
    source.sendMessage(I18n.trans("module-docker-command-clear-collection-property", "exposedPorts"));
  }

  @CommandMethod("docker config remove port <port> [protocol]")
  public void removeExposedPort(
    @NonNull CommandSource source,
    @Argument("port") int port,
    @Argument("protocol") @Nullable InternetProtocol protocol
  ) {
    this.updateDockerConfig((config, builder) -> builder.exposedPorts(config.exposedPorts().stream()
      .filter(entry -> entry.getPort() != port && (protocol == null || !protocol.equals(entry.getProtocol())))
      .collect(Collectors.toSet())));
    source.sendMessage(I18n.trans("module-docker-command-remove-collection-property", "exposedPort", port));
  }

  private void updateTaskDockerConfig(
    @NonNull ServiceTask serviceTask,
    @NonNull BiFunction<TaskDockerConfig, Builder, Builder> modifier
  ) {
    // read the docker config from the task
    var taskConfig = serviceTask.properties().get(
      "dockerConfig",
      TaskDockerConfig.class,
      TaskDockerConfig.builder().build());
    var property = modifier.apply(taskConfig, TaskDockerConfig.builder(taskConfig));
    // rewrite the config and update it in the cluster
    var task = ServiceTask.builder(serviceTask)
      .properties(serviceTask.properties().append("dockerConfig", property.build()))
      .build();
    Node.instance().serviceTaskProvider().addServiceTask(task);
  }

  private void updateDockerConfig(
    @NonNull BiFunction<DockerConfiguration, DockerConfiguration.Builder, DockerConfiguration.Builder> modifier
  ) {
    var configuration = this.module.config();
    var newConfiguration = modifier.apply(configuration, DockerConfiguration.builder(configuration)).build();
    this.module.config(newConfiguration);
  }
}
