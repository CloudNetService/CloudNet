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

package de.dytanic.cloudnet.driver.util.asm;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.SIPUSH;

import com.google.common.primitives.Primitives;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class AsmUtils {

  private AsmUtils() {
    throw new UnsupportedOperationException();
  }

  public static void pushInt(@NotNull MethodVisitor mv, int value) {
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

  public static void wrapperToPrimitive(@NotNull MethodVisitor mv, @NotNull Class<?> primitiveType) {
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

  public static void primitiveToWrapper(@NotNull MethodVisitor mv, @NotNull Class<?> primitiveType) {
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
