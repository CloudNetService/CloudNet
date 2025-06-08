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

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.constant.ClassDesc;
import lombok.NonNull;

public class AsmClassReaderTransformer implements ClassTransformer {

  @Override
  public @NonNull ClassTransform provideClassTransform(@NonNull ClassModel original) {
    var shouldTransform = original.methods().stream().anyMatch(method -> {
      var mt = method.methodTypeSymbol();
      if (method.methodName().equalsString("accept") && mt.parameterCount() == 3) {
        var classVisitor = mt.parameterType(0).displayName().equals("ClassVisitor");
        var attributeArray = mt.parameterType(1).displayName().equals("Attribute[]");
        var integer = mt.parameterType(2).displayName().equals("int");
        return classVisitor && attributeArray && integer;
      }

      return false;
    });

    if (shouldTransform) {
      var newMethodModel = original.methods().stream()
        .filter(this::checkNewAsmConstructorSignature)
        .findAny()
        .orElse(null);
      // ASM 4, 5 & 6: <init>(byte[], int, int)
      // ASM 7+: <init(byte[], int, boolean)
      return ClassTransform.transformingMethods(method -> {
        if (method.equals(newMethodModel)) {
          return true;
        }

        var mt = method.methodTypeSymbol();
        return method.methodName().equalsString("<init>") && mt.parameterCount() == 3 &&
          mt.parameterType(0).displayName().equals("byte[]") &&
          mt.parameterType(1).displayName().equals("int") &&
          mt.parameterType(2).displayName().equals("int");
      }, (builder, element) -> {
        if (element instanceof CodeModel codeModel) {
          builder.transformCode(codeModel, new AsmConstructorTransformer());
        } else {
          builder.accept(element);
        }
      });
    }
    return ClassTransform.ACCEPT_ALL;
  }

  private static final class AsmConstructorTransformer implements CodeTransform {

    private static final ClassDesc ILLEGAL_ARGUMENT_EXCEPTION_DESC = ClassDesc.of("java.lang.IllegalArgumentException");

    private boolean ifCmpEQSeen;
    private boolean readShortSeen;
    private boolean iaeSeen;

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      switch (element) {
        case BranchInstruction inst -> {
          this.ifCmpEQSeen |= inst.opcode() == Opcode.IF_ICMPLE;

          builder.accept(inst);
        }
        case InvokeInstruction inst -> {
          var matchingVirtual = inst.opcode() == Opcode.INVOKEVIRTUAL;
          var matchingOwner = inst.owner().asSymbol().displayName().equals("ClassReader");
          var matchingMethod = inst.method().name().equalsString("readShort");
          this.readShortSeen |= matchingVirtual && matchingOwner && matchingMethod;

          builder.accept(inst);
        }
        case NewObjectInstruction inst -> {
          var matchingExceptionType = inst.className().asSymbol().equals(ILLEGAL_ARGUMENT_EXCEPTION_DESC);
          this.iaeSeen |= inst.opcode() == Opcode.NEW && matchingExceptionType;

          builder.accept(inst);
        }
        case ThrowInstruction inst -> {
          if (this.ifCmpEQSeen && this.readShortSeen && this.iaeSeen) {
            this.ifCmpEQSeen = false;
            this.readShortSeen = false;
            this.iaeSeen = false;
            builder.pop();
          } else {
            builder.accept(inst);
          }
        }
        default -> builder.accept(element);
      }
    }
  }

  private boolean checkNewAsmConstructorSignature(@NonNull MethodModel methodModel) {
    var mt = methodModel.methodTypeSymbol();
    return methodModel.methodName().equalsString("<init>") && mt.parameterCount() == 3 &&
      mt.parameterType(0).displayName().equals("byte[]") &&
      mt.parameterType(1).displayName().equals("int") &&
      mt.parameterType(2).displayName().equals("boolean");
  }

  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    return internalClassName.endsWith("ClassReader") ? TransformWillingness.ACCEPT : TransformWillingness.REJECT;
  }
}
