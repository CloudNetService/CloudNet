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

package eu.cloudnetservice.modules.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.event.EventManager;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.CloudServiceManager;
import de.dytanic.cloudnet.service.ServiceConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.JVMService;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import eu.cloudnetservice.modules.docker.config.DockerImage;
import eu.cloudnetservice.modules.docker.config.TaskDockerConfig;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;

public class DockerizedService extends JVMService {

  protected final DockerClient dockerClient;
  protected final DockerConfiguration configuration;
  protected final DockerizedServiceLogCache logCache;

  protected volatile String containerId;

  protected volatile PipedInputStream stdIn;
  protected volatile PipedOutputStream stdOut;
  protected volatile ResultCallback<?> stdHolder;

  protected DockerizedService(
    @NonNull ServiceConfiguration configuration,
    @NonNull CloudServiceManager manager,
    @NonNull EventManager eventManager,
    @NonNull CloudNet nodeInstance,
    @NonNull ServiceConfigurationPreparer serviceConfigurationPreparer,
    @NonNull DockerClient dockerClient,
    @NonNull DockerConfiguration dockerConfiguration
  ) {
    super(configuration, manager, eventManager, nodeInstance, serviceConfigurationPreparer);

    this.dockerClient = dockerClient;
    this.configuration = dockerConfiguration;

    super.logCache = this.logCache = new DockerizedServiceLogCache(nodeInstance, this);
    this.initLogHandler();
  }

  @Override
  public void runCommand(@NonNull String command) {
    if (this.stdOut != null) {
      try {
        this.stdOut.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        this.stdOut.flush();
      } catch (IOException exception) {
        LOGGER.fine("Unable to send command to docker container", exception);
      }
    }
  }

  @Override
  public @NonNull String runtime() {
    return this.configuration.factoryName();
  }

  @Override
  public boolean alive() {
    if (this.containerId != null) {
      try {
        // just check if the container is alive - we do not provide a health check because docker does just
        // restart the container if the health check fails. This does only check if the container is still running.
        // Ignore not found exceptions - the container might be gone due to concurrent calls.
        var result = this.dockerClient.inspectContainerCmd(this.containerId).withSize(false).exec().getState();
        return result.getRunning() != null && result.getRunning();
      } catch (NotFoundException exception) {
        LOGGER.fine("Unable to query status of container", exception);
        return false;
      }
    }
    // the container wasn't yet initialized
    return false;
  }

  @Override
  protected void doStartProcess(
    @NonNull List<String> arguments,
    @NonNull Path wrapperPath,
    @NonNull Path applicationFilePath
  ) {
    if (this.containerId == null) {
      // get the task specific options
      var image = this.readFromTaskConfig(TaskDockerConfig::javaImage).orElse(this.configuration.javaImage());
      var taskExposedPorts = this.readFromTaskConfig(TaskDockerConfig::exposedPorts).orElse(Set.of());

      // combine the task options with the global options
      var volumes = this.collectVolumes();
      var binds = this.collectBinds(wrapperPath);
      var exposedPorts = Lists.newArrayList(Iterables.concat(taskExposedPorts, this.configuration.exposedPorts()));
      // we need to expose the port of the service we're starting as well
      exposedPorts.add(new ExposedPort(this.serviceConfiguration.port(), InternetProtocol.TCP));

      try {
        // pull the requested image
        this.buildPullCommand(image).start().awaitCompletion();
      } catch (Exception exception) {
        LOGGER.severe("Unable to pull image " + image.imageName() + " from docker registry", exception);
        return;
      }

      // create the container and store the container id
      this.containerId = this.dockerClient.createContainerCmd(image.imageName())
        .withTty(false)
        .withStdinOpen(true)
        .withVolumes(volumes)
        .withEntrypoint(arguments)
        .withStopSignal("SIGTERM")
        .withExposedPorts(exposedPorts)
        .withName(this.serviceId().uniqueId().toString())
        .withWorkingDir(this.serviceDirectory.toAbsolutePath().toString())
        .withHostConfig(HostConfig.newHostConfig()
          .withBinds(binds)
          .withNetworkMode(this.configuration.network())
          .withRestartPolicy(RestartPolicy.noRestart()))
        .exec()
        .getId();
    }

    try {
      // start the container
      this.dockerClient.startContainerCmd(this.containerId).exec();

      // open the std streams
      this.stdOut = new PipedOutputStream();
      this.stdIn = new PipedInputStream(this.stdOut);

      // attach to the container
      this.stdHolder = this.dockerClient.attachContainerCmd(this.containerId)
        .withLogs(true)
        .withStdOut(true)
        .withStdErr(true)
        .withStdIn(this.stdIn)
        .withTimestamps(false)
        .withFollowStream(true)
        .exec(new ServiceLogCacheAdapter());
    } catch (NotModifiedException | IOException exception) {
      // the container might be running already
      LOGGER.fine("Unable to start container", exception);
    }
  }

