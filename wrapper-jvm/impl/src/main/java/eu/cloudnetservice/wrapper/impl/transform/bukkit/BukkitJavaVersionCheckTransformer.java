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

package eu.cloudnetservice.wrapper.impl.transform.bukkit;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer that removes all java version checks done by Bukkit.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class BukkitJavaVersionCheckTransformer implements ClassTransformer {

  private static final String MN_MAIN = "main";
  private static final String CNI_CRAFT_BUKKIT_MAIN = "org/bukkit/craftbukkit/Main";

  private static final String CNI_SYSTEM = "java/lang/System";
  private static final String CNI_PRINT_STREAM = "java/io/PrintStream";
  private static final ClassDesc CD_PRINT_STREAM = ClassDesc.ofInternalName(CNI_PRINT_STREAM);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public BukkitJavaVersionCheckTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    var codeTransform = CodeTransform.ofStateful(BukkitJavaVersionCheckRemoveCodeTransform::new);
    return ClassTransform.transformingMethodBodies(mm -> mm.methodName().equalsString(MN_MAIN), codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isCraftBukkitMain = internalClassName.equals(CNI_CRAFT_BUKKIT_MAIN);
    return isCraftBukkitMain ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }

  /**
   * A code transformer that removes all java version checks from the CraftBukkit main class.
   *
   * @since 4.0
   */
  private static final class BukkitJavaVersionCheckRemoveCodeTransform implements CodeTransform {

    private static final byte DROP_STATE_IDLE = 0;
    private static final byte DROP_STATE_DROPPING = 1;
    private static final byte DROP_STATE_DISABLED = 2;

    // Holds the current instruction dropping state
    private byte dropState = DROP_STATE_IDLE;
    // Indicates if the coding is preparing to print something while we're dropping instructions
    private boolean preparingToPrint = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull CodeBuilder builder, @NonNull CodeElement element) {
      if (element instanceof ConstantInstruction.LoadConstantInstruction inst
        && inst.constantValue() instanceof String value) {
        if (value.equals("java.class.version") && this.dropState == DROP_STATE_IDLE) {
          // encountered the call get the java class version for the version check, start dropping all returns
          this.dropState = DROP_STATE_DROPPING;
        } else if (value.equals("nojline") || value.startsWith("Loading libraries")) {
          // this is the next thing done after the version check, validating if JLine should be used.
          // this option is there since 1.8, so it's a good anchor point which probably will not change in a while,
          // but, just to be sure, we also check if we reached the point of "Loading libraries" to not accidentally
          // replace the required final return instruction in the class
          this.dropState = DROP_STATE_DISABLED;
        }
      }

      // drop all instructions when the code is preparing to print something until
      // we encounter the part where the code would actually print, stop the force-dropping there
      if (this.preparingToPrint) {
        if (element instanceof InvokeInstruction inst
          && inst.opcode() == Opcode.INVOKEVIRTUAL
          && inst.name().stringValue().startsWith("print")
          && inst.owner().asInternalName().equals(CNI_PRINT_STREAM)) {
          this.preparingToPrint = false;
        }
        return;
      }

      // block return statements if we're currently dropping. this needs to be explicitly
      // checked as some if branches don't return, but still print out stuff
      if (element instanceof ReturnInstruction && this.dropState == DROP_STATE_DROPPING) {
        return;
      }

      // check if the current instruction prepares to print to the console by checking
      // if the field value that is being retrieved is a print stream coming from the system class
      if (element instanceof FieldInstruction inst
        && inst.opcode() == Opcode.GETSTATIC
        && inst.owner().asInternalName().equals(CNI_SYSTEM)
        && inst.type().equalsString(CD_PRINT_STREAM.descriptorString())
        && this.dropState == DROP_STATE_DROPPING) {
        this.preparingToPrint = true;
        return;
      }

      builder.accept(element);
    }
  }
}
