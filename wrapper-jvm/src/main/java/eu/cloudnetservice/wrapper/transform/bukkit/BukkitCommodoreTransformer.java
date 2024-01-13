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
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class BukkitCommodoreTransformer implements Transformer {

  @Override
  public void transform(@NonNull String classname, @NonNull ClassNode classNode) {
    for (var method : classNode.methods) {
      // prevent bukkit from re-transforming modern api using plugins on older minecraft servers by just returning
      if (method.name.equals("convert") && method.desc.equals("([BZ)[B")) {
        // check if bukkit is using an outdated asm version
        var asmVersion = -1;
        for (var instruction : method.instructions) {
          if (instruction.getOpcode() == Opcodes.LDC
            && instruction instanceof LdcInsnNode ldcInsnNode
            && ldcInsnNode.cst instanceof Integer integer) {
            asmVersion = integer;
            break;
          }
        }

        // we only need to prevent bukkit from doing stuff when they're using asm 7
        if (asmVersion < Opcodes.ASM8) {
          for (var instruction : method.instructions) {
            // search for the init of the class reader
            if (instruction.getOpcode() == Opcodes.INVOKESPECIAL
              && instruction instanceof MethodInsnNode methodInsnNode
              && methodInsnNode.name.equals("<init>")
              && methodInsnNode.owner.endsWith("org/objectweb/asm/ClassReader")) {
              // the next instruction will store the value to the stack
              var next = instruction.getNext();
              if (next != null && next.getOpcode() == Opcodes.ASTORE && next instanceof VarInsnNode varInsnNode) {
                // we need two labels - one before the reader creation, one after
                var beginLabel = new LabelNode();
                var finishLabel = new LabelNode();
                var handlerLabel = new LabelNode();
                // insert the labels
                method.instructions.insert(beginLabel);
                method.instructions.insert(next, finishLabel);

                var catchDimension = new InsnList();
                // begin the error handler
                catchDimension.add(handlerLabel);
                // on an IllegalArgumentException exception just return the input (indicates an unsupported class version)
                catchDimension.add(new VarInsnNode(Opcodes.ALOAD, 0));
                catchDimension.add(new InsnNode(Opcodes.ARETURN));
                method.instructions.add(catchDimension);
                // insert the try-catch block
                method.tryCatchBlocks.add(new TryCatchBlockNode(
                  beginLabel,
                  finishLabel,
                  handlerLabel,
                  Type.getInternalName(IllegalArgumentException.class)));

                var nonRecordValidateDimension = new InsnList();
                // validate that the method is called using the correct asm version
                var validationEnvironment = new Label();
                // load the class reader again
                nonRecordValidateDimension.add(new VarInsnNode(Opcodes.ALOAD, varInsnNode.var));
                // get the accessors of the class
                nonRecordValidateDimension.add(new MethodInsnNode(
                  Opcodes.INVOKEVIRTUAL,
                  methodInsnNode.owner,
                  "getAccess",
                  "()I",
                  false));
                // push the record offset to the stack and apply it to the previous accessors using &
                // (from the code perspective: reader.getAccess() & Opcodes.ACC_RECORD
                nonRecordValidateDimension.add(new LdcInsnNode(Opcodes.ACC_RECORD));
                nonRecordValidateDimension.add(new InsnNode(Opcodes.IAND));
                // check if the class is a record
                nonRecordValidateDimension.add(new JumpInsnNode(Opcodes.IFEQ, new LabelNode(validationEnvironment)));
                // if the class is a record just return the input class bytes
                nonRecordValidateDimension.add(new VarInsnNode(Opcodes.ALOAD, 0));
                nonRecordValidateDimension.add(new InsnNode(Opcodes.ARETURN));
                nonRecordValidateDimension.add(new LabelNode(validationEnvironment));
                // insert the instructions now
                method.instructions.insert(next, nonRecordValidateDimension);
                return;
              }
            }
          }
        }
      }
    }
  }
}
