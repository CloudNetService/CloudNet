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

package eu.cloudnetservice.wrapper.transform.fabric;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class KnotClassDelegateTransformer implements ClassTransformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    for (var method : classNode.methods) {
      if (method.name.equals("loadClass") && method.desc.equals("(Ljava/lang/String;Z)Ljava/lang/Class;")) {
        var loadedClassFwdInstruction = this.findStoreLoadedClassInstruction(method.instructions);
        if (loadedClassFwdInstruction != null) {
          var insnList = new InsnList();
          var notInParentLabel = new LabelNode();

          // Class<?> loadClass(String name, boolean resolve)
          insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
          insnList.add(new LdcInsnNode("org.objectweb.asm."));
          insnList.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "startsWith",
            "(Ljava/lang/String;)Z",
            false));
          insnList.add(new JumpInsnNode(Opcodes.IFEQ, notInParentLabel));
          insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
          insnList.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            "net/fabricmc/loader/impl/launch/knot/KnotClassDelegate",
            "parentClassLoader",
            "Ljava/lang/ClassLoader;"));
          insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
          insnList.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/ClassLoader",
            "loadClass",
            "(Ljava/lang/String;)Ljava/lang/Class;",
            false));
          insnList.add(new VarInsnNode(Opcodes.ASTORE, loadedClassFwdInstruction.var));
          insnList.add(notInParentLabel);

          method.instructions.insert(loadedClassFwdInstruction, insnList);
        }
      }
    }
  }

  private @Nullable VarInsnNode findStoreLoadedClassInstruction(@NonNull InsnList list) {
    var iterator = list.iterator();
    while (iterator.hasNext()) {
      var instruction = iterator.next();
      if (iterator.hasNext()
        && instruction instanceof MethodInsnNode node
        && node.getOpcode() == Opcodes.INVOKEINTERFACE
        && node.name.equals("findLoadedClassFwd")
      ) {
        return (VarInsnNode) iterator.next();
      }
    }

    return null;
  }
}
