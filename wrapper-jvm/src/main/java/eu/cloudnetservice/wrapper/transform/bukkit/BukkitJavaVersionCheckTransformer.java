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

package eu.cloudnetservice.wrapper.transform.bukkit;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import lombok.NonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class BukkitJavaVersionCheckTransformer implements ClassTransformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    var state = SearchingState.SEARCHING;
    for (var method : classNode.methods) {
      if (method.name.equals("main")) {
        // get the store index
        var index = this.findVersionStoreIndex(method);
        if (index != -1) {
          // we found the store index - watch for loads
          for (var instruction : method.instructions) {
            if (state == SearchingState.SEARCHING
              && instruction.getOpcode() == Opcodes.FLOAD
              && instruction instanceof VarInsnNode varInsnNode
              && varInsnNode.var == index) {
              // wait for the next if condition with the correct op code
              state = SearchingState.WAITING;
              continue;
            }

            // check if we're searching for the next if - if we found one begin to remove the if body
            if (state == SearchingState.WAITING
              && (instruction.getOpcode() == Opcodes.IFLE || instruction.getOpcode() == Opcodes.IFNE)) {
              // nuke everything until we find a return op code
              state = SearchingState.REMOVING;
              continue;
            }

            if (state == SearchingState.REMOVING) {
              method.instructions.remove(instruction);
              // the last instruction is a return - stop now
              if (instruction.getOpcode() == Opcodes.RETURN) {
                state = SearchingState.SEARCHING;
              }
            }
          }
        }
      }
    }
  }

  private int findVersionStoreIndex(@NonNull MethodNode methodNode) {
    for (var instruction : methodNode.instructions) {
      // finds the load instruction of the java class version
      if (instruction.getOpcode() == Opcodes.LDC
        && instruction instanceof LdcInsnNode node
        && node.cst instanceof String string
        && string.equals("java.class.version")) {
        // try to resolve the store index
        var next = node.getNext().getNext().getNext();
        // check if we found the correct node
        if (next.getOpcode() == Opcodes.FSTORE && next instanceof VarInsnNode varInsnNode) {
          return varInsnNode.var;
        }
      }
    }
    return -1;
  }

  private enum SearchingState {

    SEARCHING,
    WAITING,
    REMOVING
  }
}
