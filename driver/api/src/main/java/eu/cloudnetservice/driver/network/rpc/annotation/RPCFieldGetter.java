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

package eu.cloudnetservice.driver.network.rpc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation which can be added to a field to indicate when generating a data class serializer, that a specific
 * method should be used as the getter rather than either a direct field access. The field provided in this annotation
 * <strong>MUST BE</strong> declared in the same class that is declaring the annotated field. The value returned by the
 * getter must be the value that should be written into the field on the remote side. The getter method must return the
 * same type as the field type.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RPCFieldGetter {

  /**
   * Get the name of the method (case-sensitive) to use as the getter for a specific field.
   *
   * @return the name of the getter method to use.
   */
  String value();
}
