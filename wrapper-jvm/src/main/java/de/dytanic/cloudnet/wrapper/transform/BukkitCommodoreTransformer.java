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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class BukkitCommodoreTransformer implements ClassFileTransformer {

  @Override
  public byte[] transform(ClassLoader __, String className, Class<?> ___, ProtectionDomain ____, byte[] classBytes) {
    if (className.startsWith("org/bukkit/craftbukkit/") && className.endsWith("Commodore")) {
      var node = new ClassNode();
      var reader = new ClassReader(classBytes);
      reader.accept(node, 0);

      for (var method : node.methods) {
        if (method.name.equals("convert") && method.desc.equals("([BZ)[B")) {
          for (var instruction : method.instructions) {
            if (instruction.getOpcode() == Opcodes.LDC
              && instruction instanceof LdcInsnNode ldcInsnNode
              && ldcInsnNode.cst instanceof Integer asmVersion
              && asmVersion == Opcodes.ASM7) {
              method.instructions.set(instruction, new LdcInsnNode(Opcodes.ASM9));
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
}
