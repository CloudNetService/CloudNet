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

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Set;

public class DockerizedServicesModule extends DriverModule {

  private DockerConfiguration configuration;

  @ModuleTask
  public void loadConfiguration() {
    if (Files.notExists(this.configPath())) {
      this.configuration = new DockerConfiguration(
        "docker-jvm",
        "azul/zulu-openjdk-alpine:17-jre",
        Set.of("plugins/", "world/", "world_nether/", "world_the_end/"),
        Set.of(),
        "unix:///var/run/docker.sock",
        null,
        null,
        null,
        null,
        null);
      JsonDocument.newDocument(this.configuration).write(this.configPath());
    } else {
      this.configuration = JsonDocument.newDocument(this.configPath()).toInstanceOf(DockerConfiguration.class);
    }
  }

  @ModuleTask(order = 22)
  public void registerServiceFactory() {
    var clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
      .withDockerHost(this.configuration.dockerHost())
      .withDockerCertPath(this.configuration.dockerCertPath())
      .withDockerTlsVerify(this.configuration.dockerCertPath() != null)
      .withRegistryUsername(this.configuration.registryUsername())
      .withRegistryEmail(this.configuration.registryEmail())
      .withRegistryPassword(this.configuration.registryPassword())
      .withRegistryUrl(this.configuration.registryUrl())
      .build();
    var dockerHttpClient = new ApacheDockerHttpClient.Builder()
      .dockerHost(clientConfig.getDockerHost())
      .sslConfig(clientConfig.getSSLConfig())
      .connectionTimeout(Duration.ofSeconds(30))
      .responseTimeout(Duration.ofSeconds(30))
      .build();
    // create the client and instantiate the service factory based on the information
    var dockerClient = DockerClientImpl.getInstance(clientConfig, dockerHttpClient);
    CloudNet.instance().cloudServiceProvider().addCloudServiceFactory(
      this.configuration.factoryName(),
      new DockerizedServiceFactory(
        CloudNet.instance(),
        this.eventManager(),
        dockerClient,
        this.configuration));
  }

  @ModuleTask(event = ModuleLifeCycle.STOPPED)
  public void unregisterServiceFactory() {
    CloudNet.instance().cloudServiceProvider().removeCloudServiceFactory(this.configuration.factoryName());
  }
}
