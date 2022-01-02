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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler;

import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCHandler.HandlingResult;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.MethodInformation;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record DefaultHandlingResult(
  boolean wasSuccessful,
  @Nullable Object invocationResult,
  @NonNull RPCHandler invocationHandler,
  @NonNull MethodInformation targetMethodInformation
) implements HandlingResult {

  public static @NonNull HandlingResult success(
    @NonNull MethodInformation methodInformation,
    @NonNull RPCHandler invocationHandler,
    @Nullable Object result
  ) {
    return new DefaultHandlingResult(true, result, invocationHandler, methodInformation);
  }

  public static @NonNull HandlingResult failure(
    @NonNull MethodInformation information,
    @NonNull RPCHandler invocationHandler,
    @NonNull Throwable result
  ) {
    return new DefaultHandlingResult(false, result, invocationHandler, information);
  }
}
