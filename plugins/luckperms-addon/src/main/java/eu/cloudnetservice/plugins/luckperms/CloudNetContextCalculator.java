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

import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.StaticContextCalculator;

public class CloudNetContextCalculator implements StaticContextCalculator {

  private final ServiceInfoHolder serviceInfoHolder;

  @Inject
  public CloudNetContextCalculator(ServiceInfoHolder serviceInfoHolder) {
    this.serviceInfoHolder = serviceInfoHolder;
  }

  @Override
  public void calculate(@NonNull ContextConsumer consumer) {
    consumer.accept("service", this.serviceInfoHolder.serviceInfo().serviceId().name());
    consumer.accept("task", this.serviceInfoHolder.serviceInfo().serviceId().taskName());
    consumer.accept("node", this.serviceInfoHolder.serviceInfo().serviceId().nodeUniqueId());
    consumer.accept("environment", this.serviceInfoHolder.serviceInfo().serviceId().environmentName());
  }
}
