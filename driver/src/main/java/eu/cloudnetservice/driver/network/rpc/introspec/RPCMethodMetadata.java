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

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCChained;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCNoResult;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCTimeout;
import io.leangen.geantyref.GenericTypeReflector;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record RPCMethodMetadata(
  boolean chained,
  boolean concrete,
  boolean asyncReturnType,
  boolean compilerGenerated,
  boolean executionResultIgnored,
  @NonNull String name,
  @NonNull Type returnType,
  @NonNull Type unwrappedReturnType,
  @NonNull Type[] parameterTypes,
  @NonNull MethodType methodType,
  @NonNull Class<?> definingClass,
  @Nullable Duration executionTimeout
) {

  static @NonNull RPCMethodMetadata fromMethod(@NonNull Method method) {
    // interpret rpc annotations
    var chained = method.isAnnotationPresent(RPCChained.class);
    var executionResultIgnored = method.isAnnotationPresent(RPCNoResult.class);
    var rpcTimeout = RPCClassMetadata.parseRPCTimeout(method.getAnnotation(RPCTimeout.class));

    // get the actual return type if the method returns a future
    var genericReturnType = method.getGenericReturnType();
    var unwrappedFutureReturnType = unwrapWrappedFutureType(method, genericReturnType);
    var unwrappedReturnType = Objects.requireNonNullElse(unwrappedFutureReturnType, genericReturnType);

    // ensure (at least a bit) that the types are specific enough to be properly transferred through the network
    validateBounds(method, unwrappedReturnType);
    var paramTypes = method.getGenericParameterTypes();
    for (var paramType : paramTypes) {
      validateBounds(method, paramType);
    }

    var concrete = !Modifier.isAbstract(method.getModifiers());
    var methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    return new RPCMethodMetadata(
      chained,
      concrete,
      unwrappedFutureReturnType != null,
      method.isSynthetic(),
      executionResultIgnored,
      method.getName(),
      method.getGenericReturnType(),
      unwrappedReturnType,
      paramTypes,
      methodType,
      method.getDeclaringClass(),
      rpcTimeout);
  }

  /**
   * Ensures that the given type is fully bound, throwing an exception if that is not the case. The given method will be
   * used as a hint to direct into the direction of the method that caused the issue.
   *
   * @param method the method that is related to the type being checked.
   * @param type   the type to check if fully bound.
   * @throws NullPointerException  if the given method or type is null.
   * @throws IllegalStateException if the given type is not fully bound.
   */
  private static void validateBounds(@NonNull Method method, @NonNull Type type) {
    var rawType = GenericTypeReflector.erase(type);
    if (!GenericTypeReflector.isFullyBound(type) || rawType == Object.class || rawType == Future.class) {
      throw new IllegalStateException(String.format(
        "method %s in %s has too lose bounds to be properly functional with RPC",
        method.getName(), method.getDeclaringClass().getName()));
    }
  }

  /**
   * Unwraps the first (and only) type parameter of the given type, if the raw type is a future, to get the actual
   * return type to use for network codec.
   *
   * @param method the method that is related to the type being unwrapped.
   * @param type   the type to unwrap the future return type of.
   * @return the return type of the given future, null if the given type is not a future.
   * @throws NullPointerException  if the given type or method is null.
   * @throws IllegalStateException if the given type has no or multiple type parameters while being a future.
   */
  private static @Nullable Type unwrapWrappedFutureType(@NonNull Method method, @NonNull Type type) {
    var rawType = GenericTypeReflector.erase(type);
    if (!Future.class.isAssignableFrom(rawType)) {
      // not a future
      return null;
    }

    if (rawType != Task.class
      && rawType != CompletableFuture.class
      && rawType != CompletionStage.class
      && rawType != Future.class) {
      // unsupported future type
      throw new IllegalStateException(String.format(
        "method %s in %s has unsupported future return type %s; must be Task, CompletableFuture, CompletionStage or Future",
        method.getName(), method.getDeclaringClass().getName(), rawType.getName()));
    }

    if (!(type instanceof ParameterizedType parameterizedType)) {
      // future type must be parameterized
      throw new IllegalStateException(String.format(
        "Future return type of method %s in %s is not parameterized",
        method.getName(), method.getDeclaringClass().getName()));
    }

    var typeArguments = parameterizedType.getActualTypeArguments();
    if (typeArguments.length != 1) {
      // future type must have 1 type argument, else we cannot decide which type argument is actually relevant
      throw new IllegalStateException(String.format(
        "Future return type of method %s in %s has %d parameter types, expected exactly 1",
        method.getName(), method.getDeclaringClass().getName(), typeArguments.length));
    }

    // use the first type argument (Future<ReturnType>)
    return typeArguments[0];
  }
}
