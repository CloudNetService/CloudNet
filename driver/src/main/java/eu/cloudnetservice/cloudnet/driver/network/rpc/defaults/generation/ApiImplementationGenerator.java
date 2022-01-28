/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.generation;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import eu.cloudnetservice.cloudnet.common.StringUtil;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPC;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCExecutable;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCIgnore;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCNoResult;
import eu.cloudnetservice.cloudnet.driver.network.rpc.exception.ClassCreationException;
import eu.cloudnetservice.cloudnet.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.cloudnet.driver.util.asm.AsmHelper;
import eu.cloudnetservice.cloudnet.driver.util.define.ClassDefiners;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class ApiImplementationGenerator {

  private static final String DEFAULT_SUPER = "java/lang/Object";
  // sender stuff
  private static final String SENDER_DESC = Type.getDescriptor(RPCSender.class);
  private static final String SENDER_TYPE = Type.getInternalName(RPCSender.class);
  private static final String INVOKE_METHOD_DESC = Type.getMethodDescriptor(
    Type.getType(RPC.class),
    Type.getType(String.class),
    Type.getType(Object[].class));
  private static final String CONSTRUCTOR_SENDER_DESC = Type.getMethodDescriptor(
    Type.VOID_TYPE,
    Type.getType(RPCSender.class));
  // executable stuff
  private static final String EXECUTABLE_NAME = Type.getInternalName(RPCExecutable.class);
  private static final String EXECUTABLE_FIRE_FORGET = Type.getMethodDescriptor(Type.VOID_TYPE);
  private static final String EXECUTABLE_FIRE = Type.getMethodDescriptor(Type.getType(Task.class));
  private static final String EXECUTABLE_FIRE_SYNC = Type.getMethodDescriptor(Type.getType(Object.class));
  // information regarding the generated class
  private static final String GENERATED_CLASS_NAME_FORMAT = "%s$Impl_%s";

  private ApiImplementationGenerator() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings({"unchecked", "ConstantConditions"})
  public static @NonNull <T> T generateApiImplementation(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @NonNull RPCSender sender
  ) {
    try {
      var className = String.format(
        GENERATED_CLASS_NAME_FORMAT,
        Type.getInternalName(baseClass),
        StringUtil.generateRandomString(10));
      var superName = context.extendingClass() == null ? DEFAULT_SUPER : Type.getInternalName(context.extendingClass());
      // check if the class has a constructor with a rpc sender instance, use that prioritized
      boolean useSenderConstructor = false;
      if (context.extendingClass() != null) {
        try {
          context.extendingClass().getDeclaredConstructor(RPCSender.class);
          useSenderConstructor = true;
        } catch (NoSuchMethodException ignored) {
          // proceed as the clas has only a no-args constructor
        }
      }
      // a new impl writer based on the given contextual information
      var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      cw.visit(
        V1_8,
        ACC_PUBLIC | ACC_FINAL,
        className,
        null,
        superName,
        context.interfaces().stream().map(Type::getInternalName).toArray(String[]::new));
      // add the sender field
      cw
        .visitField(ACC_PRIVATE | ACC_FINAL, "sender", SENDER_DESC, null, null)
        .visitEnd();
      // generate the constructor
      MethodVisitor mv;
      {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + SENDER_DESC + ")V", null, null);
        mv.visitCode();
        // visit super
        mv.visitVarInsn(ALOAD, 0);
        if (useSenderConstructor) {
          mv.visitVarInsn(ALOAD, 1);
          mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", CONSTRUCTOR_SENDER_DESC, false);
        } else {
          mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
        }
        // write the sender field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, "sender", SENDER_DESC);
        // finish
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }

      // implement all methods
      {
        var methods = collectMethodsToVisit(context);
        for (var method : methods) {
          mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, method.getName(), Type.getMethodDescriptor(method), null, null);
          mv.visitCode();
          // get the sender field
          mv.visitVarInsn(ALOAD, 0);
          mv.visitFieldInsn(GETFIELD, className, "sender", SENDER_DESC);
          // the name of the remote method to execute (the current method)
          mv.visitLdcInsn(method.getName());
          // create a new object array (will later hold all argument of the invocation)
          var params = method.getParameterTypes();
          AsmHelper.pushInt(mv, params.length);
          mv.visitTypeInsn(ANEWARRAY, DEFAULT_SUPER);
          // add each parameter to the array
          for (int i = 0; i < params.length; i++) {
            var type = params[i];
            var intType = Type.getType(type);
            // dup the array on the stack and push the target index we want to add the object to
            mv.visitInsn(DUP);
            AsmHelper.pushInt(mv, i);
            // load the parameter
            mv.visitVarInsn(intType.getOpcode(ILOAD), i + 1);
            // we need to wrap a primitive type to the wrapper type
            if (type.isPrimitive()) {
              AsmHelper.primitiveToWrapper(mv, type);
            }
            // store the element
            mv.visitInsn(AASTORE);
          }
          // invoke the send method
          mv.visitMethodInsn(INVOKEINTERFACE, SENDER_TYPE, "invokeMethod", INVOKE_METHOD_DESC, true);
          // we cannot ignore the return type if not void
          var voidMethod = method.getReturnType().equals(void.class);
          if (!voidMethod || !method.isAnnotationPresent(RPCNoResult.class)) {
            // fire async if the result type is a completable future
            if (Task.class.isAssignableFrom(method.getReturnType())) {
              // fire the rpc async
              mv.visitMethodInsn(INVOKEINTERFACE, EXECUTABLE_NAME, "fire", EXECUTABLE_FIRE, true);
            } else {
              // fire the rpc sync
              mv.visitMethodInsn(INVOKEINTERFACE, EXECUTABLE_NAME, "fireSync", EXECUTABLE_FIRE_SYNC, true);
              // unwrap primitive types
              if (!voidMethod) {
                if (method.getReturnType().isPrimitive()) {
                  // convert the wrapper to the primitive value
                  AsmHelper.wrapperToPrimitive(mv, method.getReturnType());
                } else {
                  mv.visitTypeInsn(CHECKCAST, Type.getInternalName(method.getReturnType()));
                }
              }
            }
            // if no result was expected pop the result, else return it
            if (voidMethod) {
              mv.visitInsn(POP);
              mv.visitInsn(RETURN);
            } else {
              var rt = Type.getType(method.getReturnType());
              mv.visitInsn(rt.getOpcode(IRETURN));
            }
          } else {
            // just send
            mv.visitMethodInsn(INVOKEINTERFACE, EXECUTABLE_NAME, "fireAndForget", EXECUTABLE_FIRE_FORGET, true);
            mv.visitInsn(RETURN);
          }
          // finish the method
          mv.visitMaxs(0, 0);
          mv.visitEnd();
        }
      }
      // finish the class
      cw.visitEnd();
      // define & select the correct constructor for the class
      var constructor = ClassDefiners.current()
        .defineClass(className, baseClass, cw.toByteArray())
        .getDeclaredConstructor(RPCSender.class);
      constructor.setAccessible(true);
      // instantiate the new class
      return (T) constructor.newInstance(sender);
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate api class implementation for class %s",
        baseClass.getName()), exception);
    }
  }

  private static @NotNull Collection<Method> collectMethodsToVisit(@NotNull GenerationContext context) {
    Map<String, Method> visitedMethods = new HashMap<>();
    // first travel the class we should extend (if given)
    if (context.extendingClass() != null) {
      travelClassHierarchy(context.extendingClass(), method -> {
        var id = String.format("%s@%s", method.getName(), Type.getMethodDescriptor(method));
        visitedMethods.putIfAbsent(id, method);
      });
    }
    // travel all interfaces
    for (var inter : context.interfaces()) {
      travelClassHierarchy(inter, method -> {
        var id = String.format("%s@%s", method.getName(), Type.getMethodDescriptor(method));
        visitedMethods.putIfAbsent(id, method);
      });
    }
    // filter out all methods which are already implemented if needed
    if (context.implementAllMethods()) {
      return visitedMethods.values();
    } else {
      return visitedMethods.values().stream()
        .filter(method -> Modifier.isAbstract(method.getModifiers()))
        .toList();
    }
  }

  private static void travelClassHierarchy(@NonNull Class<?> start, @NonNull Consumer<Method> handler) {
    // travel down the class hierarchy first
    travelClassesUntilObject(start, handler);
    // visit all interfaces
    travelInterfaces(start, handler);
  }

  private static void travelClassesUntilObject(@NonNull Class<?> start, @NonNull Consumer<Method> handler) {
    Class<?> curr = start;
    do {
      // we only need public visible methods
      for (var method : curr.getDeclaredMethods()) {
        var mod = method.getModifiers();
        if (Modifier.isPublic(mod) && !Modifier.isStatic(mod) && !method.isAnnotationPresent(RPCIgnore.class)) {
          handler.accept(method);
        }
      }
      // next class
      curr = curr.getSuperclass();
    } while (curr != null && curr != Object.class);
  }

  private static void travelInterfaces(@NonNull Class<?> start, @NonNull Consumer<Method> handler) {
    for (var inter : start.getInterfaces()) {
      // we only need public visible methods
      for (var method : inter.getDeclaredMethods()) {
        var mod = method.getModifiers();
        if (Modifier.isPublic(mod) && !method.isAnnotationPresent(RPCIgnore.class)) {
          handler.accept(method);
        }
      }
    }
  }
}
