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

package eu.cloudnetservice.driver.network.rpc.defaults.generation;

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

import com.google.common.collect.ObjectArrays;
import dev.derklaro.reflexion.Reflexion;
import dev.derklaro.reflexion.matcher.ConstructorMatcher;
import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCExecutable;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCIgnore;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCNoResult;
import eu.cloudnetservice.driver.network.rpc.exception.ClassCreationException;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.network.rpc.generation.InstanceFactory;
import eu.cloudnetservice.driver.util.asm.AsmHelper;
import eu.cloudnetservice.driver.util.define.ClassDefiners;
import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * The generator for api implementations based on rpc.
 *
 * @see RPCFactory#generateRPCBasedApi(Class, GenerationContext)
 * @since 4.0
 */
public final class ApiImplementationGenerator {

  static final String DEFAULT_SUPER = "java/lang/Object";
  // network channel supplier
  static final Type CHANNEL_TYPE = Type.getType(NetworkChannel.class);
  static final String NET_CHANNEL_TYPE = CHANNEL_TYPE.getInternalName();
  static final String SUPPLIER_DESC = Type.getDescriptor(Supplier.class);
  static final String SUPPLIER_TYPE = Type.getInternalName(Supplier.class);
  static final String GET_METHOD_DESC = Type.getMethodDescriptor(Type.getType(Object.class));
  // sender stuff
  static final String SENDER_DESC = Type.getDescriptor(RPCSender.class);
  static final String SENDER_TYPE = Type.getInternalName(RPCSender.class);
  static final String INVOKE_METHOD_DESC = Type.getMethodDescriptor(
    Type.getType(RPC.class),
    Type.getType(String.class),
    Type.getType(Object[].class));
  static final String INJECT_ANNOTATION_DESC = Type.getDescriptor(Inject.class);
  // executable stuff
  static final String EXECUTABLE_NAME = Type.getInternalName(RPCExecutable.class);
  static final String EXECUTABLE_FIRE_FORGET = Type.getMethodDescriptor(Type.VOID_TYPE);
  static final String EXECUTABLE_FIRE = Type.getMethodDescriptor(Type.getType(Task.class));
  static final String EXECUTABLE_FIRE_SYNC = Type.getMethodDescriptor(Type.getType(Object.class));
  // executable with channel
  static final String EXECUTABLE_FIRE_FORGET_CHANNEL = Type.getMethodDescriptor(Type.VOID_TYPE, CHANNEL_TYPE);
  static final String EXECUTABLE_FIRE_CHANNEL = Type.getMethodDescriptor(Type.getType(Task.class), CHANNEL_TYPE);
  static final String EXECUTABLE_FIRE_SYNC_CHANNEL = Type.getMethodDescriptor(Type.getType(Object.class), CHANNEL_TYPE);
  // information regarding the generated class
  static final String GENERATED_CLASS_NAME_FORMAT = "%s$Impl_%s";
  // the main checker function if a method should be overridden or not, only applying the base checks
  // this checks if the applied method is public, not static, not a bridge, not final and isn't annotated with @RPCIgnore
  static final Predicate<Method> SHOULD_GENERATE_IMPL = method -> {
    var mod = method.getModifiers();
    return Modifier.isPublic(mod)
      && !Modifier.isStatic(mod)
      && !Modifier.isFinal(mod)
      && !method.isBridge()
      && !method.isAnnotationPresent(RPCIgnore.class);
  };