  @Override
  protected void stopProcess() {
    if (this.containerId != null) {
      try {
        // try to stop the container - we can safely ignore exceptions when the container is not running anymore
        this.dockerClient.stopContainerCmd(this.containerId).withTimeout(10).exec();
      } catch (NotFoundException | NotModifiedException exception) {
        LOGGER.fine("Unable to stop service in docker container", exception);
      }

      try {
        this.stdHolder.close();
        // close the std streams
        this.stdIn.close();
        this.stdOut.close();
      } catch (IOException exception) {
        LOGGER.fine("Unable to close std stream", exception);
      }
    }
  }

  @Override
  public void doDelete() {
    // remove the container if the container exists
    if (this.containerId != null) {
      try {
        // try to remove the container, ignore if the container is already gone
        this.dockerClient.removeContainerCmd(this.containerId).withRemoveVolumes(false).withForce(true).exec();
        // remove the container id to prevent further unnecessary calls
        this.containerId = null;
      } catch (NotFoundException exception) {
        LOGGER.fine("Unable to remove docker container", exception);
      }
    }
    // remove all service files after stopping the container
    super.doDelete();
  }

  protected @NonNull Bind[] collectBinds(@NonNull Path wrapperFilePath) {
    Set<Bind> binds = new HashSet<>();

    // allow the container full access to the work directory and the wrapper file
    binds.add(this.bindFromPath(wrapperFilePath.toAbsolutePath().toString()));
    binds.add(this.bindFromPath(this.serviceDirectory.toAbsolutePath().toString()));

    // get the task specific volumes and concat them with the default volumes
    var taskBinds = this.readFromTaskConfig(TaskDockerConfig::binds).orElse(Set.of());
    binds.addAll(Stream.concat(taskBinds.stream(), this.configuration.binds().stream())
      .map(path -> this.serviceDirectory.resolve(path).toAbsolutePath().toString())
      .map(this::bindFromPath)
      .toList());

    // uses array instead of list to ensure that there are no duplicate binds
    return binds.toArray(Bind[]::new);
  }

  protected @NonNull Volume[] collectVolumes() {
    var taskVolumes = this.readFromTaskConfig(TaskDockerConfig::volumes).orElse(Set.of());
    return Stream.concat(this.configuration.volumes().stream(), taskVolumes.stream())
      .map(Volume::new)
      .distinct()
      .toArray(Volume[]::new);
  }

  protected @NonNull <T> Optional<T> readFromTaskConfig(@NonNull Function<TaskDockerConfig, T> reader) {
    var config = this.serviceConfiguration.properties().get("docker", TaskDockerConfig.class);
    return config == null ? Optional.empty() : Optional.ofNullable(reader.apply(config));
  }

  protected @NonNull PullImageCmd buildPullCommand(@NonNull DockerImage image) {
    var cmd = this.dockerClient.pullImageCmd(image.repository());
    // append the tag if given
    if (image.tag() != null) {
      cmd.withTag(image.tag());
    }
    // append the registry if given
    if (image.registry() != null) {
      cmd.withRegistry(image.registry());
    }
    // append the platform if given
    if (image.platform() != null) {
      cmd.withPlatform(image.platform());
    }
    return cmd;
  }

  protected @NonNull Bind bindFromPath(@NonNull String path) {
    return new Bind(path, new Volume(path));
  }

  public final class ServiceLogCacheAdapter extends Adapter<Frame> {

    @Override
    public void onNext(Frame object) {
      DockerizedService.this.logCache.handle(object);
    }
  }
}
