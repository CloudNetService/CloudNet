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

import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.DEFAULT_SUPER;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.GENERATED_CLASS_NAME_FORMAT;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.SENDER_DESC;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.SUPPLIER_DESC;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.collectMethodsToVisit;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.visitFireMethod;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.visitInvokeMethod;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import com.google.common.collect.ObjectArrays;
import dev.derklaro.reflexion.Reflexion;
import dev.derklaro.reflexion.matcher.ConstructorMatcher;
import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.driver.network.rpc.ChainableRPC;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.exception.ClassCreationException;
import eu.cloudnetservice.driver.network.rpc.generation.ChainInstanceFactory;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.util.asm.StackIndexHelper;
import eu.cloudnetservice.driver.util.define.ClassDefiners;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class ChainedApiImplementationGenerator {

  private static final Type RPC_TYPE = Type.getType(RPC.class);
  private static final String RPC_DESC = RPC_TYPE.getDescriptor();

  private static final Type RPC_CHAIN_TYPE = Type.getType(RPCChain.class);
  private static final String CHAINABLE_RPC_NAME = Type.getInternalName(ChainableRPC.class);
  private static final String RPC_JOIN_METHOD_DESC = Type.getMethodDescriptor(RPC_CHAIN_TYPE, RPC_TYPE);

  private ChainedApiImplementationGenerator() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  public static @NonNull <T> ChainInstanceFactory<T> generateApiImplementation(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @NonNull RPCSender classSender,
    @NonNull Function<Object[], RPC> rpcFactory
  ) {
    try {
      var parentClass = Objects.requireNonNullElse(context.extendingClass(), baseClass);
      var className = String.format(
        GENERATED_CLASS_NAME_FORMAT,
        Type.getInternalName(parentClass),
        StringUtil.generateRandomString(10));
      var superName = context.extendingClass() == null ? DEFAULT_SUPER : Type.getInternalName(context.extendingClass());

      // the super constructor types we need when invoking
      var types = findSuperConstructorTypes(context.extendingClass());
      var superDesc = types.stream().map(Type::getDescriptor).collect(Collectors.joining());

      // a new impl writer based on the given contextual information
      var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      cw.visit(
        V1_8,
        ACC_PUBLIC | ACC_FINAL,
        className,
        null,
        superName,
        context.interfaces().stream().map(Type::getInternalName).toArray(String[]::new));

      // add the base rpc and class sender field
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "base", RPC_DESC, null, null).visitEnd();
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "sender", SENDER_DESC, null, null).visitEnd();
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "channelSupplier", SUPPLIER_DESC, null, null).visitEnd();

      // generate the constructor
      MethodVisitor mv;
      {
        // generate the constructor
        mv = cw.visitMethod(
          ACC_PUBLIC,
          "<init>",
          String.format(
            "(%s%s%s%s)V",
            RPC_DESC,
            SENDER_DESC,
            SUPPLIER_DESC,
            superDesc),
          null,
          null);
        mv.visitCode();

        // assign the rpc "base" field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, "base", RPC_DESC);

        // assign the sender field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitFieldInsn(PUTFIELD, className, "sender", SENDER_DESC);

        // assign the supplier field if present
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitFieldInsn(PUTFIELD, className, "channelSupplier", SUPPLIER_DESC);

        // load the arguments for the super constructor to the stack
        mv.visitVarInsn(ALOAD, 0);
        var stackHelper = StackIndexHelper.create(4); // 0 = this, 1-3 are taken by required parameters
        for (var type : types) {
          stackHelper.load(mv, type);
        }
        // visit the super constructor call
        mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", '(' + superDesc + ")V", false);

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

          // get the base rpc field we want to join on
          mv.visitVarInsn(ALOAD, 0);
          mv.visitFieldInsn(GETFIELD, className, "base", RPC_DESC);
          // generate the invoke method (the method we want to invoke)
          visitInvokeMethod(className, method, mv);
          // actually visit the join method, taking the base rpc as the argument
          mv.visitMethodInsn(INVOKEINTERFACE, CHAINABLE_RPC_NAME, "join", RPC_JOIN_METHOD_DESC, true);
          // fires the rpc
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
        .exactTypeAt(Constructor::getParameterTypes, RPC.class, 0)
        .exactTypeAt(Constructor::getParameterTypes, RPCSender.class, 1);

      // create the factory to create the rpc instance
      return Reflexion
        .on(definedClass)
        .findConstructor(constructorMatcher)
        .map(accessor -> (ChainInstanceFactory<T>) (constructorArgs, rpcArgs) -> {
          // collect the arguments for the invocation
          var baseRPC = rpcFactory.apply(rpcArgs);
          var arguments = new Object[]{baseRPC, classSender, context.channelSupplier()};
          // check if we need to add specific constructor args
          if (constructorArgs != null) {
            arguments = ObjectArrays.concat(arguments, constructorArgs, Object.class);
          }
          // construct the new class instance
          return (T) accessor.invokeWithArgs(arguments).getOrThrow();
        })
        .orElseThrow();
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate api class implementation for class %s",
        baseClass.getName()), exception);
    }
  }

  static @NonNull List<Class<?>> findSuperConstructorTypes(@Nullable Class<?> extendingClass) {
    if (extendingClass == null) {
      return List.of();
    }

    return Arrays.stream(extendingClass.getConstructors())
      .reduce(BinaryOperator.maxBy(Comparator.comparingInt(Constructor::getParameterCount)))
      .map(Constructor::getParameterTypes)
      .map(List::of)
      .orElse(List.of());
  }
}
