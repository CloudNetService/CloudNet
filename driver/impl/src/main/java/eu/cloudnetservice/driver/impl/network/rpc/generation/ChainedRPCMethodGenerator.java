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
import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;
import eu.cloudnetservice.utils.base.CodeGenerationUtil;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.Objects;
import lombok.NonNull;

/**
 * A method generator that builds another implementation to chain the calls.
 *
 * @since 4.0
 */
final class ChainedRPCMethodGenerator implements RPCMethodGenerator {

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
    // validate that the parameter mappings of the rpc chain meta are valid
    var rpcChainMeta = Objects.requireNonNull(targetMethod.chainMetadata(), "invalid call to chained factory");
    var paramMappings = rpcChainMeta.parameterMappings();
    var targetReturnType = targetMethod.methodType().returnType();
    var chainBaseType = Objects.requireNonNullElse(rpcChainMeta.baseImplementationType(), targetReturnType);
    this.validateConstructorArgumentMapping(paramMappings, chainBaseType, targetMethod);

    // register the instance factory, construct the chain base & push the extra constructor parameters
    var additionalGenFlags = rpcChainMeta.generationFlags();
    var instanceFactoryFieldName = context.registerAdditionalInstanceFactory(
      additionalGenFlags,
      targetReturnType,
      chainBaseType);
    var chainBaseStoreSlot = this.storeRPCChainBase(codeBuilder, generatingClass, context, targetMethod);
    var extraArgsArrayStoreSlot = this.storeExtraParameterArray(paramMappings, targetMethod.methodType(), codeBuilder);

