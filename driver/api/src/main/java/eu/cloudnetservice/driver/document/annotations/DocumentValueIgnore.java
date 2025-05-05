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

package eu.cloudnetservice.driver.document.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Annotation to indicate that the annotated field or type should be ignored during tree serialisation or
 * deserialization. While this annotation should be supported by all document type implementations, there is no
 * guarantee that this is the case.
 *
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface DocumentValueIgnore {

  /**
   * Get the serialisation direction(s) when the field should be ignored. This value defaults to both directions,
   * meaning that the field is neither serialized nor deserialized.
   *
   * @return the serialisation direction(s) when the field should be ignored.
   */
  @NonNull Direction[] value() default {Direction.SERIALIZE, Direction.DESERIALIZE};

  /**
   * Represents the possible document serialisation directions.
   *
   * @since 4.0
   */
  enum Direction {

    /**
     * Represents the serialization direction (from field/class to document).
     */
    SERIALIZE,
    /**
     * Represents the deserialization direction (from document to field/class).
     */
    DESERIALIZE
  }
}
