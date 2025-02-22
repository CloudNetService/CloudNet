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

import eu.cloudnetservice.driver.impl.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;
import eu.cloudnetservice.utils.base.CodeGenerationUtil;
import io.vavr.Tuple2;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import lombok.NonNull;

/**
 * The generator for rpc api implementations.
 *
 * @since 4.0
 */
final class RPCImplementationGenerator {

  // flag that indicates if concrete methods should be implemented
  static final int FLAG_IMPLEMENT_CONCRETE = 0x01;

  // method implementation generators
  private static final RPCMethodGenerator BASIC_GENERATOR = new BasicRPCMethodGenerator();
  private static final RPCMethodGenerator CHAINED_GENERATOR = new ChainedRPCMethodGenerator();

  private final int generationFlags;
  private final RPCClassMetadata targetClassMeta;

  private final ClassDesc generatingClass;
  private final RPCGenerationContext context;

  /**
   * Constructs a new rpc implementation generator.
   *
   * @param context         the generation context to use for the process.
   * @param targetClassMeta the metadata of the class for which the implementation is being generated.
   * @param generationFlags the option flags to customize the generation process.
   * @throws NullPointerException if the given context or target class meta is null.
   */
  RPCImplementationGenerator(
    @NonNull RPCGenerationContext context,
    @NonNull RPCClassMetadata targetClassMeta,
    int generationFlags
  ) {
    this.targetClassMeta = targetClassMeta;
    this.generationFlags = generationFlags;
    this.context = context;

    // generate the class name based on the target class
    var targetClassDesc = ClassDesc.of(targetClassMeta.targetClass().getName());
    this.generatingClass = targetClassDesc.nested("RPC_BRIDGE");
  }

  /**
   * Generates a set of instructions into the given code builder that stores all parameters of the given method into an
   * object array.
   *
   * @param codeBuilder the code builder into which the parameter store instruction should be generated.
   * @param methodType  the information about the method for which the instructions are generated.
   * @throws NullPointerException if the given code builder or method type is null.
   */
  static void generateObjectArgumentStore(@NonNull CodeBuilder codeBuilder, @NonNull MethodType methodType) {
    // construct the new array
    var paramCount = methodType.parameterCount();
    codeBuilder.ldc(paramCount).anewarray(ConstantDescs.CD_Object);

    for (var index = 0; index < paramCount; index++) {
      var paramType = methodType.parameterType(index);
      var parameterSlot = codeBuilder.parameterSlot(index);
      if (paramType.isPrimitive()) {
        // special handling for primitive types: different load opcode & needs boxing before storing in array
        var descriptor = paramType.descriptorString();
        var typeKind = TypeKind.fromDescriptor(descriptor);
        codeBuilder
          .dup()
          .ldc(index)
          .loadLocal(typeKind, parameterSlot);
        CodeGenerationUtil.boxPrimitive(codeBuilder, descriptor);
        codeBuilder.aastore();
      } else {
        // simple object type
        codeBuilder.dup().ldc(index).aload(parameterSlot).aastore();
      }
    }
  }

  /**
   * Runs the actual class implementation process.
   *
   * @return a method handle with full access to the generated target class.
   * @throws IllegalStateException if something went wrong generating the implementation class.
   */
  public @NonNull MethodHandles.Lookup generateImplementation() {
    // resolve the base constructor in the target class
    var baseConstructorInformation = this.resolveBaseConstructorInformation();
    this.context.superclassConstructorDesc = baseConstructorInformation._2();

    var classFileBytes = ClassFile.of().build(this.generatingClass, classBuilder -> {
      // set the superclass and optional the superinterface
      var targetClass = this.targetClassMeta.targetClass();
      var superDesc = ClassDesc.of(targetClass.getName());
      if (targetClass.isInterface()) {
        classBuilder.withInterfaceSymbols(superDesc);
      } else {
        classBuilder.withSuperclass(superDesc);
      }

      // add base rpc fields
      classBuilder.withField(
        RPCGenerationConstants.FN_RPC_SENDER,
        RPCGenerationConstants.CD_RPC_SENDER,
        RPCGenerationConstants.AFM_FIELD_PF);
      classBuilder.withField(
        RPCGenerationConstants.FN_BASE_RPC,
        RPCGenerationConstants.CD_CHAINABLE_RPC,
        RPCGenerationConstants.AFM_FIELD_PF);
      classBuilder.withField(
        RPCGenerationConstants.FN_CHANNEL_SUPPLIER,
        RPCGenerationConstants.CD_SUPPLIER,
        RPCGenerationConstants.AFM_FIELD_PF);

      // add base rpc methods
      this.generateBridgeRPCInvokeMethod(classBuilder);
      this.generateBridgeChannelGetterMethod(classBuilder);

      // implement the methods
      var methods = this.targetClassMeta.methods();
      for (var method : methods) {
        if (method.concrete() && !this.flagEnabled(FLAG_IMPLEMENT_CONCRETE)) {
          // skip concrete methods if they shouldn't be implemented
          continue;
        }

        // get the descriptor for the method & add the method to the class
        var methodDescriptorString = method.methodType().descriptorString();
        var methodDescriptor = MethodTypeDesc.ofDescriptor(methodDescriptorString);
        classBuilder.withMethodBody(
          method.name(),
          methodDescriptor,
          ClassFile.ACC_PUBLIC,
          code -> {
            var chainMeta = method.chainMetadata();
            if (chainMeta == null) {
              BASIC_GENERATOR.generate(code, this.generatingClass, this.context, method, methodDescriptor);
            } else {
              CHAINED_GENERATOR.generate(code, this.generatingClass, this.context, method, methodDescriptor);
            }
          });
      }

      // add the constructor to the class
      this.generateConstructor(classBuilder, baseConstructorInformation._1(), baseConstructorInformation._2());
    });

    // define the class
    return CodeGenerationUtil.defineNestedClass(this.targetClassMeta.targetClass(), classFileBytes);
  }

