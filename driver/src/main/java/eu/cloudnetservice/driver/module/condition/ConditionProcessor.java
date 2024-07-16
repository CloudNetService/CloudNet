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

package eu.cloudnetservice.driver.module.condition;

import java.lang.classfile.Annotation;
import lombok.NonNull;

/**
 * A single condition that must match in order for a method to be kept in a class. Conditions are checked before loading
 * of the target class actually happens, therefore they have limited access to the class itself.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface ConditionProcessor {

  /**
   * Determines if the condition represented by this class matches. If the condition does not match, the target method
   * is erased from the class.
   *
   * @param context           the context for the current condition evaluation.
   * @param matchedAnnotation the annotation that was matched to this condition processor.
   * @return true if the condition matches and the method should be kept and executed, false otherwise.
   * @throws NullPointerException if the given context or matched annotation is null.
   */
  boolean matches(@NonNull ConditionContext context, @NonNull Annotation matchedAnnotation);
}
