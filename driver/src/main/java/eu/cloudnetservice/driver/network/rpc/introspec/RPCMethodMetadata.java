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

package eu.cloudnetservice.driver.network.rpc.introspec;

import eu.cloudnetservice.driver.network.rpc.annotation.RPCIgnore;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCNoResult;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCTimeout;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.Duration;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record RPCMethodMetadata(
  boolean ignored,
  boolean concrete,
  boolean compilerGenerated,
  boolean executionResultIgnored,
  @NonNull String name,
  @NonNull Type returnType,
  @NonNull Type[] parameterTypes,
  @NonNull MethodType methodType,
  @NonNull Class<?> definingClass,
  @Nullable Duration executionTimeout
) {

  static @NonNull RPCMethodMetadata fromMethod(@NonNull Method method) {
    // interpret rpc annotations
    var ignored = method.isAnnotationPresent(RPCIgnore.class);
    var executionResultIgnored = method.isAnnotationPresent(RPCNoResult.class);
    var rpcTimeout = RPCClassMetadata.parseRPCTimeout(method.getAnnotation(RPCTimeout.class));

    var concrete = !Modifier.isAbstract(method.getModifiers());
    var methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    return new RPCMethodMetadata(
      ignored,
      concrete,
      method.isSynthetic(),
      executionResultIgnored,
      method.getName(),
      method.getGenericReturnType(),
      method.getGenericParameterTypes(),
      methodType,
      method.getDeclaringClass(),
      rpcTimeout);
  }

  public @NonNull String descriptorString() {
    return this.name + this.methodType.descriptorString();
  }
}
