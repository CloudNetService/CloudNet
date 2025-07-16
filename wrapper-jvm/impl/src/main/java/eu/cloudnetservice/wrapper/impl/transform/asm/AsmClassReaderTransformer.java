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

package eu.cloudnetservice.wrapper.impl.transform.asm;

import eu.cloudnetservice.wrapper.impl.transform.ClassTransformer;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Transformer to remove the class version check from the ASM class reader constructor.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class AsmClassReaderTransformer implements ClassTransformer {

  // ASM 4, 5 & 6: <init>(byte[], int, int)
  private static final MethodTypeDesc LEGACY_ASM_CTR_MT = MethodTypeDesc.of(
    ConstantDescs.CD_void,
    ConstantDescs.CD_byte.arrayType(),
    ConstantDescs.CD_int,
    ConstantDescs.CD_int);
  // ASM 7+: <init>(byte[], int, boolean)
  private static final MethodTypeDesc MODERN_ASM_CTR_MT = MethodTypeDesc.of(
    ConstantDescs.CD_void,
    ConstantDescs.CD_byte.arrayType(),
    ConstantDescs.CD_int,
    ConstantDescs.CD_boolean);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public AsmClassReaderTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform(@NonNull ClassModel original) {
    // checks if the class should be transformed. this check is necessary as the transformer is targeting
    // a class that might be relocated in another jar. this means that just checking for the full class
    // name is not an option, and we need to check for the simple class name and some other indicator
    // to find out if we're transforming the correct class. in this case, we're looking for the
    // ClassReader.accept(ClassVisitor, Attribute[], int) method
    var shouldTransform = original.methods().stream().anyMatch(method -> {
      var mt = method.methodTypeSymbol();
      var paramTypes = mt.parameterArray();
      if (method.methodName().equalsString("accept") && paramTypes.length == 3) {
        var firstParamIsCV = paramTypes[0].displayName().equals("ClassVisitor");
        var secondParamIsAttrArray = paramTypes[1].displayName().equals("Attribute[]");
        var thirdParamIsInt = paramTypes[2].displayName().equals("int");
        return firstParamIsCV && secondParamIsAttrArray && thirdParamIsInt;
      }

      return false;
    });
    if (!shouldTransform) {
      return ClassTransform.ACCEPT_ALL;
    }

    // finds the model of the method to transform, preferring the modern constructor over the legacy one
    var hasModernCtr = original.methods().stream().anyMatch(mm -> mm.methodTypeSymbol().equals(MODERN_ASM_CTR_MT));
    var targetCtrMt = hasModernCtr ? MODERN_ASM_CTR_MT : LEGACY_ASM_CTR_MT;

    var codeTransform = new AsmConstructorTransformer();
    return ClassTransform.transformingMethodBodies(mm -> mm.methodTypeSymbol().equals(targetCtrMt), codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    return internalClassName.endsWith("/ClassReader") ? TransformWillingness.ACCEPT : TransformWillingness.REJECT;
  }

  /**
   * Code transform that removes the throw instruction on an unsupported class file major from the target method.
   *
   * @since 4.0
   */
  private static final class AsmConstructorTransformer implements CodeTransform {

    private static final ClassDesc CD_ILLEGAL_ARG_EX = ClassDesc.of(IllegalArgumentException.class.getName());

    private boolean ifCmpSeen;
    private boolean readShortSeen;
    private boolean illegalArgConstructSeen;
    private boolean illegalArgThrowRemoved;

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull CodeBuilder builder, @NonNull CodeElement element) {
      var dropInstruction = switch (element) {
        case BranchInstruction inst -> {
          this.ifCmpSeen |= inst.opcode() == Opcode.IF_ICMPLE;
          yield false; // keep inst
        }
        case InvokeInstruction inst -> {
          var owner = inst.owner().asSymbol();
          this.readShortSeen |= inst.opcode() == Opcode.INVOKEVIRTUAL
            && owner.displayName().equals("ClassReader")
            && inst.method().name().equalsString("readShort");
          yield false; // keep inst
        }
        case NewObjectInstruction inst -> {
          this.illegalArgConstructSeen |= inst.className().asSymbol().equals(CD_ILLEGAL_ARG_EX);
          yield false; // keep inst
        }
        case ThrowInstruction _ when this.ifCmpSeen
          && this.readShortSeen
          && this.illegalArgConstructSeen
          && !this.illegalArgThrowRemoved -> {
          this.illegalArgThrowRemoved = true;
          builder.pop(); // pop constructed IAE from stack
          yield true; // drop inst
        }
        default -> false; // keep inst
      };
      if (!dropInstruction) {
        builder.with(element);
      }
    }
  }
}
