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

import eu.cloudnetservice.driver.base.Named;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.time.Duration;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The metadata of a method that can be invoked using rpc.
 *
 * @since 4.0
 */
public interface RPCMethodMetadata extends Named {

  /**
   * Get if the method is concretely implemented, false if the method is abstract.
   *
   * @return true if the method is concretely implemented, false if the method is abstract.
   */
  boolean concrete();

  /**
   * Get if the method has an async return type (returns a supported future subtype).
   *
   * @return true if the method has an async return type (returns a supported future subtype).
   */
  boolean asyncReturnType();

  /**
   * Get if the method was specifically annotated to not wait for the rpc execution.
   *
   * @return true if the method was specifically annotated to not wait for the rpc execution.
   */
  boolean executionResultIgnored();

  /**
   * Get the full generic return type of the method.
   *
   * @return the full generic return type of the method.
   */
  @NonNull
  Type returnType();

  /**
   * Get the fully generic parameter types.
   *
   * @return the fully generic parameter types.
   */
  @NonNull
  Type[] parameterTypes();

  /**
   * Get the method type of the method.
   *
   * @return the method type of the method.
   */
  @NonNull
  MethodType methodType();

  /**
   * Get the class in which the method is defined.
   *
   * @return the class in which the method is defined.
   */
  @NonNull
  Class<?> definingClass();

  /**
   * Get the execution timeout of the method, null if not defined.
   *
   * @return the execution timeout of the method, null if not defined.
   */
  @Nullable
  Duration executionTimeout();
}
