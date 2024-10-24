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
import eu.cloudnetservice.utils.base.CodeGenerationUtil;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;

/**
 * A method generator that builds and executes an RPC directly.
 *
 * @since 4.0
 */
final class BasicRPCMethodGenerator implements RPCMethodGenerator {

  // method descriptor for "fire(NetworkChannel): CompletableFuture"
  private static final MethodTypeDesc MTD_FIRE = MethodTypeDesc.of(
    RPCGenerationConstants.CD_COMPLETABLE_FUTURE,
    RPCGenerationConstants.CD_NETWORK_CHANNEL);
  // method descriptor for "fireSync(NetworkChannel): Object"
  private static final MethodTypeDesc MTD_FIRE_SYNC = MethodTypeDesc.of(
    ConstantDescs.CD_Object,
    RPCGenerationConstants.CD_NETWORK_CHANNEL);
  // method descriptor for "fireAndForget(NetworkChannel): void"
  private static final MethodTypeDesc MTD_FIRE_AND_FORGET = MethodTypeDesc.of(
    ConstantDescs.CD_void,
    RPCGenerationConstants.CD_NETWORK_CHANNEL);

  /**
   * {@inheritDoc}
   */
  @Override
  public void generate(
    @NonNull CodeBuilder codeBuilder,
    @NonNull ClassDesc generatingClass,
    @NonNull RPCGenerationContext context,
    @NonNull DefaultRPCMethodMetadata targetMethod,
    @NonNull MethodTypeDesc targetMethodDesc
  ) {
    var typeDescriptorFieldName = context.registerTypeDescriptorField(targetMethod);
    codeBuilder
      // push method name to the stack
      .aload(0)
      .ldc(targetMethod.name())
      // load type descriptor of target method to the stack
      .aload(0)
      .getfield(generatingClass, typeDescriptorFieldName, RPCGenerationConstants.CD_TYPE_DESC);

    // push the arguments into an array
    RPCImplementationGenerator.generateObjectArgumentStore(codeBuilder, targetMethod.methodType());

    // get the rpc to execute and get the channel into which the request should be sent
    codeBuilder
      .invokevirtual(
        generatingClass,
        RPCGenerationConstants.MN_BRIDGE_INVOKE,
        RPCGenerationConstants.MTD_BRIDGE_RPC_INVOKE)
      .aload(0)
      .invokevirtual(
        generatingClass,
        RPCGenerationConstants.MN_BRIDGE_GET_CHANNEL,
        RPCGenerationConstants.MTD_BRIDGE_GET_CHANNEL);

    if (targetMethod.executionResultIgnored()) {
      // use fireAndForget to drop the method result
      codeBuilder.invokeinterface(RPCGenerationConstants.CD_RPC_EXECUTABLE, "fireAndForget", MTD_FIRE_AND_FORGET);

      // return a default value from the method
      var returnType = targetMethodDesc.returnType();
      var returnTypeKind = TypeKind.fromDescriptor(returnType.descriptorString());
      switch (returnTypeKind) {
        case VoidType -> codeBuilder.return_();
        case LongType -> codeBuilder.lconst_0().lreturn();
        case FloatType -> codeBuilder.fconst_0().freturn();
        case DoubleType -> codeBuilder.dconst_0().dreturn();
        case ReferenceType -> codeBuilder.aconst_null().areturn();
        case ByteType, ShortType, IntType, BooleanType, CharType -> codeBuilder.iconst_0().ireturn();
      }
    } else if (targetMethod.asyncReturnType()) {
      // async method execution
      codeBuilder
        .invokeinterface(RPCGenerationConstants.CD_RPC_EXECUTABLE, "fire", MTD_FIRE)
        .areturn();
    } else {
      // sync method execution
      var returnType = targetMethodDesc.returnType();
      codeBuilder.invokeinterface(RPCGenerationConstants.CD_RPC_EXECUTABLE, "fireSync", MTD_FIRE_SYNC);
      if (returnType.isPrimitive()) {
        var returnTypeKind = TypeKind.fromDescriptor(returnType.descriptorString());
        if (returnTypeKind == TypeKind.VoidType) {
          // void return type, nothing to return
          codeBuilder.pop().return_();
        } else {
          // unbox the primitive value
          CodeGenerationUtil.unboxPrimitive(codeBuilder, returnType.descriptorString());
          codeBuilder.return_(returnTypeKind);
        }
      } else {
        // cast & return the value
        codeBuilder.checkcast(returnType).areturn();
      }
    }
  }
}