    // invoke the instance factory for the chained type
    var returnDescriptor = ClassDesc.ofDescriptor(targetReturnType.descriptorString());
    codeBuilder
      .aload(0)
      .getfield(generatingClass, instanceFactoryFieldName, RPCGenerationConstants.CD_INT_INSTANCE_FACTORY)
      .aload(chainBaseStoreSlot)
      .aload(0)
      .getfield(generatingClass, RPCGenerationConstants.FN_CHANNEL_SUPPLIER, RPCGenerationConstants.CD_SUPPLIER)
      .aload(extraArgsArrayStoreSlot)
      .invokevirtual(
        RPCGenerationConstants.CD_INT_INSTANCE_FACTORY,
        "constructInstance",
        RPCGenerationConstants.MTD_INT_INSTANCE_FACTORY_CONSTRUCT)
      .checkcast(returnDescriptor)
      .areturn();
  }

  /**
   * Constructs the base rpc chain for the chained invocation of the method and stores it into the returned slot.
   *
   * @param codeBuilder     the code builder of the method that is being generated.
   * @param generatingClass the class that is being generated.
   * @param context         the context of the current implementation run.
   * @param targetMethod    the target method that is currently being implemented.
   * @return the slot into which the base rpc instance was stored.
   * @throws NullPointerException if the given code builder, generating class, context or target method is null.
   */
  private int storeRPCChainBase(
    @NonNull CodeBuilder codeBuilder,
    @NonNull ClassDesc generatingClass,
    @NonNull RPCGenerationContext context,
    @NonNull DefaultRPCMethodMetadata targetMethod
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

    // construct the base rpc for the method & store it
    var chainBaseStoreSlot = codeBuilder.allocateLocal(TypeKind.ReferenceType);
    codeBuilder
      .invokevirtual(
        generatingClass,
        RPCGenerationConstants.MN_BRIDGE_INVOKE,
        RPCGenerationConstants.MTD_BRIDGE_RPC_INVOKE)
      .astore(chainBaseStoreSlot);
    return chainBaseStoreSlot;
  }

  /**
   * Stores the extra parameter for super constructor invocation according to the provided mappings into an object array
   * which is located at the returned slot.
   *
   * @param paramMapping the mapping of the method parameters to the constructor parameters.
   * @param methodType   the type of the method that is currently being implemented.
   * @param codeBuilder  the code builder of the method that is being generated.
   * @return the slot into which the extra parameter array was stored.
   * @throws NullPointerException if the given method type or code builder is null.
   */
  private int storeExtraParameterArray(
    int[] paramMapping,
    @NonNull MethodType methodType,
    @NonNull CodeBuilder codeBuilder
  ) {
    // allocate the array
    var mappingCount = paramMapping.length / 2;
    var extraArgsArrayStoreSlot = codeBuilder.allocateLocal(TypeKind.ReferenceType);
    codeBuilder
      .ldc(mappingCount)
      .anewarray(ConstantDescs.CD_Object)
      .astore(extraArgsArrayStoreSlot);

    for (var index = 0; index < paramMapping.length; index += 2) {
      var methodParamIndex = paramMapping[index];
      var constructorParamIndex = paramMapping[index + 1];

      // load the array & push the target array index
      codeBuilder.aload(extraArgsArrayStoreSlot).ldc(constructorParamIndex);

      if (methodParamIndex >= 0) {
        // actual parameter from method requested
        var paramType = methodType.parameterType(methodParamIndex);
        var paramSlot = codeBuilder.parameterSlot(methodParamIndex);
        if (paramType.isPrimitive()) {
          // box primitive type before storing
          var typeKind = TypeKind.fromDescriptor(paramType.descriptorString());
          codeBuilder.loadLocal(typeKind, paramSlot);
          CodeGenerationUtil.boxPrimitive(codeBuilder, paramType.descriptorString());
          codeBuilder.aastore();
        } else {
          // just load the type and store it
          codeBuilder.aload(paramSlot).aastore();
        }
      } else {
        // special argument requested
        var specialArg = RPCInternalInstanceFactory.SpecialArg.fromParamMappingIndex(methodParamIndex);
        codeBuilder
          .getstatic(
            RPCGenerationConstants.CD_INT_FACTORY_SPECIAL_ARG,
            specialArg.name(),
            RPCGenerationConstants.CD_INT_FACTORY_SPECIAL_ARG)
          .aastore();
      }
    }

    return extraArgsArrayStoreSlot;
  }

  /**
   * Ensures that the provided parameter mapping of the user for the method to constructor parameters is valid, by
   * resolving the target constructor that will be called and validating against that.
   *
   * @param paramMappings the provided parameter mappings.
   * @param chainBaseType the chain base type that will be used as the super class when generating.
   * @param targetMethod  the target method that is being implemented.
   * @throws NullPointerException  if the given chain base type or target method is null.
   * @throws IllegalStateException if the provided constructor parameter mappings are invalid.
   */
  private void validateConstructorArgumentMapping(
    int[] paramMappings,
    @NonNull Class<?> chainBaseType,
    @NonNull DefaultRPCMethodMetadata targetMethod
  ) {
    var constructors = chainBaseType.getDeclaredConstructors();
    for (var constructor : constructors) {
      if (constructor.isAnnotationPresent(RPCInvocationTarget.class)) {
        // found rpc invocation target, validate the constructor arguments
        this.validateConstructorArgumentMapping(paramMappings, targetMethod, constructor);
        return;
      }
    }

    // no target construct found which means the no-args fallback will be used. ensure that
    // there are no parameter mappings present in this case
    if (paramMappings.length > 0) {
      throw new IllegalStateException(String.format(
        "expected 0 argument mappings for chained return type %s from %s in %s, got %d",
        targetMethod.methodType().returnType().getName(),
        targetMethod.name(),
        targetMethod.definingClass().getName(),
        paramMappings.length / 2));
    }
  }

  /**
   * Validates that the given parameter mappings match with the method type of the target method and the given resolved
   * constructor from the class that is being implemented.
   *
   * @param paramMappings     the provided parameter mappings.
   * @param targetMethod      the target method that is being implemented.
   * @param targetConstructor the resolved constructor in the chain base class that will be used for invocations.
   * @throws NullPointerException  if the given target method or constructor is null.
   * @throws IllegalStateException if the provided constructor parameter mappings are invalid.
   */
  private void validateConstructorArgumentMapping(
    int[] paramMappings,
    @NonNull DefaultRPCMethodMetadata targetMethod,
    @NonNull Constructor<?> targetConstructor
  ) {
    var requiredParameterCount = paramMappings.length / 2;
    if (targetConstructor.getParameterCount() != requiredParameterCount) {
      // invalid parameter count
      throw new IllegalStateException(String.format(
        "target constructor in %s for chain implementation of %s in %s has not enough parameters (expected %d, got %d)",
        targetConstructor.getDeclaringClass().getName(),
        targetMethod.name(),
        targetMethod.definingClass().getName(),
        requiredParameterCount,
        targetConstructor.getParameterCount()));
    }

    var constructorParamTypes = targetConstructor.getParameterTypes();
    for (var index = 0; index < paramMappings.length; index += 2) {
      var methodParamIndex = paramMappings[index];
      var constructorParamIndex = paramMappings[index + 1];

      if (constructorParamIndex >= constructorParamTypes.length) {
        // invalid parameter index on constructor
        throw new IllegalStateException(String.format(
          "method %s in %s tries to map to constructor parameter %d which doesn't exist on target constructor in %s",
          targetMethod.name(),
          targetMethod.definingClass().getName(),
          constructorParamIndex,
          targetConstructor.getDeclaringClass().getName()));
      }

      var constructorParamType = constructorParamTypes[constructorParamIndex];
      if (methodParamIndex >= 0) {
        var methodParamType = targetMethod.methodType().parameterType(methodParamIndex);
        if (!constructorParamType.isAssignableFrom(methodParamType)) {
          // constructor defines a param at the index which is not assignable
          throw new IllegalStateException(String.format(
            "target constructor in %s defines param type %s at index %d, but method %s in %s tried to map param type %s (index %d)",
            targetConstructor.getDeclaringClass().getName(),
            constructorParamType.getName(),
            constructorParamIndex,
            targetMethod.name(),
            targetMethod.definingClass().getName(),
            methodParamType.getName(),
            methodParamIndex));
        }
      } else {
        // special parameter was requested
        var specialArg = RPCInternalInstanceFactory.SpecialArg.fromParamMappingIndex(methodParamIndex);
        if (!constructorParamType.isAssignableFrom(specialArg.argType())) {
          // constructor defines a param at the index which is not assignable
          throw new IllegalStateException(String.format(
            "target constructor in %s defines param type %s at index %d, but method %s in %s tried to map special type %s",
            targetConstructor.getDeclaringClass().getName(),
            constructorParamType.getName(),
            constructorParamIndex,
            targetMethod.name(),
            targetMethod.definingClass().getName(),
            specialArg.argType().getName()));
        }
      }
    }
  }
}
