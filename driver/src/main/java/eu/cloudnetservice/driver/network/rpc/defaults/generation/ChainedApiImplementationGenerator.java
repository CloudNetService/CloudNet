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

package eu.cloudnetservice.driver.network.rpc.defaults.generation;

import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.DEFAULT_SUPER;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.GENERATED_CLASS_NAME_FORMAT;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.SENDER_DESC;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.collectMethodsToVisit;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.visitFireMethod;
import static eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator.visitInvokeMethod;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.driver.network.rpc.ChainableRPC;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.exception.ClassCreationException;
import eu.cloudnetservice.driver.network.rpc.generation.ChainInstanceFactory;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceId;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ChainedApiImplementationGenerator {

  private static final String RPC_DESC = Type.getDescriptor(RPC.class);
  private static final Type RPC_TYPE = Type.getType(RPC.class);
  private static final Type CHAIN_RPC_TYPE = Type.getType(RPCChain.class);
  private static final String CHAIN_RPC_DESC = Type.getMethodDescriptor(CHAIN_RPC_TYPE, RPC_TYPE);
  private static final String CHAIN_RPC_NAME = Type.getInternalName(ChainableRPC.class);

  private ChainedApiImplementationGenerator() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull <T> ChainInstanceFactory<T> generateApiImplementation(
    @NonNull Class<T> baseClass,
    @NonNull GenerationContext context,
    @NonNull RPC baseRPC,
    @NonNull RPCSender classSender
  ) {
    try {
      var className = String.format(
        GENERATED_CLASS_NAME_FORMAT,
        Type.getInternalName(baseClass),
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
      // add the base rpc field
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "base", RPC_DESC, null, null).visitEnd();
      // add the sender field
      cw.visitField(ACC_PRIVATE | ACC_FINAL, "sender", SENDER_DESC, null, null).visitEnd();

      MethodVisitor mv;
      {
        // generate the constructor
        mv = cw.visitMethod(
          ACC_PUBLIC,
          "<init>",
          String.format(
            "(%s%s%s)V",
            RPC_DESC,
            SENDER_DESC,
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

        // call the super "<init>" method*
        mv.visitVarInsn(ALOAD, 0);
        for (int i = 0; i < types.size(); i++) {
          var type = types.get(i);
          var wrappedType = Type.getType(type);
          // first two arguments are special, we allow them to be the sender or base rpc as well
          if ((i == 0 || i == 1)) {
            // check for the base rpc
            if (type.equals(RPC.class)) {
              mv.visitVarInsn(ALOAD, 1);
              continue;
            }

            // check for the sender
            if (type.equals(RPCSender.class)) {
              mv.visitVarInsn(ALOAD, 2);
              continue;
            }
          }

          // visit the corresponding type index
          mv.visitVarInsn(wrappedType.getOpcode(ILOAD), 3 + i);
        }
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
          // generate the join on the base rpc
          mv.visitVarInsn(ALOAD, 0);
          mv.visitFieldInsn(GETFIELD, className, "base", RPC_DESC);

          // generate the invoke method
          visitInvokeMethod(className, method, mv);

          mv.visitMethodInsn(INVOKEVIRTUAL, CHAIN_RPC_NAME, "join", CHAIN_RPC_DESC, false);

          // we cannot ignore the return type if not void
          visitFireMethod(method, mv);

          // finish the method
          mv.visitMaxs(0, 0);
          mv.visitEnd();
        }
      }
      // finish the class
      cw.visitEnd();
      // define & select the correct constructor for the class
      /**var constructor = ClassDefiners.current()
       .defineClass(className, baseClass, cw.toByteArray())
       .getDeclaredConstructor(RPCSender.class);
       constructor.setAccessible(true);
       // instantiate the new class
       return (T) constructor.newInstance(sender);*/

      var constructorTypes =




      Files.write(Path.of("test.class"), cw.toByteArray());


    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate api class implementation for class %s",
        baseClass.getName()), exception);
    }
  }

  private static @NonNull List<Class<?>> findSuperConstructorTypes(@Nullable Class<?> extendingClass) {
    if (extendingClass == null) {
      return List.of();
    }

    return Arrays.stream(extendingClass.getConstructors())
      .reduce(BinaryOperator.maxBy(Comparator.comparingInt(Constructor::getParameterCount)))
      .map(Constructor::getParameterTypes)
      .map(List::of)
      .orElse(List.of());
  }

  public static class TestAbc extends ServiceId {

    /**
     * Creates a new service identity instance.
     *
     * @param taskName        the name of the task this service is based on.
     * @param nameSplitter    the splitter of the service name, to put between the task name and task service id.
     * @param allowedNodes    the nodes which are allowed to start the service.
     * @param uniqueId        the unique id (version 4) of the service, there is never a duplicate within CloudNet.
     * @param taskServiceId   the numeric id of the service within the task.
     * @param nodeUniqueId    the unique id of the node which picked up the service, null if not yet elected.
     * @param environmentName the name of the environment type of the service.
     * @param environment     the resolved environment type, null if not yet resolved.
     * @throws NullPointerException if any given parameter is null, except for the resolved environment type and node.
     */
    protected TestAbc(String taskName, String nameSplitter, Set<String> allowedNodes,
      UUID uniqueId, int taskServiceId, String nodeUniqueId, String environmentName,
      ServiceEnvironmentType environment) {
      super(taskName, nameSplitter, allowedNodes, uniqueId, taskServiceId, nodeUniqueId, environmentName, environment);
    }
  }
}
