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
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer implementation that disables the {@code stackableBuckets} method on old Paper versions. This is due to
 * the fact that the method uses illegal reflection in the attempt to set a final field which is not possible anymore on
 * newer java versions.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class PaperConfigTransformer implements ClassTransformer {

  private static final String MN_STACKABLE_BUCKETS = "stackableBuckets";
  private static final String CNI_PAPER_CONFIG = "org/github/paperspigot/PaperSpigotConfig";

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public PaperConfigTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    CodeTransform codeTransform = (codebuilder, _) -> codebuilder.return_();
    return ClassTransform.transformingMethodBodies(
      mm -> mm.methodName().equalsString(MN_STACKABLE_BUCKETS),
      codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformAcceptance checkClassAcceptance(@NonNull String internalClassName) {
    var isPaperConfig = internalClassName.equals(CNI_PAPER_CONFIG);
    return isPaperConfig ? TransformAcceptance.ACCEPT_ONCE : TransformAcceptance.REJECT;
  }
}
