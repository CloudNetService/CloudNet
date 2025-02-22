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
import java.lang.classfile.CodeTransform;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer implementation that disables the {@code setAccessible} method in the FAWE Config on old FAWE versions.
 * This is due to the fact that the method uses illegal reflection in the attempt to set a final field which is not
 * possible anymore on newer java versions.
 */
@ApiStatus.Internal
public final class FAWEConfigTransformer implements ClassTransformer {

  private static final String CNI_CONFIG = "com/boydti/fawe/config/Config";
  private static final String MN_SET_ACCESSIBLE = "setAccessible";

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public FAWEConfigTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    CodeTransform codeTransform = (builder, _) -> builder.return_();
    return ClassTransform.transformingMethodBodies(
      mm -> mm.methodName().equalsString(MN_SET_ACCESSIBLE),
      codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isFaweConfig = CNI_CONFIG.equals(internalClassName);
    return isFaweConfig ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }
}
