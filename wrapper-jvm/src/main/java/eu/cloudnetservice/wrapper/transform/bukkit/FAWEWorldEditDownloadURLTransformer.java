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

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.constant.ConstantDescs;
import lombok.NonNull;

public class FAWEWorldEditDownloadURLTransformer implements ClassTransformer {

  private static final String OLD_DOWNLOAD_URL = "https://addons.cursecdn.com/files/2431/372/worldedit-bukkit-6.1.7.2.jar";
  private static final String NEW_DOWNLOAD_URL = "https://mediafilez.forgecdn.net/files/2431/372/worldedit-bukkit-6.1.7.2.jar";
  private static final String CNI_JARS = "com/boydti/fawe/util/Jars";

  @Override
  public @NonNull ClassTransform provideClassTransform() {
    CodeTransform codeTransform = (builder, element) -> {
      if (element instanceof ConstantInstruction.LoadConstantInstruction loadConstant) {
        if (loadConstant.opcode() == Opcode.LDC) {
          if (loadConstant.constantValue().equals(OLD_DOWNLOAD_URL)) {
            builder.ldc(NEW_DOWNLOAD_URL);
            return;
          }
        }
      }
      builder.with(element);
    };
    return ClassTransform.transformingMethodBodies(
      mm -> mm.methodName().equalsString(ConstantDescs.CLASS_INIT_NAME),
      codeTransform);
  }

  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isJars = internalClassName.equals(CNI_JARS);
    return isJars ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }
}
