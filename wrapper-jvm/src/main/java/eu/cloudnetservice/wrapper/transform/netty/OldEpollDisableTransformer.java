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

package eu.cloudnetservice.wrapper.transform.netty;

import eu.cloudnetservice.wrapper.transform.Transformer;
import lombok.NonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class OldEpollDisableTransformer implements Transformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    // check if epoll should get disabled
    for (var method : classNode.methods) {
      if (method.name.equals("<clinit>")) {
        // old versions of netty are invoking a method called "epollCreate", new ones are invoking "newEpollCreate"
        for (var instruction : method.instructions) {
          if (instruction instanceof MethodInsnNode methodInsnNode && methodInsnNode.name.equals("epollCreate")) {
            var instructions = new InsnList();
            // if the old epoll method is used then just set the unavailability cause
            instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(UnsupportedOperationException.class)));
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new LdcInsnNode("Netty 4.0.X is incompatible with Java 9+"));
            instructions.add(new MethodInsnNode(
              Opcodes.INVOKESPECIAL,
              Type.getInternalName(UnsupportedOperationException.class),
              "<init>",
              "(Ljava/lang/String;)V",
              false));
            instructions.add(new FieldInsnNode(
              Opcodes.PUTSTATIC,
              // This prevents shadow from renaming io/netty to eu/cloudnetservice/io/netty
              String.join("/", "io", "netty", "channel", "epoll", "Epoll"),
              "UNAVAILABILITY_CAUSE",
              Type.getDescriptor(Throwable.class)));
            instructions.add(new InsnNode(Opcodes.RETURN));
            // put it before the first instruction of the method
            method.instructions.insert(instructions);
            return;
          }
        }
      }
    }
  }
}
