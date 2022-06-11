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

package eu.cloudnetservice.wrapper.transform.fabric;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import eu.cloudnetservice.wrapper.transform.Transformer;
import lombok.NonNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class KnotClassDelegateTransformer implements Transformer {

  private static void insertLoadFromParent(@NonNull InsnList instructions, @NonNull String prefix) {
    var ifDimension = new Label();

    // checks if the class name starts with the given prefix
    instructions.add(new VarInsnNode(ALOAD, 1));
    instructions.add(new LdcInsnNode(prefix));
    instructions.add(new MethodInsnNode(
      INVOKEVIRTUAL,
      Type.getInternalName(String.class),
      "startsWith",
      "(Ljava/lang/String;)Z",
      false));
    instructions.add(new JumpInsnNode(IFEQ, new LabelNode(ifDimension)));

    // loads the parent class loader which holds our classes
    instructions.add(new MethodInsnNode(
      INVOKESTATIC,
      Type.getInternalName(ClassLoader.class),
      "getSystemClassLoader",
      "()Ljava/lang/ClassLoader;",
      false));
    instructions.add(new VarInsnNode(ALOAD, 1));
    instructions.add(new MethodInsnNode(
      INVOKEVIRTUAL,
      Type.getInternalName(ClassLoader.class),
      "loadClass",
      "(Ljava/lang/String;)Ljava/lang/Class;",
      false));
    instructions.add(new InsnNode(ARETURN));
    instructions.add(new LabelNode(ifDimension));
  }

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    for (var method : classNode.methods) {
      if (method.name.equals("loadClass")) {
        var instructions = new InsnList();

        // if checks
        insertLoadFromParent(instructions, "eu.cloudnetservice.wrapper.");
        insertLoadFromParent(instructions, "eu.cloudnetservice.common.");
        insertLoadFromParent(instructions, "eu.cloudnetservice.driver.");

        // insert the instructions
        method.instructions.insert(instructions);
      }
    }
  }
}
