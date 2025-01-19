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

package eu.cloudnetservice.driver.impl.network.rpc.generation;

import eu.cloudnetservice.driver.impl.network.rpc.introspec.DefaultRPCMethodMetadata;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;

/**
 * An interface that is used to generate RPC method implementations.
 *
 * @since 4.0
 */
sealed interface RPCMethodGenerator permits BasicRPCMethodGenerator, ChainedRPCMethodGenerator {

  /**
   * Generates the method body.
   *
   * @param codeBuilder      the code builder of the method body.
   * @param generatingClass  the descriptor of the class that is being generated.
   * @param context          the generation context.
   * @param targetMethod     the target method that is being implemented.
   * @param targetMethodDesc the descriptor of the method that is being implemented.
   * @throws NullPointerException if one of the given parameters is null.
   */
  void generate(
    @NonNull CodeBuilder codeBuilder,
    @NonNull ClassDesc generatingClass,
    @NonNull RPCGenerationContext context,
    @NonNull DefaultRPCMethodMetadata targetMethod,
    @NonNull MethodTypeDesc targetMethodDesc);
}
