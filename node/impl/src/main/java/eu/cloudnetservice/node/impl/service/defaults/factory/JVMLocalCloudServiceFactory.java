/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.service.defaults.factory;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.service.InternalCloudServiceManager;
import eu.cloudnetservice.node.impl.service.defaults.JVMService;
import eu.cloudnetservice.node.impl.service.defaults.log.ProcessServiceLogReadScheduler;
import eu.cloudnetservice.node.impl.tick.DefaultTickLoop;
import eu.cloudnetservice.node.impl.version.ServiceVersionProvider;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class JVMLocalCloudServiceFactory extends BaseLocalCloudServiceFactory {

  protected final I18n i18n;
  protected final MeterRegistry meterRegistry;
  protected final DefaultTickLoop mainThread;
  protected final EventManager eventManager;
  protected final CloudServiceManager cloudServiceManager;
  protected final ProcessServiceLogReadScheduler processLogReadScheduler;

  @Inject
  public JVMLocalCloudServiceFactory(
    @NonNull @Service I18n i18n,
    @NonNull MeterRegistry meterRegistry,
    @NonNull DefaultTickLoop tickLoop,
    @NonNull Configuration nodeConfig,
    @NonNull CloudServiceManager cloudServiceManager,
    @NonNull EventManager eventManager,
    @NonNull ServiceVersionProvider versionProvider,
    @NonNull ProcessServiceLogReadScheduler processLogReadScheduler
  ) {
    super(nodeConfig, versionProvider);
    this.i18n = i18n;
    this.meterRegistry = meterRegistry;
    this.mainThread = tickLoop;
    this.eventManager = eventManager;
    this.cloudServiceManager = cloudServiceManager;
    this.processLogReadScheduler = processLogReadScheduler;
  }

  @Override
  public @NonNull CloudService createCloudService(
    @NonNull CloudServiceManager manager,
    @NonNull ServiceConfiguration configuration
  ) {
    var config = this.validateConfiguration(manager, configuration);
    var preparer = manager.servicePreparer(config.serviceId().environment());
    return new JVMService(
      this.i18n,
      this.mainThread,
      this.configuration,
      this.meterRegistry,
      config,
      (InternalCloudServiceManager) manager,
      this.eventManager,
      this.versionProvider,
      preparer,
      this.processLogReadScheduler);
  }

  @Override
  public @NonNull String name() {
    return "jvm";
  }
}
