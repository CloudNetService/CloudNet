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

package eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker;

import eu.cloudnetservice.driver.network.rpc.introspec.RPCMethodMetadata;
import eu.cloudnetservice.driver.util.CodeGenerationUtil;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A utility class to generate and define an invoker for a method in the runtime.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class MethodInvokerGenerator {

  // method name of MethodInvoker.callMethod
  private static final String MI_CALL_METHOD_NAME = "callMethod";

  // method descriptor for MethodInvoker.callMethod(Object, Object[]): Object
  private static final MethodTypeDesc MTD_MI_CALL_METHOD = MethodTypeDesc.of(
    /* returns       */ ConstantDescs.CD_Object,
    /* target param  */ ConstantDescs.CD_Object,
    /* params param  */ ConstantDescs.CD_Object.arrayType());

  // method type for the generated no-args constructor in a MethodInvoker impl
  private static final MethodType MI_CONSTRUCTOR_TYPE = MethodType.methodType(void.class);

  public static @NonNull MethodInvoker makeMethodInvoker(@NonNull RPCMethodMetadata targetMethod) {
    // generate the name of the class, format: "<original class name>$RPCInvoker$<method name>"
    var ownerClassDesc = ClassDesc.ofDescriptor(targetMethod.definingClass().descriptorString());
    var classDesc = ownerClassDesc.nested("RPCInvoker", targetMethod.name());

    var classFileBytes = ClassFile.of().build(classDesc, classBuilder -> {
      // generate no-args super constructor call
      classBuilder.withMethodBody(
        ConstantDescs.INIT_NAME,
        ConstantDescs.MTD_void,
        ClassFile.ACC_PUBLIC,
        code -> code.aload(0).invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void));

      // implement the callMethod method
      classBuilder.withMethodBody(MI_CALL_METHOD_NAME, MTD_MI_CALL_METHOD, ClassFile.ACC_PUBLIC, code -> {
        // load target parameter
        code.aload(1);

        // load all parameters, as the array length is known ahead-of-time we can just unroll this into one long list
        // of instructions rather than using a loop in the generated code
        var parameterTypes = targetMethod.methodType().parameterArray();
        for (var index = 0; index < parameterTypes.length; index++) {
          // load the array, push the index in the array we want to access, load the actual element at the array index
          code.aload(2).ldc(index).aaload();

          var parameterType = parameterTypes[index];
          if (parameterType.isPrimitive()) {
            // unbox the primitive type if the target parameter type is primitive
            CodeGenerationUtil.unboxPrimitive(code, parameterType.descriptorString());
          } else {
            // just insert a cast to put in the right type
            var targetTypeDesc = ClassDesc.ofDescriptor(parameterType.descriptorString());
            code.checkcast(targetTypeDesc);
          }
        }

        var targetMethodType = MethodTypeDesc.ofDescriptor(targetMethod.methodType().descriptorString());
        if (targetMethod.definingClass().isInterface()) {
          // target method is defined in interface
          code.invokeinterface(ownerClassDesc, targetMethod.name(), targetMethodType);
        } else {
          // target method is defined in concrete class
          code.invokevirtual(ownerClassDesc, targetMethod.name(), targetMethodType);
        }

        var returnDescriptor = targetMethodType.returnType().descriptorString();
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
      return (MethodInvoker) noArgsConstructor.invokeExact();
    } catch (Throwable throwable) {
      throw new AssertionError("unable to define or constructor method accessor", throwable);
    }
  }



/*

  private static final String SUPER = "java/lang/Object";
  private static final String OBJ_DESCRIPTOR = Type.getDescriptor(Object.class);
  // MethodInvoker related stuff
  private static final String[] METHOD_INVOKER = new String[]{Type.getInternalName(MethodInvoker.class)};
  private static final String CALL_METHOD_DESCRIPTOR = Type.getMethodDescriptor(
    Type.getType(Object.class),
    Type.getType(Object[].class));
  // Constructor stuff
  private static final String CONSTRUCTOR_DESCRIPTOR = Type.getMethodDescriptor(
    Type.VOID_TYPE,
    Type.getType(Object.class));
  // generated classes related stuff
  private static final String CLASS_NAME_FORMAT = "%s$GeneratedInvoker_%s_%s";
  private static final String NO_ARGS_CONSTRUCTOR_CLASS_NAME_FORMAT = "%s$GeneratedConstructorInvoker_%s";

  public @NonNull MethodInvoker makeMethodInvoker(@NonNull MethodInformation methodInfo) {
    try {
      var className = String.format(
        CLASS_NAME_FORMAT,
        Type.getInternalName(methodInfo.definingClass()),
        methodInfo.name(),
        StringUtil.generateRandomString(25));
      // init the class writer for a public final class implementing the MethodInvoker
      var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, SUPER, METHOD_INVOKER);
      // visit the instance field
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "instance", OBJ_DESCRIPTOR, null, null).visitEnd();
      // generate a no-args constructor
      MethodVisitor mv;
      {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", CONSTRUCTOR_DESCRIPTOR, null, null);
        mv.visitCode();
        // call super()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER, "<init>", "()V", false);
        // assign the instance field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, "instance", OBJ_DESCRIPTOR);
        // finish the constructor
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      {
        mv = cw.visitMethod(ACC_PUBLIC, "callMethod", CALL_METHOD_DESCRIPTOR, null, null);
        mv.visitCode();
        // get the instance field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "instance", OBJ_DESCRIPTOR);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(methodInfo.definingClass()));
        // visit each argument of the method
        var arguments = new Type[methodInfo.arguments().length];
        for (var i = 0; i < methodInfo.arguments().length; i++) {
          var rawType = GenericTypeReflector.erase(methodInfo.arguments()[i]);
          // load the argument supplied for the index
          mv.visitVarInsn(ALOAD, 1);
          AsmHelper.pushInt(mv, i);
          mv.visitInsn(AALOAD);
          // check if the raw type is primitive
          if (rawType.isPrimitive()) {
            AsmHelper.wrapperToPrimitive(mv, rawType);
          } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(rawType));
          }
          // store the argument type
          arguments[i] = Type.getType(rawType);
        }
        // invoke the method
        mv.visitMethodInsn(
          methodInfo.definingClass().isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL,
          Type.getInternalName(methodInfo.definingClass()),
          methodInfo.name(),
          Type.getMethodDescriptor(Type.getType(methodInfo.rawReturnType()), arguments),
          methodInfo.definingClass().isInterface());
        // for a void return type return null, else return the method invocation result - respect primitives
        if (methodInfo.voidMethod()) {
          mv.visitInsn(ACONST_NULL);
        } else if (methodInfo.rawReturnType().isPrimitive()) {
          AsmHelper.primitiveToWrapper(mv, methodInfo.rawReturnType());
        }
        // return the value of the stack
        mv.visitInsn(ARETURN);
        // finish the method
        mv.visitEnd();
        mv.visitMaxs(0, 0);
      }
      // finish the class
      cw.visitEnd();
      // define and make the constructor accessible
      var constructor = ClassDefiners.current()
        .defineClass(className, methodInfo.definingClass(), cw.toByteArray())
        .getDeclaredConstructor(Object.class);
      constructor.setAccessible(true);
      // instantiate
      return (MethodInvoker) constructor.newInstance(methodInfo.sourceInstance());
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Cannot generate rpc handler for method %s defined in class %s",
        methodInfo.name(),
        methodInfo.definingClass().getCanonicalName()
      ), exception);
    }
  }

  public @NonNull MethodInvoker makeNoArgsConstructorInvoker(@NonNull Class<?> clazz) {
    try {
      // make a class name which is definitely unique for the class
      var className = String.format(
        NO_ARGS_CONSTRUCTOR_CLASS_NAME_FORMAT,
        Type.getInternalName(clazz),
        StringUtil.generateRandomString(25));

      // init the class writer for a public final class implementing the MethodInvoker
      var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, SUPER, METHOD_INVOKER);
      // generate a constructor and the invoke method
      MethodVisitor mv;
      {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      {
        mv = cw.visitMethod(ACC_PUBLIC, "callMethod", CALL_METHOD_DESCRIPTOR, null, null);
        mv.visitCode();
        // create the instance
        mv.visitTypeInsn(NEW, Type.getInternalName(clazz));
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(clazz), "<init>", "()V", false);
        mv.visitInsn(ARETURN);
        // finish the method
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      // finish the class
      cw.visitEnd();

      // declare the class and get the constructor
      var constructor = ClassDefiners.current()
        .defineClass(className, clazz, cw.toByteArray())
        .getDeclaredConstructor();
      constructor.setAccessible(true);
      // create a new instance of the class
      return (MethodInvoker) constructor.newInstance();
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Cannot generate rpc no args constructor for class %s",
        clazz.getCanonicalName()
      ), exception);
    }
  }

 */
}