  /**
   * Checks if the given flag is enabled in this implementation generator.
   *
   * @param flag the flag to check the status of.
   * @return true if the given flag is enabled, false otherwise.
   */
  private boolean flagEnabled(int flag) {
    return (this.generationFlags & flag) != 0;
  }

  /**
   * Tries to find the super constructor that should be invoked from the generated implementation class.
   *
   * @return the super constructor to invoke from the generated class.
   * @throws IllegalStateException if no target constructor for rpc found in target class.
   */
  private @NonNull Tuple2<ClassDesc, MethodTypeDesc> resolveBaseConstructorInformation() {
    var targetClass = this.targetClassMeta.targetClass();
    if (targetClass.isInterface()) {
      // is an interface - no constructor args needed
      return new Tuple2<>(ConstantDescs.CD_Object, RPCGenerationConstants.MTD_NO_ARGS_CONSTRUCTOR);
    }

    // try to find a constructor that is explicitly annotated with @RPCInvocationTarget
    var superClassDesc = ClassDesc.of(targetClass.getName());
    var constructors = targetClass.getDeclaredConstructors();
    for (var constructor : constructors) {
      if (constructor.isAnnotationPresent(RPCInvocationTarget.class)) {
        var constructorParamTypes = constructor.getParameterTypes();
        var paramTypeDescriptors = Arrays.stream(constructorParamTypes)
          .map(paramType -> ClassDesc.ofDescriptor(paramType.descriptorString()))
          .toList();
        var constructorMethodDescriptor = MethodTypeDesc.of(ConstantDescs.CD_void, paramTypeDescriptors);
        return new Tuple2<>(superClassDesc, constructorMethodDescriptor);
      }
    }

    // try to find a no-args constructor in the class
    for (var constructor : constructors) {
      if (constructor.getParameterCount() == 0) {
        return new Tuple2<>(superClassDesc, RPCGenerationConstants.MTD_NO_ARGS_CONSTRUCTOR);
      }
    }

    throw new IllegalStateException(String.format("no target constructor for rpc found in %s", targetClass.getName()));
  }

  /**
   * Generates a local utility method that can be used to invoke a method in this target class. The method automatically
   * joins the base rpc with the newly constructed rpc, if any is provided.
   *
   * @param classBuilder the class builder into which the method should be generated.
   * @throws NullPointerException if the given class builder is null.
   */
  private void generateBridgeRPCInvokeMethod(@NonNull ClassBuilder classBuilder) {
    classBuilder.withMethodBody(
      RPCGenerationConstants.MN_BRIDGE_INVOKE,
      RPCGenerationConstants.MTD_BRIDGE_RPC_INVOKE,
      ClassFile.ACC_PRIVATE,
      code -> {
        var baseRPCStoreSlot = code.allocateLocal(TypeKind.ReferenceType);
        code
          // construct the base RPC
          .aload(0)
          .getfield(this.generatingClass, RPCGenerationConstants.FN_RPC_SENDER, RPCGenerationConstants.CD_RPC_SENDER)
          .aload(1)
          .aload(2)
          .aload(3)
          .invokeinterface(RPCGenerationConstants.CD_RPC_SENDER, "invokeMethod", RPCGenerationConstants.MTD_RPC_INVOKE)
          .astore(baseRPCStoreSlot)
          // return the constructed RPC if no base RPC is available or join the constructed RPC with the base RPC
          .aload(0)
          .getfield(this.generatingClass, RPCGenerationConstants.FN_BASE_RPC, RPCGenerationConstants.CD_CHAINABLE_RPC)
          .ifThenElse(
            Opcode.IFNULL,
            ifNullCode -> ifNullCode.aload(baseRPCStoreSlot).areturn(),
            ifNonNullCode -> ifNonNullCode
              .aload(0)
              .getfield(
                this.generatingClass,
                RPCGenerationConstants.FN_BASE_RPC,
                RPCGenerationConstants.CD_CHAINABLE_RPC)
              .aload(baseRPCStoreSlot)
              .invokeinterface(RPCGenerationConstants.CD_CHAINABLE_RPC, "join", RPCGenerationConstants.MTD_RPC_JOIN)
              .areturn());
      });
  }

