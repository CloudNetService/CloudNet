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

import com.google.common.base.Enums;
import eu.cloudnetservice.driver.module.condition.ConditionContext;
import eu.cloudnetservice.driver.module.condition.ConditionProcessor;
import eu.cloudnetservice.driver.module.condition.ConditionalOnClass;
import jakarta.inject.Singleton;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.lang.constant.ClassDesc;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A condition processor for the {@code @ConditionalOnClass} annotation.
 *
 * @since 4.0
 */
@Singleton
@ApiStatus.Internal
public final class ConditionalOnClassProcessor implements ConditionProcessor {

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matches(@NonNull ConditionContext context, @NonNull Annotation matchedAnnotation) {
    // gets the list of ClassDec that were requested by the annotation from the Class<?>[] value() method
    var requestedClasses = matchedAnnotation.elements()
      .stream()
      .filter(element -> element.name().equalsString("value"))
      .map(element -> element.value() instanceof AnnotationValue.OfArray ofArray ? ofArray : null)
      .filter(Objects::nonNull)
      .flatMap(arrayValue -> arrayValue.values().stream())
      .map(singleValue -> singleValue instanceof AnnotationValue.OfClass ofClass ? ofClass : null)
      .filter(Objects::nonNull)
      .map(AnnotationValue.OfClass::classSymbol)
      .toList();
    // get the requested presence from the Presence presence() method
    // note: the element list does not contain the element if the default value is used, hence the additional orElse
    var requestedPresence = matchedAnnotation.elements()
      .stream()
      .filter(element -> element.name().equalsString("presence"))
      .map(element -> element.value() instanceof AnnotationValue.OfEnum ofEnum ? ofEnum : null)
      .filter(Objects::nonNull)
      .map(enumValue -> {
        var constantName = enumValue.constantName().stringValue();
        return Enums.getIfPresent(ConditionalOnClass.Presence.class, constantName).orNull();
      })
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(ConditionalOnClass.Presence.PRESENT);

    // check if each requested class matches the requested presence policy
    for (var requestedClass : requestedClasses) {
      var classIsPresent = this.checkClassPresence(requestedClass, context.moduleClassLoader());
      if ((requestedPresence == ConditionalOnClass.Presence.PRESENT && !classIsPresent)
        || (requestedPresence == ConditionalOnClass.Presence.ABSENT && classIsPresent)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the class represented by the given class descriptor is available through the given class loader.
   *
   * @param classDesc   the descriptor of the class to check for.
   * @param classLoader the class loader to use for the class presence checking.
   * @return true if the class represented by the given class descriptor is present, false otherwise.
   * @throws NullPointerException if the given class descriptor or class loader is null.
   */
  private boolean checkClassPresence(@NonNull ClassDesc classDesc, @NonNull ClassLoader classLoader) {
    // drop array information
    while (classDesc.isArray()) {
      classDesc = classDesc.componentType();
    }

    // check for primitive types, they are always loaded and present
    if (classDesc.isPrimitive()) {
      return true;
    }

    // get the binary name of the class
    var packageName = classDesc.packageName();
    var className = classDesc.displayName();
    if (packageName.isEmpty()) {
      // class is in unnamed package
      return this.checkClassPresence(className, classLoader);
    } else {
      // class is in named package
      var fullClassName = String.format("%s.%s", packageName, className);
      return this.checkClassPresence(fullClassName, classLoader);
    }
  }

  /**
   * Checks if the class with the given binary class name is available through the given class loader.
   *
   * @param className   the binary class name of the class to check for.
   * @param classLoader the class loader to use for the class presence checking.
   * @return true if the class with the given binary name is present, false otherwise.
   * @throws NullPointerException if the given class name or class loader is null.
   */
  private boolean checkClassPresence(@NonNull String className, @NonNull ClassLoader classLoader) {
    try {
      Class.forName(className, false, classLoader);
      return true;
    } catch (ClassNotFoundException | LinkageError _) {
      return false;
    }
  }
}
