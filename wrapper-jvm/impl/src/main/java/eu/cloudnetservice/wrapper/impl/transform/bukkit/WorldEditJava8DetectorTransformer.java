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
 * A transformer implementation that disables warnings produced by WorldEdit on older versions if the java version is
 * not Java 8.
 */
@ApiStatus.Internal
public final class WorldEditJava8DetectorTransformer implements ClassTransformer {

  private static final String CN_JAVA_8_DETECTOR = "com/sk89q/worldedit/util/Java8Detector";
  private static final String MN_NOTIFY_IF_NOT_8 = "notifyIfNot8";

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public WorldEditJava8DetectorTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    CodeTransform codeTransform = (builder, _) -> builder.return_();
    return ClassTransform.transformingMethodBodies(
      mm -> mm.methodName().equalsString(MN_NOTIFY_IF_NOT_8),
      codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isJava8Detector = internalClassName.equals(CN_JAVA_8_DETECTOR);
    return isJava8Detector ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }
}
