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

package eu.cloudnetservice.driver.module.condition.processors;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.module.condition.ConditionContext;
import eu.cloudnetservice.driver.module.condition.ConditionProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A condition processor for the {@code @ConditionalOnEnv} annotation.
 *
 * @since 4.0
 */
@Singleton
@ApiStatus.Internal
public final class ConditionalOnEnvProcessor implements ConditionProcessor {

  private final DriverEnvironment currentEnvironment;

  /**
   * Constructs a new instance of this processor.
   *
   * @param componentInfo the information about the current running component.
   * @throws NullPointerException if the given component info is null.
   */
  @Inject
  public ConditionalOnEnvProcessor(@NonNull ComponentInfo componentInfo) {
    this.currentEnvironment = componentInfo.environment();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matches(@NonNull ConditionContext context, @NonNull Annotation matchedAnnotation) {
    return matchedAnnotation.elements()
      .stream()
      // get the value array
      .filter(element -> element.name().equalsString("value"))
      .map(element -> element.value() instanceof AnnotationValue.OfArray ofArray ? ofArray : null)
      .filter(Objects::nonNull)
      // extract the string values
      .flatMap(arrayValue -> arrayValue.values().stream())
      .map(singleValue -> singleValue instanceof AnnotationValue.OfString ofString ? ofString : null)
      .filter(Objects::nonNull)
      .map(AnnotationValue.OfString::stringValue)
      // check if there is a match
      .anyMatch(this.currentEnvironment.name()::equals);
  }
}
