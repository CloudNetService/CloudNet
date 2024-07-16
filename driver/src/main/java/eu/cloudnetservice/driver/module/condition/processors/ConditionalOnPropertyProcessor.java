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

import com.google.common.base.Strings;
import eu.cloudnetservice.driver.module.condition.ConditionContext;
import eu.cloudnetservice.driver.module.condition.ConditionProcessor;
import jakarta.inject.Singleton;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A condition processor for the {@code @ConditionalOnProperty} annotation.
 *
 * @since 4.0
 */
@Singleton
@ApiStatus.Internal
public final class ConditionalOnPropertyProcessor implements ConditionProcessor {

  /**
   * Compares the given expected to the given actual value, either case-sensitive or case-insensitive based on the given
   * case-sensitive flag.
   *
   * @param expected      the value that is expected.
   * @param actual        the value that is actually provided.
   * @param caseSensitive true if the values should be compared case-sensitive, false otherwise.
   * @return true if the given actual value equals the given expected value according to the provided check flags.
   * @throws NullPointerException if the given expected or actual value is null.
   */
  private static boolean compareStrings(@NonNull String expected, @NonNull String actual, boolean caseSensitive) {
    return caseSensitive ? expected.equals(actual) : expected.equalsIgnoreCase(actual);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matches(@NonNull ConditionContext context, @NonNull Annotation matchedAnnotation) {
    // did not specify a value for the env key or the system property, so nothing to match against
    var annotationElements = matchedAnnotation.elements();
    if (annotationElements.size() <= 1) {
      return true;
    }

    // extract the values set in the annotation
    String envKey = null;
    String systemProp = null;
    String expectedValue = null;
    boolean caseSensitive = true;
    for (var element : annotationElements) {
      switch (element.value()) {
        case AnnotationValue.OfString ofString -> {
          var stringValue = ofString.stringValue();
          switch (element.name().stringValue()) {
            case "env" -> envKey = Strings.emptyToNull(stringValue);
            case "prop" -> systemProp = Strings.emptyToNull(stringValue);
            case "expected" -> expectedValue = stringValue;
          }
        }
        case AnnotationValue.OfBoolean ofBoolean
          when element.name().equalsString("caseSensitive") -> caseSensitive = ofBoolean.booleanValue();
        default -> {
          // unknown or ignored property on the annotation
        }
      }
    }

    // should usually not happen as the value is required and
    // must be given in order to successfully compile the class
    Objects.requireNonNull(expectedValue, "missing expected value from @ConditionalOnProperty");

    // check env first, as specified in the javadoc
    if (envKey != null) {
      var envValue = System.getenv(envKey);
      if (envValue != null) {
        return compareStrings(expectedValue, envValue, caseSensitive);
      }
    }

    // check system property
    if (systemProp != null) {
      var systemPropertyValue = System.getProperty(systemProp);
      if (systemPropertyValue != null) {
        return compareStrings(expectedValue, systemPropertyValue, caseSensitive);
      }
    }

    return false;
  }
}
