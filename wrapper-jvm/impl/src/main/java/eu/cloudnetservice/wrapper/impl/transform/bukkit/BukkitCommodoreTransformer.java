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
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeTransform;
import java.lang.reflect.AccessFlag;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer which adds a try-catch block around the {@code Commodore.convert} method content to prevent issues with
 * older ASM versions on newer Java versions (f. ex. exceptions due to unsupported class file versions).
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class BukkitCommodoreTransformer implements ClassTransformer {

  private static final String MN_CONVERT = "convert";
  private static final String CN_COMMODORE = "Commodore";
  private static final String PNI_COMMODORE = "org/bukkit/craftbukkit/";

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public BukkitCommodoreTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    var codeTransform = CodeTransform.ofStateful(ConvertMethodTryCatchWrapperCodeTransform::new);
    return ClassTransform.transformingMethodBodies(
      mm -> {
        // the method descriptor itself changed, but it always takes the raw class byte array as the first argument
        var descriptorString = mm.methodType().stringValue();
        return descriptorString.startsWith("([B") && mm.methodName().equalsString(MN_CONVERT);
      },
      codeTransform
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isCommodore = internalClassName.startsWith(PNI_COMMODORE) && internalClassName.endsWith(CN_COMMODORE);
    return isCommodore ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }

  /**
   * A code transform for CraftBukkit {@code Commodore.transform} method that records all instructions in the method and
   * wraps them into a try-catch block at the end, just returning the class input data if there is a failure.
   *
   * @since 4.0
   */
  private static final class ConvertMethodTryCatchWrapperCodeTransform implements CodeTransform {

    private final Deque<CodeElement> methodElements = new ArrayDeque<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull CodeBuilder builder, @NonNull CodeElement element) {
      this.methodElements.add(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void atEnd(@NonNull CodeBuilder builder) {
      // get if the method we're transforming is static or not, this changed in 1.21.1
      // if the method is non-static we need to load a different slot to get the raw bytecode
      // argument that was supplied to the method
      // see https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/commits/0a7bd6c81a33cfaaa2f4d2456c6b237792f38fe6
      var methodModel = builder.original()
        .flatMap(CodeModel::parent)
        .orElseThrow(() -> new IllegalStateException("original method not preset on remap"));
      var transformMethodIsStatic = methodModel.flags().has(AccessFlag.STATIC);

      // inserts a try block using the captured instructions that are in the original method
      // inserts a no-op catch block & a return instruction after the catch block to return the raw input data
      // this is needed as sometimes there are labels generated after the last return instruction in the
      // original source code which let the code builder think that there is reachable code after the catch
      // block, which is actually not the case. generating the return statement outside the catch block
      // works around that issue (note that the labels are for some reason normalized *after* the checks
      // which would prevent this issue from happening completely...)
      builder
        .trying(
          this.methodElements::forEach,
          catchBuilder -> catchBuilder.catchingAll(_ -> {
          }))
        .aload(transformMethodIsStatic ? 0 : 1)
        .areturn();
    }
  }
}
