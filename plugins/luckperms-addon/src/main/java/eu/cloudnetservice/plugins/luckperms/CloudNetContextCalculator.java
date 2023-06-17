/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.plugins.luckperms;

import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.StaticContextCalculator;

@Singleton
public class CloudNetContextCalculator implements StaticContextCalculator {

  private final WrapperConfiguration wrapperConfiguration;

  @Inject
  public CloudNetContextCalculator(@NonNull WrapperConfiguration wrapperConfiguration) {
    this.wrapperConfiguration = wrapperConfiguration;
  }

  @Override
  public void calculate(@NonNull ContextConsumer consumer) {
    consumer.accept("service", this.wrapperConfiguration.serviceConfiguration().serviceId().name());
    consumer.accept("task", this.wrapperConfiguration.serviceConfiguration().serviceId().taskName());
    consumer.accept("node", this.wrapperConfiguration.serviceConfiguration().serviceId().nodeUniqueId());
    consumer.accept("environment", this.wrapperConfiguration.serviceConfiguration().serviceId().environmentName());
  }
}
