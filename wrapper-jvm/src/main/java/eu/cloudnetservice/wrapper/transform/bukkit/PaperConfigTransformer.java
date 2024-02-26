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

import eu.cloudnetservice.wrapper.transform.Transformer;
import lombok.NonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;

public final class PaperConfigTransformer implements Transformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    for (var method : classNode.methods) {
      // this method tries to use reflections to modify the modifiers of a field. Stop that
      if (method.name.equals("stackableBuckets")) {
        method.instructions.insert(new InsnNode(Opcodes.RETURN));
      }
    }
  }
}
