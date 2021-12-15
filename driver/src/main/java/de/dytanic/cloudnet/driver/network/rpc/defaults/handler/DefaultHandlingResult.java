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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler;

import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler.HandlingResult;
import de.dytanic.cloudnet.driver.network.rpc.defaults.MethodInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DefaultHandlingResult(
  boolean wasSuccessful,
  @Nullable Object invocationResult,
  @NotNull RPCHandler invocationHandler,
  @NotNull MethodInformation targetMethodInformation
) implements HandlingResult {

  public static @NotNull HandlingResult success(
    @NotNull MethodInformation methodInformation,
    @NotNull RPCHandler invocationHandler,
    @Nullable Object result
  ) {
    return new DefaultHandlingResult(true, result, invocationHandler, methodInformation);
  }

  public static @NotNull HandlingResult failure(
    @NotNull MethodInformation information,
    @NotNull RPCHandler invocationHandler,
    @NotNull Throwable result
  ) {
    return new DefaultHandlingResult(false, result, invocationHandler, information);
  }
}
