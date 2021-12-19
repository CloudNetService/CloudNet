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

package de.dytanic.cloudnet.wrapper.transform.bukkit;

import de.dytanic.cloudnet.wrapper.transform.Transformer;
import lombok.NonNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class BukkitCommodoreTransformer implements Transformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    for (var method : classNode.methods) {
      // prevent bukkit from re-transforming modern api using plugins on older minecraft servers by just returning
      if (method.name.equals("convert") && method.desc.equals("([BZ)[B")) {
        var instructions = new InsnList();
        var label = new Label();

        instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, new LabelNode(label)));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new InsnNode(Opcodes.ARETURN));
        instructions.add(new LabelNode(label));

        method.instructions.insert(instructions);
      }
    }
  }
}
