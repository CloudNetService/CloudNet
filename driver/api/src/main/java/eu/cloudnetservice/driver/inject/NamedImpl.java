/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.driver.inject;

import jakarta.inject.Named;
import java.lang.annotation.Annotation;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the {@link Named} annotation. This implementation can be used when constructing a binding for a
 * type that requires a {@link Named} annotation as qualifier.
 *
 * @param value the value of the named annotation.
 * @since 4.0
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public record NamedImpl(@NonNull String value) implements Named {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<? extends Annotation> annotationType() {
    return Named.class;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Named named && Objects.equals(this.value, named.value());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return (127 * "value".hashCode()) ^ this.value.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return "@" + Named.class.getName() + "(value=\"" + this.value + "\")";
  }
}
