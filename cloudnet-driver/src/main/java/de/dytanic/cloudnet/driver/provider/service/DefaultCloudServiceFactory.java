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

package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Just a wrapper to support the deprecated features, will be gone when these features are gone.
 */
@Deprecated
@ScheduledForRemoval
public abstract class DefaultCloudServiceFactory implements CloudServiceFactory {

  @Override
  public @Nullable Collection<ServiceInfoSnapshot> createCloudService(
    String nodeUniqueId,
    int amount,
    String name,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticService,
    Collection<ServiceRemoteInclusion> includes,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    Collection<String> groups,
    ProcessConfiguration processConfiguration,
    JsonDocument properties,
    Integer port
  ) {
    return IntStream.range(0, amount)
      .mapToObj($ -> this.createCloudService(ServiceConfiguration.builder()
        .node(nodeUniqueId)
        .task(name)
        .runtime(runtime)
        .autoDeleteOnStop(autoDeleteOnStop)
        .staticService(staticService)
        .inclusions(includes)
        .templates(templates)
        .deployments(deployments)
        .groups(groups)
        .maxHeapMemory(processConfiguration.getMaxHeapMemorySize())
        .jvmOptions(processConfiguration.getJvmOptions())
        .processParameters(processConfiguration.getProcessParameters())
        .environment(processConfiguration.getEnvironment())
        .properties(properties)
        .startPort(port == null ? processConfiguration.getEnvironment().getDefaultStartPort() : port)
        .build()))
      .collect(Collectors.toList());
  }
}