  /**
   * Generates a utility method to get the channel into which all rpc requests should be sent, which internally just
   * calls the channel supplier.
   *
   * @param classBuilder the class builder into which the method should be generated.
   * @throws NullPointerException if the given class builder is null.
   */
  private void generateBridgeChannelGetterMethod(@NonNull ClassBuilder classBuilder) {
    classBuilder.withMethodBody(
      RPCGenerationConstants.MN_BRIDGE_GET_CHANNEL,
      RPCGenerationConstants.MTD_BRIDGE_GET_CHANNEL,
      ClassFile.ACC_PRIVATE,
      code -> code
        .aload(0)
        .getfield(this.generatingClass, RPCGenerationConstants.FN_CHANNEL_SUPPLIER, RPCGenerationConstants.CD_SUPPLIER)
        .invokeinterface(RPCGenerationConstants.CD_SUPPLIER, "get", RPCGenerationConstants.MTD_SUPPLIER_GET)
        .checkcast(RPCGenerationConstants.CD_NETWORK_CHANNEL)
        .areturn());
  }

  /**
   * Generates the constructor and the constructor code into the given target class builder.
   *
   * @param classBuilder         the builder of the class to generate the constructor in.
   * @param superClass           the super class in which the target constructor is located.
   * @param superConstructorDesc the descriptor of the constructor in the super class.
   * @throws NullPointerException if the given class builder, super class or super constructor descriptor is null.
   */
  private void generateConstructor(
    @NonNull ClassBuilder classBuilder,
    @NonNull ClassDesc superClass,
    @NonNull MethodTypeDesc superConstructorDesc
  ) {
    // get the param types & assign the static types directly
    var instanceFactoryCount = this.context.additionalInstanceFactoryCount();
    var instanceFactoryParamTypes = new ClassDesc[instanceFactoryCount];
    Arrays.fill(instanceFactoryParamTypes, RPCGenerationConstants.CD_INT_INSTANCE_FACTORY);

    // generate the constructor code
    var baseConstructorMethodType = RPCInternalInstanceFactory.MTD_BASIC_IMPLEMENTATION_CONSTRUCTOR;
    var constructorMethodType = baseConstructorMethodType.insertParameterTypes(
      baseConstructorMethodType.parameterCount(),
      instanceFactoryParamTypes);
    classBuilder.withMethodBody(
      ConstantDescs.INIT_NAME,
      constructorMethodType,
      ClassFile.ACC_PUBLIC,
      code -> {
        code
          // assign channel supplier field
          .aload(0)
          .aload(1)
          .putfield(
            this.generatingClass,
            RPCGenerationConstants.FN_CHANNEL_SUPPLIER,
            RPCGenerationConstants.CD_SUPPLIER)
          // assign rpc sender field
          .aload(0)
          .aload(2)
          .putfield(
            this.generatingClass,
            RPCGenerationConstants.FN_RPC_SENDER,
            RPCGenerationConstants.CD_RPC_SENDER)
          // assign base rpc field
          .aload(0)
          .aload(3)
          .putfield(
            this.generatingClass,
            RPCGenerationConstants.FN_BASE_RPC,
            RPCGenerationConstants.CD_CHAINABLE_RPC);

        // apply the stuff registered in the context
        this.context.applyToClassAndConstructor(this.generatingClass, classBuilder, code);

        // call the super constructor
        code.aload(0);
        var superConstructorParamCount = superConstructorDesc.parameterCount();
        for (var index = 0; index < superConstructorParamCount; index++) {
          // load the index from the object arg array
          code.aload(4).ldc(index).aaload();

          var superConstructorParamType = superConstructorDesc.parameterType(index);
          if (superConstructorParamType.isPrimitive()) {
            // unbox the constructor type
            CodeGenerationUtil.unboxPrimitive(code, superConstructorParamType.descriptorString());
          } else {
            // cast to the parameter type
            code.checkcast(superConstructorParamType);
          }
        }

        code
          .invokespecial(superClass, ConstantDescs.INIT_NAME, superConstructorDesc)
          .return_();
      });
  }
}
