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

package eu.cloudnetservice.wrapper.transform.minestom;

import eu.cloudnetservice.wrapper.transform.Transformer;
import lombok.NonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class MinestomStopCleanlyTransformer implements Transformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    for (var method : classNode.methods) {
      if (method.name.equals("stopCleanly")) {
        var instructions = new InsnList();

        // System.exit(0);
        instructions.add(new InsnNode(Opcodes.ICONST_0));
        instructions.add(new MethodInsnNode(
          Opcodes.INVOKESTATIC,
          Type.getInternalName(System.class),
          "exit",
          "(I)V"
        ));

        method.instructions.insertBefore(method.instructions.getLast(), instructions);
        return;
      }
    }
  }
}
