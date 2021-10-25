/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker;

import static de.dytanic.cloudnet.driver.util.asm.AsmUtils.primitiveToWrapper;
import static de.dytanic.cloudnet.driver.util.asm.AsmUtils.pushInt;
import static de.dytanic.cloudnet.driver.util.asm.AsmUtils.wrapperToPrimitive;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import com.google.common.reflect.TypeToken;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.driver.network.rpc.defaults.MethodInformation;
import de.dytanic.cloudnet.driver.network.rpc.exception.ClassCreationException;
import de.dytanic.cloudnet.driver.util.define.ClassDefiners;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class MethodInvokerGenerator {

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

  public @NotNull MethodInvoker makeMethodInvoker(@NotNull MethodInformation methodInfo) {
    try {
      String className = String.format(
        CLASS_NAME_FORMAT,
        Type.getInternalName(methodInfo.getDefiningClass()),
        methodInfo.getName(),
        StringUtil.generateRandomString(25));
      // init the class writer for a public final class implementing the MethodInvoker
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
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
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(methodInfo.getDefiningClass()));
        // visit each argument of the method
        Type[] arguments = new Type[methodInfo.getArguments().length];
        for (int i = 0; i < methodInfo.getArguments().length; i++) {
          Class<?> rawType = TypeToken.of(methodInfo.getArguments()[i]).getRawType();
          // load the argument supplied for the index
          mv.visitVarInsn(ALOAD, 1);
          pushInt(mv, i);
          mv.visitInsn(AALOAD);
          // check if the raw type is primitive
          if (rawType.isPrimitive()) {
            wrapperToPrimitive(mv, rawType);
          } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(rawType));
          }
          // store the argument type
          arguments[i] = Type.getType(rawType);
        }
        // invoke the method
        mv.visitMethodInsn(
          methodInfo.getDefiningClass().isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL,
          Type.getInternalName(methodInfo.getDefiningClass()),
          methodInfo.getName(),
          Type.getMethodDescriptor(Type.getType(methodInfo.getRawReturnType()), arguments),
          methodInfo.getDefiningClass().isInterface());
        // for a void return type return null, else return the method invocation result - respect primitives
        if (methodInfo.isVoidMethod()) {
          mv.visitInsn(ACONST_NULL);
        } else if (methodInfo.getRawReturnType().isPrimitive()) {
          primitiveToWrapper(mv, methodInfo.getRawReturnType());
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
      Constructor<?> constructor = ClassDefiners.current()
        .defineClass(className, methodInfo.getDefiningClass(), cw.toByteArray())
        .getDeclaredConstructor(Object.class);
      constructor.setAccessible(true);
      // instantiate
      return (MethodInvoker) constructor.newInstance(methodInfo.getSourceInstance());
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Cannot generate rpc handler for method %s defined in class %s",
        methodInfo.getName(),
        methodInfo.getDefiningClass().getCanonicalName()
      ), exception);
    }
  }

  public @NotNull MethodInvoker makeNoArgsConstructorInvoker(@NotNull Class<?> clazz) {
    try {
      // make a class name which is definitely unique for the class
      String className = String.format(
        NO_ARGS_CONSTRUCTOR_CLASS_NAME_FORMAT,
        Type.getInternalName(clazz),
        StringUtil.generateRandomString(25));

      // init the class writer for a public final class implementing the MethodInvoker
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
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
      Constructor<?> constructor = ClassDefiners.current()
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
}
