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

package de.dytanic.cloudnet.wrapper.transform.netty;

import de.dytanic.cloudnet.wrapper.transform.Transformer;
import lombok.NonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class OldEpollDisableTransformer implements Transformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    var usesOldEpollCreateMethod = false;
    // check if epoll should get disabled
    for (var method : classNode.methods) {
      if (method.name.equals("<clinit>")) {
        // old versions of netty are invoking a method called "epollCreate", new ones are invoking "newEpollCreate"
        for (var instruction : method.instructions) {
          if (instruction instanceof MethodInsnNode methodInsnNode && methodInsnNode.name.equals("epollCreate")) {
            usesOldEpollCreateMethod = true;
          }
        }
      }
    }

    // if there is an old method call
    if (usesOldEpollCreateMethod) {
      for (var method : classNode.methods) {
        // isAvailable? nope
        if (method.name.equals("isAvailable")) {
          method.instructions.insert(new InsnNode(Opcodes.IRETURN));
          method.instructions.insert(new InsnNode(Opcodes.ICONST_0));
          continue;
        }

        // ensureAvailability? sure do
        if (method.name.equals("ensureAvailability")) {
          var instructions = new InsnList();
          // just rethrow the result of unavailabilityCause()
          instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            // This prevents shadow from renaming io/netty to eu/cloudnetservice/io/netty
            String.format("%s/%s/%s", "io", "netty", "channel/epoll/Epoll"),
            "unavailabilityCause",
            "()Ljava/lang/Throwable;",
            false));
          instructions.add(new InsnNode(Opcodes.ATHROW));
          instructions.add(new InsnNode(Opcodes.RETURN));
          // add the instructions
          method.instructions.insert(instructions);
          continue;
        }

        // unavailabilityCause? just not available
        if (method.name.equals("unavailabilityCause")) {
          var instructions = new InsnList();
          // init a new throwable with no real information
          instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(UnsatisfiedLinkError.class)));
          instructions.add(new InsnNode(Opcodes.DUP));
          instructions.add(new LdcInsnNode("failed to load the required native library"));
          instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            Type.getInternalName(UnsatisfiedLinkError.class),
            "<init>",
            "(Ljava/lang/String;)V",
            false));
          instructions.add(new InsnNode(Opcodes.ARETURN));
          // add the instructions
          method.instructions.insert(instructions);
        }
      }
    }
  }
}
