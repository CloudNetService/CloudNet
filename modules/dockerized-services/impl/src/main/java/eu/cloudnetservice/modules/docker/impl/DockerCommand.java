/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.docker.impl;

import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import eu.cloudnetservice.modules.docker.config.DockerImage;
import eu.cloudnetservice.modules.docker.config.DockerPortMapping;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.Nullable;

@Singleton
@Permission("cloudnet.command.docker")
@Description("module-docker-command-description")
public record DockerCommand(@NonNull DockerizedServicesModule module, @NonNull ServiceTaskProvider taskProvider) {

  @Command("docker task <task> image <repository> [tag]")
  public void setImage(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("repository") @NonNull String repository,
    @Argument("tag") @Nullable String tag,
    @Flag("registry") @Quoted @Nullable String registry,
    @Flag("platform") @Quoted @Nullable String platform
  ) {
    this.updateTaskDockerConfig(
      task,
      (_, builder) -> builder.javaImage(new DockerImage(repository, tag, registry, platform)));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "javaImage",
      task.name(),
      String.format("%s:%s", repository, tag)));
  }

  @Command("docker task <task> remove image")
  public void removeImage(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, (_, builder) -> builder.javaImage(null));
    source.sendMessage(i18n.translate("command-tasks-set-property-success", "javaImage", task.name(), "null"));
  }

  @Command("docker task <task> add bind <bind>")
  public void addBind(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateTaskDockerConfig(task, (_, builder) -> builder.addBind(bind));
    source.sendMessage(i18n.translate("command-tasks-add-collection-property", "bind", task.name(), bind));
  }

  @Command("docker task <task> clear binds")
  public void clearBinds(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, (_, builder) -> builder.binds(Set.of()));
    source.sendMessage(i18n.translate("command-tasks-clear-property", "binds", task.name()));
  }

  @Command("docker task <task> remove bind <bind>")
  public void removeBind(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("bind") String bind
  ) {
    this.updateTaskDockerConfig(task, (config, builder) -> builder.binds(config.binds().stream()
      .filter(entry -> !entry.equals(bind))
      .collect(Collectors.toSet())));
    source.sendMessage(i18n.translate("command-tasks-remove-collection-property", "bind", task.name(), bind));
  }

  @Command("docker task <task> add volume <volume>")
  public void addVolume(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateTaskDockerConfig(task, (_, builder) -> builder.addVolume(volume));
    source.sendMessage(i18n.translate("command-tasks-add-collection-property", "volume", task.name(), volume));
  }

  @Command("docker task <task> clear volumes")
  public void clearVolumes(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, (_, builder) -> builder.volumes(Set.of()));
    source.sendMessage(i18n.translate("command-tasks-clear-property", "volumes", task.name()));
  }

  @Command("docker task <task> remove volume <volume>")
  public void removeVolumes(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("volume") String volume
  ) {
    this.updateTaskDockerConfig(task, (config, builder) -> builder.volumes(config.volumes().stream()
      .filter(entry -> !entry.equals(volume))
      .collect(Collectors.toSet())));
    source.sendMessage(i18n.translate("command-tasks-remove-collection-property", "volume", task.name(), volume));
  }

  @Command("docker task <task> add port <port> [protocol]")
  public void addExposedPort(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable DockerPortMapping.Protocol protocol
  ) {
    var portMapping = new DockerPortMapping(protocol == null ? DockerPortMapping.Protocol.TCP : protocol, port);
    this.updateTaskDockerConfig(task, (_, builder) -> builder.addExposedPort(portMapping));
    source.sendMessage(
      i18n.translate("command-tasks-add-collection-property", "exposedPort", task.name(), portMapping));
  }

  @Command("docker task <task> clear ports")
  public void clearExposedPorts(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task
  ) {
    this.updateTaskDockerConfig(task, (_, builder) -> builder.exposedPorts(Set.of()));
    source.sendMessage(i18n.translate("command-tasks-clear-property", "exposedPorts", task.name()));
  }

  @Command("docker task <task> remove port <port> [protocol]")
  public void removeExposedPort(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("task") @NonNull ServiceTask task,
    @Argument("port") int port,
    @Argument("protocol") @Nullable DockerPortMapping.Protocol protocol
  ) {
    this.updateTaskDockerConfig(task, (config, builder) -> builder.exposedPorts(config.exposedPorts().stream()
      .filter(entry -> entry.port() != port && (protocol == null || !protocol.equals(entry.protocol())))
      .collect(Collectors.toSet())));
    source.sendMessage(i18n.translate("command-tasks-remove-collection-property", "exposedPort", task.name(), port));
  }

  @Command("docker config network <network>")
  public void setNetwork(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("network") @NonNull String network
  ) {
    this.updateDockerConfig((_, builder) -> builder.network(network));
    source.sendMessage(i18n.translate("module-docker-command-set-success", "network", network));
  }

  @Command("docker config image <repository> [tag]")
  public void setImage(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("repository") @NonNull String repository,
    @Argument("tag") @Nullable String tag,
    @Flag("registry") @Quoted @Nullable String registry,
    @Flag("platform") @Quoted @Nullable String platform
  ) {
    this.updateDockerConfig((_, builder) -> builder.javaImage(new DockerImage(repository, tag, registry, platform)));
    source.sendMessage(i18n.translate(
      "module-docker-command-set-success",
      "javaImage",
      String.format("%s:%s", repository, tag)));
  }

  @Command("docker config registry <registry>")
  public void setRegistry(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("registry") @NonNull String registry,
    @Flag("user") @Quoted @Nullable String user,
    @Flag("email") @Quoted @Nullable String email,
    @Flag("password") @Quoted @Nullable String password
  ) {
    this.updateDockerConfig((_, builder) -> builder
      .registryUrl(registry)
      .registryUsername(user)
      .registryEmail(email)
      .registryPassword(password));
    source.sendMessage(i18n.translate("module-docker-command-set-success", "registry", registry));
  }

  @Command("docker config remove registry")
  public void removeRegistry(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    this.updateDockerConfig((_, builder) -> builder
      .registryUrl(null)
      .registryUsername(null)
      .registryEmail(null)
      .registryPassword(null));
    source.sendMessage(i18n.translate("module-docker-command-remove-success", "registry"));
  }

  @Command("docker config user <user>")
  public void setUser(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("user") @Greedy @NonNull String user
  ) {
    this.updateDockerConfig((_, builder) -> builder.user(user));
    source.sendMessage(i18n.translate("module-docker-command-set-success", "user", user));
  }

  @Command("docker config remove user")
  public void removeUser(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    this.updateDockerConfig((_, builder) -> builder.user(null));
    source.sendMessage(i18n.translate("module-docker-command-remove-success", "user"));
  }

  @Command("docker config add bind <bind>")
  public void addBind(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("bind") String bind
  ) {
    this.updateDockerConfig((_, builder) -> builder.addBind(bind));
    source.sendMessage(i18n.translate("module-docker-command-add-collection-property", "bind", bind));
  }

  @Command("docker config clear binds")
  public void clearBinds(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    this.updateDockerConfig((_, builder) -> builder.binds(Set.of()));
    source.sendMessage(i18n.translate("module-docker-command-clear-collection-property", "binds"));
  }

  @Command("docker config remove bind <bind>")
  public void removeBind(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("bind") String bind
  ) {
    this.updateDockerConfig((config, builder) -> builder.binds(config.binds().stream()
      .filter(entry -> !entry.equals(bind))
      .collect(Collectors.toSet())));
    source.sendMessage(i18n.translate("module-docker-command-remove-collection-property", "bind", bind));
  }

  @Command("docker config add volume <volume>")
  public void addVolume(@NonNull @Service I18n i18n, @NonNull CommandSource source, @Argument("volume") String volume) {
    this.updateDockerConfig((_, builder) -> builder.addVolume(volume));
    source.sendMessage(i18n.translate("module-docker-command-add-collection-property", "volume", volume));
  }

  @Command("docker config clear volumes")
  public void clearVolumes(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    this.updateDockerConfig((_, builder) -> builder.volumes(Set.of()));
    source.sendMessage(i18n.translate("module-docker-command-clear-collection-property", "volumes"));
  }

  @Command("docker config remove volume <volume>")
  public void removeVolumes(@NonNull @Service I18n i18n, @NonNull CommandSource source, @Argument("volume") String volume) {
    this.updateDockerConfig((config, builder) -> builder.volumes(config.volumes().stream()
      .filter(entry -> !entry.equals(volume))
      .collect(Collectors.toSet())));
    source.sendMessage(i18n.translate("module-docker-command-remove-collection-property", "volume", volume));
  }

  @Command("docker config add port <port> [protocol]")
  public void addExposedPort(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("port") int port,
    @Argument("protocol") @Nullable DockerPortMapping.Protocol protocol
  ) {
    var portMapping = new DockerPortMapping(protocol == null ? DockerPortMapping.Protocol.TCP : protocol, port);
    this.updateDockerConfig((_, builder) -> builder.addExposedPort(portMapping));
    source.sendMessage(i18n.translate("module-docker-command-add-collection-property", "exposedPort", portMapping));
  }

  @Command("docker config clear ports")
  public void clearExposedPorts(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    this.updateDockerConfig((_, builder) -> builder.exposedPorts(Set.of()));
    source.sendMessage(i18n.translate("module-docker-command-clear-collection-property", "exposedPorts"));
  }

  @Command("docker config remove port <port> [protocol]")
  public void removeExposedPort(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @Argument("port") int port,
    @Argument("protocol") @Nullable DockerPortMapping.Protocol protocol
  ) {
    this.updateDockerConfig((config, builder) -> builder.exposedPorts(config.exposedPorts().stream()
      .filter(entry -> entry.port() != port && (protocol == null || !protocol.equals(entry.protocol())))
      .collect(Collectors.toSet())));
    source.sendMessage(i18n.translate("module-docker-command-remove-collection-property", "exposedPort", port));
  }

  private void updateTaskDockerConfig(
    @NonNull ServiceTask serviceTask,
    @NonNull BiFunction<TaskDockerConfig, TaskDockerConfig.Builder, TaskDockerConfig.Builder> modifier
  ) {
    // read the docker config from the task
    var taskConfig = serviceTask.propertyHolder().readObject(
      "dockerConfig",
      TaskDockerConfig.class,
      TaskDockerConfig.builder().build());
    var property = modifier.apply(taskConfig, TaskDockerConfig.builder(taskConfig));

    // rewrite the config and update it in the cluster
    var task = ServiceTask.builder(serviceTask)
      .modifyProperties(properties -> properties.append("dockerConfig", property.build()))
      .build();
    this.taskProvider.addServiceTask(task);
  }

  private void updateDockerConfig(
    @NonNull BiFunction<DockerConfiguration, DockerConfiguration.Builder, DockerConfiguration.Builder> modifier
  ) {
    var configuration = this.module.config();
    var newConfiguration = modifier.apply(configuration, DockerConfiguration.builder(configuration)).build();
    this.module.config(newConfiguration);
  }
}
