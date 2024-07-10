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

package eu.cloudnetservice.wrapper.transform.minestom;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class MinestomStopCleanlyTransformer implements ClassTransformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    for (var method : classNode.methods) {
      // Find the stop method that does not have any parameters and returns void
      if (method.name.equals("stop")) {
        var instructions = new InsnList();

        // System.exit(0);
        instructions.add(new InsnNode(Opcodes.ICONST_0));
        instructions.add(new MethodInsnNode(
          Opcodes.INVOKESTATIC,
          Type.getInternalName(System.class),
          "exit",
          "(I)V"
        ));

        var lastReturnStatement = findLastReturn(method);
        if (lastReturnStatement == null) {
          // If no return statement was found, just add the instructions at the end of the method
          method.instructions.add(instructions);
        } else {
          method.instructions.insertBefore(lastReturnStatement, instructions);
        }
      }
    }
  }

  /**
   * Find the last return instruction in the method. This method is useful to skip instructions in the method that come
   * after the return statement, such as a label node or a line number node.
   *
   * @param method The method to search in
   * @return The last return instruction in the method, or null if no return instruction was found
   * @throws NullPointerException if the given method node is null.
   */
  private static @Nullable AbstractInsnNode findLastReturn(@NonNull MethodNode method) {
    var instruction = method.instructions.getLast();
    while (instruction.getOpcode() != Opcodes.RETURN) {
      instruction = instruction.getPrevious();
    }
    return instruction;
  }
}
