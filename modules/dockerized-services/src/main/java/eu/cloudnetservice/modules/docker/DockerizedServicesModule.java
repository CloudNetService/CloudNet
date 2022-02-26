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

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import eu.cloudnetservice.modules.docker.config.DockerImage;
import java.time.Duration;
import java.util.Set;

public class DockerizedServicesModule extends DriverModule {

  private DockerConfiguration configuration;

  @ModuleTask
  public void loadConfiguration() {
    this.configuration = this.readConfig(DockerConfiguration.class, () -> new DockerConfiguration(
      "docker-jvm",
      "host",
      DockerImage.builder().repository("azul/zulu-openjdk").tag("17-jre-headless").build(),
      Set.of(),
      Set.of(),
      Set.of(),
      "unix:///var/run/docker.sock",
      null,
      null,
      null,
      null,
      null,
      null));
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
