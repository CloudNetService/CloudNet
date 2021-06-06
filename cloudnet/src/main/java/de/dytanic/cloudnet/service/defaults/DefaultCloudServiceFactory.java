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

package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

public class DefaultCloudServiceFactory implements ICloudServiceFactory {

  private final String runtime;
  private final BiFunction<ICloudServiceManager, ServiceConfiguration, ICloudService> factoryFunction;

  public DefaultCloudServiceFactory(String runtime,
    BiFunction<ICloudServiceManager, ServiceConfiguration, ICloudService> factoryFunction) {
    this.runtime = runtime;
    this.factoryFunction = factoryFunction;
  }

  @Override
  public @NotNull String getRuntime() {
    return this.runtime;
  }

  @Override
  public ICloudService createCloudService(@NotNull ICloudServiceManager cloudServiceManager,
    @NotNull ServiceConfiguration serviceConfiguration) {
    return this.factoryFunction.apply(cloudServiceManager, serviceConfiguration);
  }
}
