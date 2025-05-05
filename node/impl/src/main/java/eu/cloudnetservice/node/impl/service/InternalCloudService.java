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

package eu.cloudnetservice.node.impl.service;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.node.service.CloudService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface InternalCloudService extends CloudService {

  void networkChannel(@Nullable NetworkChannel channel);

  void handleServiceRegister();

  void updateServiceInfoSnapshot(@NonNull ServiceInfoSnapshot serviceInfoSnapshot);
}
