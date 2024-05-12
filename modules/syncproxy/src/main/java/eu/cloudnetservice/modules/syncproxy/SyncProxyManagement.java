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

package eu.cloudnetservice.modules.syncproxy;

import eu.cloudnetservice.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import lombok.NonNull;

@RPCValidation
public interface SyncProxyManagement {

  @NonNull SyncProxyConfiguration configuration();

  void configuration(@NonNull SyncProxyConfiguration configuration);

  void registerService(@NonNull ServiceRegistry registry);
}
