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

package de.dytanic.cloudnet.driver.network.rpc.defaults;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class DefaultRPC implements RPC {

  private final RPCSender sender;
  private final String className;
  private final String methodName;
  private final Object[] arguments;

  private boolean resultExpectation = true;

  public DefaultRPC(RPCSender sender, String className, String methodName, Object[] arguments) {
    this.sender = sender;
    this.className = className;
    this.methodName = methodName;
    this.arguments = arguments;
  }

  @Override
  public @NotNull RPCSender getSender() {
    return this.sender;
  }

  @Override
  public @NotNull String getClassName() {
    return this.className;
  }

  @Override
  public @NotNull String getMethodeName() {
    return this.methodName;
  }

  @Override
  public @NotNull Object[] getArguments() {
    return this.arguments;
  }

  @Override
  public @NotNull RPC disableResultExpectation() {
    this.resultExpectation = false;
    return this;
  }

  @Override
  public void fireAndForget() {
    this.fireAndForget(Objects.requireNonNull(this.sender.getAssociatedComponent().getFirstChannel()));
  }

  @Override
  public <T> @NotNull T fireSync() {
    return this.fireSync(Objects.requireNonNull(this.sender.getAssociatedComponent().getFirstChannel()));
  }

  @Override
  public @NotNull <T> ITask<T> fire() {
    return this.fire(Objects.requireNonNull(this.sender.getAssociatedComponent().getFirstChannel()));
  }

  @Override
  public void fireAndForget(@NotNull INetworkChannel component) {
    this.disableResultExpectation().fireSync(component);
  }

  @Override
  public <T> @NotNull T fireSync(@NotNull INetworkChannel component) {
    return null;
  }

  @Override
  public @NotNull <T> ITask<T> fire(@NotNull INetworkChannel component) {
    return null;
  }
}
