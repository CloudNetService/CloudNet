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

package eu.cloudnetservice.wrapper.impl.transform.netty;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Transformer to disable the use of unsafe in netty when the {@code CleanerJava24Linker} (introduced in netty 4.2.3) is
 * available. This should result in a similar performance than the use of unsafe, without using the deprecated
 * memory-access methods in unsafe.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class ConditionalUnsafeDisableTransform implements ClassTransformer {

  private static final String SYS_PROP_NO_UNSAFE = "io.netty.noUnsafe";
  private static final String CNI_PLATFORM_DEPENDENT0 = "io/netty/util/internal/PlatformDependent0";
  private static final String CNI_CLEANER_LINKER_JAVA24 = "io/netty/util/internal/CleanerJava24Linker";

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
    var cleanerLinkerResource = switch (loader) {
      case ClassLoader cl -> cl.getResource(CNI_CLEANER_LINKER_JAVA24 + ".class");
      case null -> ClassLoader.getSystemResource(CNI_CLEANER_LINKER_JAVA24 + ".class");
    };
    if (cleanerLinkerResource != null) {
      System.setProperty(SYS_PROP_NO_UNSAFE, "true");
    }

    return ClassTransform.ACCEPT_ALL;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    // need to decide only once for the entire lifetime of the jvm
    var isPlatformDependent0 = internalClassName.equals(CNI_PLATFORM_DEPENDENT0);
    return isPlatformDependent0 ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }
}
