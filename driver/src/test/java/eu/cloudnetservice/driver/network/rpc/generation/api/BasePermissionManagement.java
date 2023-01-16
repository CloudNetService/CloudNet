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

package eu.cloudnetservice.driver.network.rpc.generation.api;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import java.util.UUID;

public abstract class BasePermissionManagement implements PermissionManagement {

  static final PermissionUser VAL = PermissionUser.builder().name("Testing1234").uniqueId(UUID.randomUUID()).build();

  @Override
  public PermissionUser user(UUID uniqueId) {
    return VAL;
  }

  @Override
  public Task<PermissionUser> userAsync(UUID uniqueId) {
    return Task.completedTask(VAL);
  }
}
