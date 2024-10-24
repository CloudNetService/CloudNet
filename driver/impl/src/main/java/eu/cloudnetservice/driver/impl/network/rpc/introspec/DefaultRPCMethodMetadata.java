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

package eu.cloudnetservice.driver.impl.network.rpc.introspec;

import eu.cloudnetservice.driver.impl.network.rpc.generation.RPCInternalInstanceFactory;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCChained;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCNoResult;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCTimeout;
import eu.cloudnetservice.driver.network.rpc.introspec.RPCMethodMetadata;
import io.leangen.geantyref.GenericTypeReflector;
import java.lang.invoke.MethodType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata information about a method that can be used with RPC.
 *
 * @param concrete               if the method is concretely implemented, false if the method is abstract.
 * @param asyncReturnType        if the method has an async return type (returns a supported future subtype).
 * @param compilerGenerated      if the method was inserted into the class by the compiler.
 * @param executionResultIgnored if the method was specifically annotated to not wait for the rpc execution.
 * @param name                   the name of the method.
 * @param returnType             the full generic return type of the method.
 * @param unwrappedReturnType    the return type of the method after unwrapping, e.g. if the returning an async type.
 * @param parameterTypes         the fully generic parameter types.
 * @param methodType             the method type of the method.
 * @param definingClass          the class in which the method is defined.
 * @param executionTimeout       the execution timeout of the method, null if not defined.
 * @param chainMetadata          the metadata for chain implementation, if annotated with {@link RPCChained}.
 * @since 4.0
 */
