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

package eu.cloudnetservice.driver.event.invoker;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.event.EventListenerException;
import eu.cloudnetservice.driver.util.define.ClassDefiners;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Utility class to generate listener invokers for listeners using runtime code generation.
 *
 * @see ListenerInvoker
 * @since 4.0
 */
@ApiStatus.Internal
public final class ListenerInvokerGenerator {

  private static final AtomicInteger ID = new AtomicInteger();

  private static final String SUPER = "java/lang/Object";
  private static final String[] INVOKER = new String[]{Type.getInternalName(ListenerInvoker.class)};
  private static final String INVOKE_DESC = Type.getMethodDescriptor(
    Type.VOID_TYPE,
    Type.getType(Object.class),
    Type.getType(Event.class));

  /**
   * Generates a new listener invoker for the given event listener method.
   *
   * @param listener the instance of the listener class generating for.
   * @param method   the method for which an invoker gets generated.
   * @param event    the event class which the listener is handling.
   * @return a generated event listener to invoke the target listener method.
   * @throws NullPointerException   if the given listener, method or event class is null
   * @throws EventListenerException if an exception occurs during the code generation.
   */
  public static @NonNull ListenerInvoker generate(
    @NonNull Object listener,
    @NonNull Method method,
    @NonNull Class<?> event
  ) {
    try {
      // make a class name which is definitely unique for the method
      var className = String.format(
        "%s$%s_%d",
        Type.getInternalName(listener.getClass()),
        method.getName(),
        ID.incrementAndGet());

      // init the class writer for a public final class implementing the ListenerInvoker
      var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, SUPER, INVOKER);
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
        mv = cw.visitMethod(ACC_PUBLIC, "invoke", INVOKE_DESC, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(listener.getClass()));
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(event));
        mv.visitMethodInsn(
          INVOKEVIRTUAL,
          Type.getInternalName(listener.getClass()),
          method.getName(),
          Type.getMethodDescriptor(method),
          false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      // finish construction
      cw.visitEnd();
      // define and make the constructor accessible
      var constructor = ClassDefiners.current()
        .defineClass(className, listener.getClass(), cw.toByteArray())
        .getDeclaredConstructor();
      constructor.setAccessible(true);
      // instantiate
      return (ListenerInvoker) constructor.newInstance();
    } catch (Exception exception) {
      throw new EventListenerException(String.format(
        "Failed to generate event invoker for listener method %s in %s",
        method.getName(),
        listener.getClass().getCanonicalName()
      ), exception);
    }
  }
}