  private ApiImplementationGenerator() {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates an implementation of a class sending rpc requests based on the given generation context.
   *
   * @param baseClass the return type model class.
   * @param context   the generation context.
   * @param sender    the rpc sender to use for the class.
   * @param <T>       the type which gets generated.
   * @return a supplier to create an instance of the class which has all requested methods rpc based implemented.
   * @throws NullPointerException   if the given base class, context or rpc sender is null.
   * @throws ClassCreationException if the generator is unable to generate an implementation of the class.
   */
  // Suppresses ConstantConditions as IJ is dumb
  @SuppressWarnings({"unchecked"})
  public static @NonNull <T> InstanceFactory<T> generateApiImplementation(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @NonNull RPCSender sender
  ) {
    try {
      var parentClass = Objects.requireNonNullElse(context.extendingClass(), baseClass);
      var className = String.format(
        GENERATED_CLASS_NAME_FORMAT,
        Type.getInternalName(parentClass),
        StringUtil.generateRandomString(10));
      var superName = context.extendingClass() == null ? DEFAULT_SUPER : Type.getInternalName(context.extendingClass());

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
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "sender", SENDER_DESC, null, null).visitEnd();
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "channelSupplier", SUPPLIER_DESC, null, null).visitEnd();

      // check if the class has a constructor with a rpc sender instance, use that prioritized
      var targetConstructorArgs = ChainedApiImplementationGenerator.findSuperConstructorTypes(context.extendingClass());

      // generate the constructor
      MethodVisitor mv;
      mv = cw.visitMethod(
        ACC_PUBLIC,
        "<init>",
        String.format(
          "(%s%s%s)V",
          SENDER_DESC,
          SUPPLIER_DESC,
          targetConstructorArgs.stream()
            .map(Type::getDescriptor)
            .filter(type -> !type.equals(SENDER_DESC) && !type.equals(SUPPLIER_DESC))
            .collect(Collectors.joining())),
        null,
        null);
      mv.visitAnnotation(INJECT_ANNOTATION_DESC, true);
      mv.visitCode();

      // visit super
      mv.visitVarInsn(ALOAD, 0);
      if (targetConstructorArgs.isEmpty()) {
        mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
      } else {
        // load all parameters to the stack
        for (int i = 0; i < targetConstructorArgs.size(); i++) {
          var argumentType = targetConstructorArgs.get(i);

          if (i == 0 && argumentType.equals(RPCSender.class)) {
            // argument is the provided sender
            mv.visitVarInsn(ALOAD, 1);
          } else if ((i == 0 || i == 1) && argumentType.equals(Supplier.class)) {
            // argument is the provided channel supplier
            mv.visitVarInsn(ALOAD, 2);
          } else {
            var type = Type.getType(argumentType);
            mv.visitVarInsn(type.getOpcode(ILOAD), 2 + i); // 0 = this, 1 = sender, 2 = channel supplier
          }
        }

        mv.visitMethodInsn(
          INVOKESPECIAL,
          superName,
          "<init>",
          targetConstructorArgs.stream()
            .map(Type::getDescriptor)
            .collect(Collectors.joining("", "(", ")V")),
          false);
      }

      // write the sender field
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitFieldInsn(PUTFIELD, className, "sender", SENDER_DESC);
      // write the channelSupplier field
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitFieldInsn(PUTFIELD, className, "channelSupplier", SUPPLIER_DESC);
      // finish
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();

      // implement all methods
      {
        var methods = collectMethodsToVisit(context);
        for (var method : methods) {
          mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, method.getName(), Type.getMethodDescriptor(method), null, null);
          mv.visitCode();
          // generate the invoke method
          visitInvokeMethod(className, method, mv);
          // we cannot ignore the return type if not void
          visitFireMethod(method, mv, className, context);
          // finish the method
          mv.visitMaxs(0, 0);
          mv.visitEnd();
        }
      }
      // finish the class
      cw.visitEnd();

      // define & select the correct constructor for the class
      var definedClass = ClassDefiners.current().defineClass(className, parentClass, cw.toByteArray());
      var constructorMatcher = ConstructorMatcher.newMatcher()
        .exactType(Constructor::getDeclaringClass, definedClass)
        .exactTypeAt(Constructor::getParameterTypes, RPCSender.class, 0)
        .exactTypeAt(Constructor::getParameterTypes, Supplier.class, 1);

