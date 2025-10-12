/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.transform.netty;

import eu.cloudnetservice.wrapper.impl.transform.ClassTransformer;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Transformer to disable the use of unsafe in netty when the {@code CleanerJava24Linker} (introduced in netty 4.2.3) is
 * available. This should result in a similar performance than the use of unsafe, without using the deprecated
 * memory-access methods in {@code sun.misc.Unsafe}.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class ConditionalUnsafeDisableTransform implements ClassTransformer {

  private static final String SYS_PROP_NO_UNSAFE = "io.netty.noUnsafe";
  private static final String CN_PLATFORM_DEPENDENT0 = "PlatformDependent0";
  private static final String CN_CLEANER_LINKER_JAVA24 = "CleanerJava24Linker";

  private static final String MN_EXPLICIT_NO_UNSAFE_CAUSE = "explicitNoUnsafeCause0";
  private static final MethodTypeDesc MTD_EXPLICIT_NO_UNSAFE_CAUSE = MethodTypeDesc.of(ConstantDescs.CD_Throwable);

  private static final ClassDesc CD_UNSUPPORTED_OP_EX = ClassDesc.of(UnsupportedOperationException.class.getName());
  private static final MethodTypeDesc MTD_UNSUPPORTED_OP_EX_NEW =
    MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public ConditionalUnsafeDisableTransform() {
    var explicitNoUnsafeValue = System.getProperty(SYS_PROP_NO_UNSAFE);
    if (explicitNoUnsafeValue != null) {
      throw new UnsupportedOperationException("transformer disabled as " + SYS_PROP_NO_UNSAFE + " is specified");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform(
    @NonNull ClassModel original,
    @Nullable Module module,
    @Nullable ClassLoader loader
  ) {
    // check if native access is enabled for the module, this assumes
    // that native access is always enabled for the unnamed module
    if (module != null && !module.isNativeAccessEnabled()) {
      return ClassTransform.ACCEPT_ALL;
    }

    // check if the CleanerJava24Linker class is available, in which case we want to prefer
    // this cleaner implementation over unsafe, so we disable the use of unsafe in netty. this
    // might come with some side effects for other libs. therefore, the transformer can be disabled.
    var packageName = original.thisClass().asSymbol().packageName().replace('.', '/');
    var cleanerJava24LinkerClassName = String.format("%s/%s.class", packageName, CN_CLEANER_LINKER_JAVA24);
    var cleanerLinkerResource = switch (loader) {
      case ClassLoader cl -> cl.getResource(cleanerJava24LinkerClassName);
      case null -> ClassLoader.getSystemResource(cleanerJava24LinkerClassName);
    };
    if (cleanerLinkerResource != null) {
      return (builder, element) -> {
        if (element instanceof MethodModel mm
          && mm.methodName().equalsString(MN_EXPLICIT_NO_UNSAFE_CAUSE)
          && mm.methodTypeSymbol().equals(MTD_EXPLICIT_NO_UNSAFE_CAUSE)) {
          builder.withMethodBody(mm.methodName(), mm.methodType(), mm.flags().flagsMask(), code -> code
            .new_(CD_UNSUPPORTED_OP_EX)
            .dup()
            .ldc("Unsafe auto-disabled on J24+ by CloudNet when linker-based cleaner is available")
            .invokespecial(CD_UNSUPPORTED_OP_EX, ConstantDescs.INIT_NAME, MTD_UNSUPPORTED_OP_EX_NEW)
            .areturn());
        } else {
          builder.with(element);
        }
      };
    }

    return ClassTransform.ACCEPT_ALL;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isPlatformDependent0 = internalClassName.endsWith(CN_PLATFORM_DEPENDENT0);
    return isPlatformDependent0 ? TransformWillingness.ACCEPT : TransformWillingness.REJECT;
  }
}
