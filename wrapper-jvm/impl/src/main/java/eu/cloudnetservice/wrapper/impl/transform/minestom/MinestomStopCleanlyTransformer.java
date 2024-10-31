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

package eu.cloudnetservice.wrapper.impl.transform.minestom;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer for the {@code ServerProcessImpl} class in Minestom which inserts a call to {@code System.exit} before
 * the last return statement in the {@code stop} method to ensure a clean shutdown of the wrapper process.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class MinestomStopCleanlyTransformer implements ClassTransformer {

  private static final String MN_SYSTEM_EXIT = "exit";
  private static final String MN_SERVER_PROCESS_STOP = "stop";
  private static final ClassDesc CD_SYSTEM = ClassDesc.of(System.class.getName());
  private static final String CNI_MINECRAFT_SERVER = "net/minestom/server/ServerProcessImpl";
  private static final MethodTypeDesc MTD_EXIT = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   *
   * @throws UnsupportedOperationException if this transformer is explicitly disabled.
   */
  public MinestomStopCleanlyTransformer() {
    var transformerDisabled = Boolean.getBoolean("cloudnet.wrapper.minestom-stop-transform-disabled");
    if (transformerDisabled) {
      throw new UnsupportedOperationException("transformer disabled via system property");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    var codeTransform = CodeTransform.ofStateful(ServerProcessImplStopCodeTransform::new);
    return ClassTransform.transformingMethodBodies(
      mm -> mm.methodName().equalsString(MN_SERVER_PROCESS_STOP),
      codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isServerProcessImpl = internalClassName.equals(CNI_MINECRAFT_SERVER);
    return isServerProcessImpl ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }

  /**
   * A transform that resolves the last return statement in a method and inserts a call to {@code System.exit} before.
   *
   * @since 4.0
   */
  private static final class ServerProcessImplStopCodeTransform implements CodeTransform {

    // Holds the resolved last return instruction in the method which will be
    // resolved once before the method transformation actually begins. This
    // instruction should never be null as each method must have a return instruction
    private CodeElement lastReturnElement;

    /**
     * {@inheritDoc}
     */
    @Override
    public void atStart(@NonNull CodeBuilder builder) {
      // Resolves the "original" code of the method, which must always be present as we're transforming
      // (clearly stated in the javadoc). Therefore, the thrown exception should never occur.
      var codeModel = builder.original().orElseThrow(() -> new IllegalStateException("original method code unknown"));

      // find & assign the last return instructions of the method
      // by iterating through a reversed view of the element list
      var reversedElements = codeModel.elementList().reversed();
      for (var element : reversedElements) {
        if (element instanceof ReturnInstruction) {
          this.lastReturnElement = element;
          return;
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull CodeBuilder builder, @NonNull CodeElement element) {
      if (element == this.lastReturnElement) {
        // current instruction will be the last return of the stop method, insert System.exit
        // to force a clean stop of the wrapper at this point
        builder.iconst_0().invokestatic(CD_SYSTEM, MN_SYSTEM_EXIT, MTD_EXIT);
      }
      builder.accept(element);
    }
  }
}