      // instantiate the new class
      var accessor = Reflexion.on(definedClass).findConstructor(constructorMatcher).orElseThrow();
      return constructorArgs -> {
        var arguments = ObjectArrays.concat(
          new Object[]{sender, context.channelSupplier()},
          constructorArgs,
          Object.class);
        return (T) accessor.invokeWithArgs(arguments).getOrThrow();
      };
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate api class implementation for class %s",
        baseClass.getName()), exception);
    }
  }

  /**
   * Generates the {@link RPCSender#invokeMethod(String)} method to gather the rpc result.
   *
   * @param className the name of the class owning the method.
   * @param method    the method to generate the invoke method in.
   * @param mv        the method visitor for the method.
   * @throws NullPointerException if the given class name, method or method visitor is null.
   */
  static void visitInvokeMethod(@NonNull String className, @NonNull Method method, @NonNull MethodVisitor mv) {
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
    for (var i = 0; i < params.length; i++) {
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
  }

  /**
   * Decides which {@link RPC#fire()} method is appropriate to call for the return type of this method and generates the
   * call on the {@link RPC} that was generated previously.
   *
   * @param method    the method to invoke using the rpc.
   * @param mv        the method visitor for the current method.
   * @param className the name of the class owning the method.
   * @param context   the generation context for this generation step.
   * @throws NullPointerException if the given method, method visitor, class name or context is null.
   */
  @SuppressWarnings("ConstantConditions")
  static void visitFireMethod(
    @NonNull Method method,
    @NonNull MethodVisitor mv,
    @NonNull String className,
    @NonNull GenerationContext context
  ) {
    var hasChannelSupplier = context.channelSupplier() != null;
    // load the network channel we want to send the request to onto the stack if needed
    if (hasChannelSupplier) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, className, "channelSupplier", SUPPLIER_DESC);
      mv.visitMethodInsn(INVOKEINTERFACE, SUPPLIER_TYPE, "get", GET_METHOD_DESC, true);
      mv.visitTypeInsn(CHECKCAST, NET_CHANNEL_TYPE);
    }

    // visit the actual fire method
    var voidMethod = method.getReturnType().equals(void.class);
    if (!voidMethod || !method.isAnnotationPresent(RPCNoResult.class)) {
      // fire async if the result type is a completable future
      if (Task.class.isAssignableFrom(method.getReturnType())) {
        // fire the rpc async
        mv.visitMethodInsn(
          INVOKEINTERFACE,
          EXECUTABLE_NAME,
          "fire",
          hasChannelSupplier ? EXECUTABLE_FIRE_CHANNEL : EXECUTABLE_FIRE,
          true);
      } else {
        // fire the rpc sync
        mv.visitMethodInsn(
          INVOKEINTERFACE,
          EXECUTABLE_NAME,
          "fireSync",
          hasChannelSupplier ? EXECUTABLE_FIRE_SYNC_CHANNEL : EXECUTABLE_FIRE_SYNC,
          true);
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
      mv.visitMethodInsn(
        INVOKEINTERFACE,
        EXECUTABLE_NAME,
        "fireAndForget",
        hasChannelSupplier ? EXECUTABLE_FIRE_FORGET_CHANNEL : EXECUTABLE_FIRE_FORGET,
        true);
      mv.visitInsn(RETURN);
    }
  }

  /**
   * Collects all methods based on the given context which should get implemented.
   *
   * @param context the context.
   * @return all methods which need to get implemented.
   * @throws NullPointerException if the given context is null.
   */
  static @NonNull Collection<Method> collectMethodsToVisit(@NonNull GenerationContext context) {
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

  /**
   * Travels down the class hierarchy of the given class, first visiting all superclasses (except Object) and then all
   * interfaces, posting all public, non-static methods to the given handler.
   *
   * @param start   the class to start the travel from.
   * @param handler the handler to accept all methods.
   * @throws NullPointerException if the given start class or handler is null.
   */
  private static void travelClassHierarchy(@NonNull Class<?> start, @NonNull Consumer<Method> handler) {
    var curr = start;
    do {
      // we only need public visible methods
      for (var method : curr.getDeclaredMethods()) {
        if (SHOULD_GENERATE_IMPL.test(method)) {
          handler.accept(method);
        }
      }
      // travel all interfaces of the current class
      travelInterfaces(curr, handler);
      // next class
      curr = curr.getSuperclass();
    } while (curr != null && curr != Object.class);
  }

  /**
   * Travels over all interfaces of the given class.
   *
   * @param start   the class to start the travel from.
   * @param handler the handler to accept all methods.
   * @throws NullPointerException if the given start class or handler is null.
   */
  private static void travelInterfaces(@NonNull Class<?> start, @NonNull Consumer<Method> handler) {
    for (var inter : start.getInterfaces()) {
      // we only need public visible methods
      for (var method : inter.getDeclaredMethods()) {
        if (SHOULD_GENERATE_IMPL.test(method)) {
          handler.accept(method);
        }
      }
    }
  }
}
