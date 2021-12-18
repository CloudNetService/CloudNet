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

package de.dytanic.cloudnet.wrapper.transform;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import lombok.NonNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class BukkitJavaVersionCheckTransformer implements ClassFileTransformer {

  @Override
  public byte[] transform(ClassLoader __, String className, Class<?> ___, ProtectionDomain ____, byte[] classBytes) {
    if (className.startsWith("org/bukkit/craftbukkit/") && className.endsWith("Main")) {
      var node = new ClassNode();
      var reader = new ClassReader(classBytes);
      reader.accept(node, 0);

      // 0 - searching for fload
      // 1 - waiting for if
      // 2 - removing everything
      var state = 0;
      for (var method : node.methods) {
        if (method.name.equals("main")) {
          // get the store index
          var index = this.findVersionStoreIndex(method);
          if (index != -1) {
            // we found the store index - watch for loads
            for (var instruction : method.instructions) {
              if (state == 0
                && instruction.getOpcode() == Opcodes.FLOAD
                && instruction instanceof VarInsnNode varInsnNode
                && varInsnNode.var == index) {
                // get the weapons
                state = 1;
                continue;
              }

              // check if we're watching and if the current insn is a compare if
              if (state == 1 && (instruction.getOpcode() == Opcodes.IFLE || instruction.getOpcode() == Opcodes.IFNE)) {
                state = 2;
                continue;
              }

              if (state == 2) {
                method.instructions.remove(instruction);
                if (instruction.getOpcode() == Opcodes.RETURN) {
                  state = 0;
                }
              }
            }
          }
        }
      }

      var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      node.accept(writer);
      return writer.toByteArray();
    }

    return null;
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
}
