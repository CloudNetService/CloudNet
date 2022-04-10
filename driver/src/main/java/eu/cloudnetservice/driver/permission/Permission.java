/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.Nameable;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

/**
 * This permission record represents a permission for a permission group or user.
 *
 * @param name          the permission to represent with this record.
 * @param potency       the potency of this permission. The potency determines if a permission is added or subtracted
 *                      from other permissions with the same name. If the resulting potency is negative the permission
 *                      is not granted.
 * @param timeOutMillis the milliseconds to the absolute time at which this permission should expire. A timeout lower
 *                      than 1 indicates a permission to be permanent.
 * @see PermissionUser
 * @see PermissionGroup
 * @see PermissionManagement
 * @since 4.0
 */
public record Permission(
  @NonNull String name,
  int potency,
  long timeOutMillis
) implements Nameable, Comparable<Permission> {

  /**
   * Creates a new permission with the given name. The potency and timeOutMillis default to 0.
   *
   * @param permission the permission to create
   * @return the new permission.
   * @throws NullPointerException if the given permission is null.
   */
  public static @NonNull Permission of(@NonNull String permission) {
    return builder().name(permission).build();
  }

  /**
   * Creates a new permission builder instance.
   *
   * @return the new builder instance.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new permission builder instance and copies all values of the given permission into the new builder.
   *
   * @param permission the permission to copy from.
   * @return the new builder instance with values of the given permission.
   * @throws NullPointerException if the given permission is null.
   */
  public static @NonNull Builder builder(@NonNull Permission permission) {
    return builder()
      .name(permission.name())
      .potency(permission.potency())
      .timeOutMillis(permission.timeOutMillis());
  }

  /**
   * Compares the absolute potency of this permission with the absolute potency of the given permission.
   *
   * @param other the permission to compare this one to.
   * @return {@link Integer#compare(int, int)} with both potencies.
   * @throws NullPointerException if the given permission is null.
   */
  @Override
  public int compareTo(@NonNull Permission other) {
    return Integer.compare(Math.abs(this.potency()), Math.abs(other.potency()));
  }

  /**
   * A builder for a Permission.
   *
   * @since 4.0
   */
  public static class Builder {

    private String name;
    private int potency = 0;
    private long timeOutMillis = 0;

    /**
     * Sets the name of this permission.
     *
     * @param name the name of the permission.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the potency of this permission.
     *
     * @param potency the potency of this permission
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder potency(int potency) {
      this.potency = potency;
      return this;
    }

    /**
     * Sets the given milliseconds to the absolute time at which this permission should expire.
     *
     * @param timeOutMillis the time-out for this permission.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder timeOutMillis(long timeOutMillis) {
      this.timeOutMillis = timeOutMillis;
      return this;
    }

    /**
     * Sets the given time-out to the absolute time at which this permission should expire. The time-out is added to the
     * current time millis {@link System#currentTimeMillis()}.
     *
     * @param unit    the unit of the given time out.
     * @param timeOut the time-out for this permission.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given unit is null.
     */
    public @NonNull Builder timeOutMillis(@NonNull TimeUnit unit, long timeOut) {
      return this.timeOutMillis(System.currentTimeMillis() + unit.toMillis(timeOut));
    }

    /**
     * Builds the new permission with all previously set options.
     *
     * @return the new permission.
     * @throws NullPointerException if the name of the permission is missing.
     */
    public @NonNull Permission build() {
      Preconditions.checkNotNull(this.name, "No name given");

      return new Permission(this.name, this.potency, this.timeOutMillis);
    }
  }
}