public record DefaultRPCMethodMetadata(
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
  @Nullable Duration executionTimeout,
  @Nullable MethodChainMetadata chainMetadata
) implements RPCMethodMetadata {

  /**
   * Constructs and validates a rpc method metadata based on the properties of the given method.
   *
   * @param method the method to construct the rpc method metadata for.
   * @return the constructed rpc method metadata.
   * @throws NullPointerException  if the given method is null.
   * @throws IllegalStateException if some precondition, to ensure functionality with rpc, fails.
   */
  static @NonNull DefaultRPCMethodMetadata fromMethod(@NonNull Method method) {
    // interpret rpc annotations
    var chainedAnnotation = method.getAnnotation(RPCChained.class);
    var executionResultIgnored = method.isAnnotationPresent(RPCNoResult.class);
    var rpcTimeout = RPCClassMetadata.parseRPCTimeout(method.getAnnotation(RPCTimeout.class));

    // get the actual return type if the method returns a future
    var genericReturnType = method.getGenericReturnType();
    var unwrappedFutureReturnType = unwrapWrappedFutureType(method, genericReturnType);
    var unwrappedReturnType = Objects.requireNonNullElse(unwrappedFutureReturnType, genericReturnType);
    var rawUnwrappedReturnType = GenericTypeReflector.erase(unwrappedReturnType);

    // ensure (at least a bit) that the types are specific enough to be properly transferred through the network
    validateBounds(method, unwrappedReturnType);
    var paramTypes = method.getGenericParameterTypes();
    for (var paramType : paramTypes) {
      validateBounds(method, paramType);
    }

    // extract chain rpc information
    var chainMetadata = extractAndValidateChainMeta(method, rawUnwrappedReturnType, chainedAnnotation);

    var concrete = !Modifier.isAbstract(method.getModifiers());
    var methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    return new DefaultRPCMethodMetadata(
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
      rpcTimeout,
      chainMetadata);
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
    if (!GenericTypeReflector.isFullyBound(type)
      || rawType == Object.class
      || rawType == CompletionStage.class
      || Future.class.isAssignableFrom(rawType)) {
      throw new IllegalStateException(String.format(
        "method %s in %s has too lose bounds to be properly functional with RPC",
        method.getName(), method.getDeclaringClass().getName()));
    }

    // validate array bounds
    if (rawType.isArray()) {
      validateBounds(method, rawType.getComponentType());
    } else if (type instanceof GenericArrayType genericArrayType) {
      validateBounds(method, genericArrayType.getGenericComponentType());
    }
  }

  /**
   * Extract and validate the metadata of the chain annotation information. Returns null if the given chain annotation
   * is null.
   *
   * @param method          the method on which the chain annotation is located.
   * @param rawReturnType   the raw return type of the method.
   * @param chainAnnotation the chain annotation instance located on the method.
   * @return the extracted method chain metadata.
   * @throws NullPointerException  if the given method or raw return type is null.
   * @throws IllegalStateException if chain annotation has invalid data provided.
   */
  private static @Nullable MethodChainMetadata extractAndValidateChainMeta(
    @NonNull Method method,
    @NonNull Class<?> rawReturnType,
    @Nullable RPCChained chainAnnotation
  ) {
    if (chainAnnotation == null) {
      // method is not annotated with chain metadata
      return null;
    }

    var givenChainBaseType = chainAnnotation.baseImplementation();
    var chainBaseImpl = givenChainBaseType == Object.class ? null : givenChainBaseType;
    if (chainBaseImpl != null && !rawReturnType.isAssignableFrom(chainBaseImpl)) {
      // base impl is not a valid subtype of the actual return type
      throw new IllegalStateException(String.format(
        "chain annotation for method %s in %s declared base type %s which is not a subtype of %s",
        method.getName(),
        method.getDeclaringClass().getName(),
        chainBaseImpl.getName(),
        rawReturnType.getName()));
    }

    if (chainBaseImpl != null) {
      // base impl will be used for generation, validate that instead of the raw return type
      RPCClassMetadata.validateTargetClass(chainBaseImpl);
      validateChainBaseClass(method, chainBaseImpl);
    } else {
      // base impl not present, validate the raw return type
      RPCClassMetadata.validateTargetClass(rawReturnType);
      validateChainBaseClass(method, rawReturnType);
    }

    // partly validate the chain parameter mappings
    var parameterMappings = chainAnnotation.parameterMapping();
    if (parameterMappings.length != 0) {
      if (parameterMappings.length % 2 != 0) {
        // as all entries must be mapped to a constructor index, so there are 2 entries for each parameter
        throw new IllegalStateException(String.format(
          "chain parameter mapping on method %s in %s must have a divisible by 2 mapping, got %d mappings",
          method.getName(), method.getDeclaringClass().getName(), parameterMappings.length));
      }

      if ((parameterMappings.length / 2) > method.getParameterCount()) {
        throw new IllegalStateException(String.format(
          "chain parameter mapping on method %s in %s defines more mapping than parameters, got %d mappings and %d params",
          method.getName(),
          method.getDeclaringClass().getName(),
          parameterMappings.length / 2,
          method.getParameterCount()));
      }

      var seenParamIndexes = new BitSet(parameterMappings.length / 2);
      var seenConstructorIndexes = new BitSet(parameterMappings.length / 2);
      for (var index = 0; index < parameterMappings.length; index += 2) {
        var paramIndex = parameterMappings[index];
        var constructorIndex = parameterMappings[index + 1];
        if (paramIndex >= method.getParameterCount()
          || constructorIndex < 0
          || paramIndex < RPCInternalInstanceFactory.SpecialArg.SPECIAL_ARG_MAX_INDEX) {
          // invalid parameter or constructor index provided
          throw new IllegalStateException(String.format(
            "chain parameter of method %s in %s mapped incorrectly, param index: %d, constructor index: %d",
            method.getName(), method.getDeclaringClass().getName(), paramIndex, constructorIndex));
        }

        if (seenParamIndexes.get(paramIndex) || seenConstructorIndexes.get(constructorIndex)) {
          // either the param index or constructor index was already used
          throw new IllegalStateException(String.format(
            "chain parameter on method %s in %s mapped twice, either from param %d or to constructor param %d",
            method.getName(), method.getDeclaringClass().getName(), paramIndex, constructorIndex));
        }

        // mark both indexes as used
        seenParamIndexes.set(paramIndex);
        seenConstructorIndexes.set(constructorIndex);
      }
    }

    var generationFlags = chainAnnotation.generationFlags();
    return new MethodChainMetadata(generationFlags, parameterMappings, chainBaseImpl);
  }

  /**
   * Validates that the given chain base class is valid and can be implemented by code generation.
   *
   * @param method        the method on which the chain annotation is located.
   * @param chainBaseImpl the chain base type which should be implemented.
   * @throws NullPointerException  if the given method or class is null.
   * @throws IllegalStateException if a validation error occurs.
   */
  private static void validateChainBaseClass(@NonNull Method method, @NonNull Class<?> chainBaseImpl) {
    if (chainBaseImpl.isSealed() || chainBaseImpl.isRecord()) {
      // record or sealed classes cannot be extended
      throw new IllegalStateException(String.format(
        "cannot implement class %s returned from %s in %s for chained rpc invocations: class is a record or sealed",
        method.getName(), method.getDeclaringClass().getName(), chainBaseImpl.getName()));
    }

    if (chainBaseImpl.getPackageName().startsWith("java.")) {
      // don't want to define classes in java core package, this is only meant for user code
      throw new IllegalStateException(String.format(
        "cannot implemented %s returned from %s in %s for chained rpc invocation: class is from java namespace",
        method.getName(), method.getDeclaringClass().getName(), chainBaseImpl.getName()));
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

    if (rawType != Future.class
      && rawType != CompletionStage.class
      && rawType != CompletableFuture.class) {
      // unsupported future type
      throw new IllegalStateException(String.format(
        "method %s in %s has unsupported future return type %s; must be CompletableFuture, CompletionStage or Future",
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

  /**
   * Metadata information for a chained invocation. Only present if the target method is annotated with
   * {@link RPCChained}. Note that the parameter mapping target constructor indexes are not validated.
   *
   * @param generationFlags        the flags to pass when generating the class implementation.
   * @param parameterMappings      the index mapping of method parameter to constructor parameter index.
   * @param baseImplementationType the base implementation type for the generation of the method.
   * @since 4.0
   */
  public record MethodChainMetadata(
    int generationFlags,
    int[] parameterMappings,
    @Nullable Class<?> baseImplementationType
  ) {

  }
}
