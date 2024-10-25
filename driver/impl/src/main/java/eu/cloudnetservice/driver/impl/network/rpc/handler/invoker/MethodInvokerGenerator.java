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

package eu.cloudnetservice.driver.impl.network.rpc.handler.invoker;

import eu.cloudnetservice.driver.impl.network.rpc.introspec.DefaultRPCMethodMetadata;
import eu.cloudnetservice.utils.base.CodeGenerationUtil;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import lombok.NonNull;

/**
 * A utility class to generate and define an invoker for a method in the runtime.
 *
 * @since 4.0
 */
public final class MethodInvokerGenerator {

  // constants for use with method invoker
  private static final String MI_CALL_METHOD_NAME = "callMethod";
  private static final ClassDesc CD_METHOD_INVOKER = ClassDesc.of(MethodInvoker.class.getName());

  // method descriptor for MethodInvoker.callMethod(Object, Object[]): Object
  private static final MethodTypeDesc MTD_MI_CALL_METHOD = MethodTypeDesc.of(
    /* returns       */ ConstantDescs.CD_Object,
    /* target param  */ ConstantDescs.CD_Object,
    /* params param  */ ConstantDescs.CD_Object.arrayType());

  // method type for the generated no-args constructor in a MethodInvoker impl
  private static final MethodType MI_CONSTRUCTOR_TYPE = MethodType.methodType(void.class);

  public static @NonNull MethodInvoker makeMethodInvoker(@NonNull DefaultRPCMethodMetadata targetMethod) {
    // generate the name of the class, format: "<original class name>$RPCInvoker$<method name>"
    var ownerClassDesc = ClassDesc.ofDescriptor(targetMethod.definingClass().descriptorString());
    var classDesc = ownerClassDesc.nested("RPCInvoker", targetMethod.name());

    var classFileBytes = ClassFile.of().build(classDesc, classBuilder -> {
      // implements the method invoker interface
      classBuilder.withInterfaceSymbols(CD_METHOD_INVOKER);

      // generate no-args super constructor call
      classBuilder.withMethodBody(
        ConstantDescs.INIT_NAME,
        ConstantDescs.MTD_void,
        ClassFile.ACC_PUBLIC,
        code -> code
          .aload(0)
          .invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void)
          .return_());

      // implement the callMethod method
      classBuilder.withMethodBody(MI_CALL_METHOD_NAME, MTD_MI_CALL_METHOD, ClassFile.ACC_PUBLIC, code -> {
        // load target parameter & cast it to the declaring class
        code.aload(1).checkcast(ownerClassDesc);

        // load all parameters, as the array length is known ahead-of-time we can just unroll this into one long list
        // of instructions rather than using a loop in the generated code
        var targetMethodType = targetMethod.methodType();
        var targetMethodParameterCount = targetMethodType.parameterCount();
        for (var index = 0; index < targetMethodParameterCount; index++) {
          // load the array, push the index in the array we want to access, load the actual element at the array index
          code.aload(2).ldc(index).aaload();

          var parameterType = targetMethodType.parameterType(index);
          if (parameterType.isPrimitive()) {
            // unbox the primitive type if the target parameter type is primitive
            CodeGenerationUtil.unboxPrimitive(code, parameterType.descriptorString());
          } else {
            // just insert a cast to put in the right type
            var targetTypeDesc = ClassDesc.ofDescriptor(parameterType.descriptorString());
            code.checkcast(targetTypeDesc);
          }
        }

        var targetMethodTypeDesc = MethodTypeDesc.ofDescriptor(targetMethod.methodType().descriptorString());
        if (targetMethod.definingClass().isInterface()) {
          // target method is defined in interface
          code.invokeinterface(ownerClassDesc, targetMethod.name(), targetMethodTypeDesc);
        } else {
          // target method is defined in concrete class
          code.invokevirtual(ownerClassDesc, targetMethod.name(), targetMethodTypeDesc);
        }

        var returnDescriptor = targetMethodTypeDesc.returnType().descriptorString();
        switch (returnDescriptor) {
          // void method should return null from the method invocation
          case "V" -> code.aconst_null();
          // primitive types need to be put into their wrapper type before returning
          case "Z", "B", "S", "C", "I", "J", "F", "D" -> CodeGenerationUtil.boxPrimitive(code, returnDescriptor);
        }

        // return the wrapped object return value
        code.areturn();
      });
    });

    try {
      // define the class as a nest mate in the class defining the method
      var classLookup = CodeGenerationUtil.defineNestedClass(targetMethod.definingClass(), classFileBytes);
      var noArgsConstructor = classLookup.findConstructor(classLookup.lookupClass(), MI_CONSTRUCTOR_TYPE);
      return (MethodInvoker) noArgsConstructor.invoke();
    } catch (Throwable throwable) {
      throw new AssertionError("unable to define or constructor method accessor", throwable);
    }
  }
}
