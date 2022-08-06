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

package eu.cloudnetservice.driver.util.asm;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.SIPUSH;

import com.google.common.primitives.Primitives;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * The AsmHelper provides util methods for commonly used asm operations.
 *
 * @see org.objectweb.asm.Opcodes
 * @since 4.0
 */
@ApiStatus.Internal
public final class AsmHelper {

  private AsmHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Pushes the given int value onto the stack. The following table shows the opcodes which are used based on the given
   * int value.
   * <ul>
   *   <li>values &lt; -1 or &gt; {@code Short.MAX_VALUE}: LDC
   *   <li>values &gt;= -1 and &lt;= 5: ICONST_M1 - ICONST_5
   *   <li>values &lt;= {@code Byte.MAX_VALUE}: BIPUSH
   *   <li>values &lt;= {@code Short.MAX_VALUE}: SIPUSH
   * </ul>
   *
   * @param mv    the visitor to push to.
   * @param value the value to push onto the stack.
   */
  public static void pushInt(@NonNull MethodVisitor mv, int value) {
    if (value < -1) {
      mv.visitLdcInsn(value);
    } else if (value <= 5) {
      mv.visitInsn(ICONST_0 + value);
    } else if (value <= Byte.MAX_VALUE) {
      mv.visitIntInsn(BIPUSH, value);
    } else if (value <= Short.MAX_VALUE) {
      mv.visitIntInsn(SIPUSH, value);
    } else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Unwraps a primitive wrapper to the raw primitive type and pushes it onto the given {@link MethodVisitor}.
   * Counterpart to this is {@link AsmHelper#primitiveToWrapper(MethodVisitor, Class)}.
   *
   * @param mv            the visitor to push to.
   * @param primitiveType the class of the primitive type
   */
  public static void wrapperToPrimitive(@NonNull MethodVisitor mv, @NonNull Class<?> primitiveType) {
    var wrapper = Primitives.wrap(primitiveType);
    // cast to the wrapper type
    mv.visitTypeInsn(CHECKCAST, org.objectweb.asm.Type.getInternalName(wrapper));
    // convert to the primitive type
    mv.visitMethodInsn(
      INVOKEVIRTUAL,
      org.objectweb.asm.Type.getInternalName(wrapper),
      primitiveType.getSimpleName() + "Value", // for example: int.class.getSimpleName() + Value = intValue()
      org.objectweb.asm.Type.getMethodDescriptor(org.objectweb.asm.Type.getType(primitiveType)),
      false);
  }

  /**
   * Wraps a primitive type into it's wrapper type and pushes it onto the given {@link MethodVisitor}. Counterpart to
   * this is {@link AsmHelper#wrapperToPrimitive(MethodVisitor, Class)}.
   *
   * @param mv            the visitor to push to.
   * @param primitiveType the class of the primitive type
   */
  public static void primitiveToWrapper(@NonNull MethodVisitor mv, @NonNull Class<?> primitiveType) {
    var wrapper = Primitives.wrap(primitiveType);
    // invoke the valueOf method in the wrapper to class to convert the primitive type to an object
    mv.visitMethodInsn(
      INVOKESTATIC,
      Type.getInternalName(wrapper),
      "valueOf",
      Type.getMethodDescriptor(Type.getType(wrapper), Type.getType(primitiveType)),
      false);
  }
}
