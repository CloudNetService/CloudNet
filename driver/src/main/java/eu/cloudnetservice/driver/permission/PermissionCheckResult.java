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

package eu.cloudnetservice.driver.permission;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The permission check results represents a tristate for a permission of permissible.
 * <p>
 * Possible values:
 * <ul>
 *   <li>{@link #ALLOWED} - a positive permission
 *   <li>{@link #DENIED} - a not existent permission
 *   <li>{@link #FORBIDDEN} - a negative permission
 * </ul>
 *
 * @since 4.0
 */
public enum PermissionCheckResult {

  /**
   * This check result indicates that the permissible has the permission and the potency of the permission is not
   * negative. Therefore, the permission is granted.
   */
  ALLOWED(true),

  /**
   * This check result indicates that the permissible does not have the permission. Therefore, the permission is
   * denied.
   */
  DENIED(false),

  /**
   * This check result indicates that the permissible has the permission but the potency is negative and therefore the
   * permission is forbidden.
   */
  FORBIDDEN(false);

  private final boolean value;

  /**
   * Constructs a new check result.
   *
   * @param value if the permission is present (allowed) or not.
   */
  PermissionCheckResult(boolean value) {
    this.value = value;
  }

  /**
   * Converts the nullable boolean into a permission check result. If the given boolean is null the result is {@code
   * DENIED}, if the boolean is true then {@code ALLOWED}, {@code FORBIDDEN} otherwise.
   *
   * @param value the boolean to convert
   * @return the converted check result.
   */
  public static @NonNull PermissionCheckResult fromBoolean(@Nullable Boolean value) {
    return value == null ? DENIED : value ? ALLOWED : FORBIDDEN;
  }

  /**
   * Converts the given permission into a permission check result. If the permission is null the result is {@code
   * DENIED}, a potency that is not negative results in a {@code ALLOWED}, {@code FORBIDDEN} otherwise.
   *
   * @param permission the permission to convert.
   * @return the converted check result.
   */
  public static @NonNull PermissionCheckResult fromPermission(@Nullable Permission permission) {
    return fromBoolean(permission == null ? null : permission.potency() >= 0);
  }

  /**
   * Returns the check result as a boolean indicating whether the permission should be granted or not.
   *
   * @return the result as boolean.
   */
  public boolean asBoolean() {
    return this.value;
  }
}
