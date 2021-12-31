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

package de.dytanic.cloudnet.driver.network.rpc.defaults;

import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCProvider;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import lombok.NonNull;

public abstract class DefaultRPCProvider implements RPCProvider {

  protected final Class<?> targetClass;
  protected final ObjectMapper objectMapper;
  protected final DataBufFactory dataBufFactory;

  protected DefaultRPCProvider(
    @NonNull Class<?> targetClass,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    this.targetClass = targetClass;
    this.objectMapper = objectMapper;
    this.dataBufFactory = dataBufFactory;
  }

  @Override
  public @NonNull Class<?> targetClass() {
    return this.targetClass;
  }

  @Override
  public @NonNull ObjectMapper objectMapper() {
    return this.objectMapper;
  }

  @Override
  public @NonNull DataBufFactory dataBufFactory() {
    return this.dataBufFactory;
  }
}
