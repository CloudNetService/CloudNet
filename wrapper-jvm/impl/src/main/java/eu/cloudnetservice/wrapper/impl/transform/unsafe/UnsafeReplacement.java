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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Marker annotation to indicate that the annotated method or field replaces the equivalent method or field in
 * {@code sun.misc.Unsafe}. In the case of a field only the name is matched, in the case of a method the name and
 * descriptor of the method are matched. The method descriptor is derived from the annotated method.
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@interface UnsafeReplacement {

  /**
   * Get the name of the method or field from Unsafe that is being replaced.
   *
   * @return the name of the method or field from Unsafe that is being replaced.
   */
  @NonNull
  String name();
}
