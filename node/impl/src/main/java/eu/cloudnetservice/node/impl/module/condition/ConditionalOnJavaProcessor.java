/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.condition;

import com.google.common.base.Enums;
import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.node.module.condition.ConditionContext;
import eu.cloudnetservice.node.module.condition.ConditionProcessor;
import eu.cloudnetservice.node.module.condition.ConditionalOnJava;
import jakarta.inject.Singleton;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.util.Objects;
import lombok.NonNull;

/**
 * A condition processor for the {@code @ConditionalOnJava} annotation.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = ConditionProcessor.class, name = "eu.cloudnetservice.node.module.condition.ConditionalOnJava")
public final class ConditionalOnJavaProcessor implements ConditionProcessor {

  private static final JavaVersion RUNTIME_JAVA_VERSION = JavaVersion.runtimeVersion();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<? extends java.lang.annotation.Annotation> annotation() {
    return ConditionalOnJava.class;
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
      // extract the requested java versions
      .flatMap(arrayValue -> arrayValue.values().stream())
      .map(singleValue -> singleValue instanceof AnnotationValue.OfEnum ofEnum ? ofEnum : null)
      .filter(Objects::nonNull)
      .map(enumValue -> {
        var enumConstant = enumValue.constantName().stringValue();
        return Enums.getIfPresent(JavaVersion.class, enumConstant).orNull();
      })
      .filter(Objects::nonNull)
      // check if there is a match
      .anyMatch(requestedJavaVersion -> requestedJavaVersion == RUNTIME_JAVA_VERSION);
  }
}
