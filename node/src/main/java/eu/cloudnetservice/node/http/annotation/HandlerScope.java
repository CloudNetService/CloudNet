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

package eu.cloudnetservice.node.http.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Enforces a scope for the annotated http handler. If this annotation is present on class and method level, then the
 * annotation on method level is used.
 * <p>
 * This annotation must be used in combination with {@code @BasicAuth} or {@code @BearerAuth} to have any effect.
 *
 * @since 4.0
 */
@Documented
@ApiStatus.Experimental
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface HandlerScope {

  /**
   * The scopes that are enforced on the http handler annotated with this annotation. The caller of this handler must
   * have at least one of the given scopes in order to successfully call this handler.
   * <p>
   * Note: All supplied scopes have to follow the scope pattern described in
   * {@link eu.cloudnetservice.node.http.RestUserManagement#SCOPE_NAMING_REGEX}.
   *
   * @return the scopes that are enforced on the http handler annotated with this annotation.
   */
  @NonNull String[] value();
}
