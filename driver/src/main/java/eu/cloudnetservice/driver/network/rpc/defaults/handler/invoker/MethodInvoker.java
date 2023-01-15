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

package eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A method invoker used by rpc to invoke a requested method. Method invokers should only get created once for a class,
 * never multiple times during rpc processing.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface MethodInvoker {

  /**
   * Invokes the target method of this invoker using the given arguments.
   *
   * @param arguments the arguments to use when invoking the target method.
   * @return the return value of the method invocation.
   * @throws NullPointerException if the given arguments array is null.
   */
  @Nullable Object callMethod(@NonNull Object... arguments);
}
