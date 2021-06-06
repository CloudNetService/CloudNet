/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.permission;

/**
 * A response, if a permissible has to check his permission that contains and allow and element
 */
public enum PermissionCheckResult {

  /**
   * The permissible has the following permission or the privileges to has the permission
   */
  ALLOWED(true),

  /**
   * The following permission is not defined or the potency from permissible or from the defined permission is not high
   * enough
   */
  DENIED(false),

  /**
   * The following permission is set on permissible, but the potency is set to -1 and doesn't allow to has the
   * permission
   */
  FORBIDDEN(false);

  private final boolean value;

  PermissionCheckResult(boolean value) {
    this.value = value;
  }

  public static PermissionCheckResult fromBoolean(Boolean result) {
    return result == null ? DENIED : result ? ALLOWED : FORBIDDEN;
  }

  public static PermissionCheckResult fromPermission(Permission permission) {
    return fromBoolean(permission == null ? null : permission.getPotency() >= 0);
  }

  /**
   * Returns the result as boolean
   *
   * @return the result as boolean
   */
  public boolean asBoolean() {
    return this.value;
  }
}
