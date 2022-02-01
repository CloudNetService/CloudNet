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

package eu.cloudnetservice.cloudnet.driver.permission;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.Nameable;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

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
   * Compares the potency of this permission with the potency of the given permission.
   *
   * @param other the permission to compare this one to.
   * @return {@link Integer#compare(int, int)} with both potencies.
   * @throws NullPointerException if the given permission is null.
   */
  @Override
  public int compareTo(@NonNull Permission other) {
    return Integer.compare(Math.abs(this.potency()), Math.abs(other.potency()));
  }

  public static class Builder {

    private String name;
    private int potency = 0;
    private long timeOutMillis = 0;

    /**
     * Sets the name of this permission.
     *
     * @param name the name of the permission.
     * @return the same instance for chaining.
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
     * @return the same instance for chaining.
     */
    public @NonNull Builder potency(int potency) {
      this.potency = potency;
      return this;
    }

    /**
     * Sets the given millis as time-out millis for this permission.
     *
     * @param timeOutMillis the time-out for this permission.
     * @return the same instance for chaining.
     */
    public @NonNull Builder timeOutMillis(long timeOutMillis) {
      this.timeOutMillis = timeOutMillis;
      return this;
    }

    /**
     * Sets the time-out for this permission. The time-out is added to the current time millis {@link
     * System#currentTimeMillis()}.
     *
     * @param unit    the unit of the given time out.
     * @param timeOut the time-out for this permission.
     * @return the same instance for chaining.
     */
    public @NonNull Builder timeOutMillis(@NonNull TimeUnit unit, long timeOut) {
      return this.timeOutMillis(System.currentTimeMillis() + unit.toMillis(timeOut));
    }

    /**
     * Builds the new permission with all previously set options.
     *
     * @return the new permission.
     * @throws com.google.common.base.VerifyException if the name of the permission is missing.
     */
    public @NonNull Permission build() {
      Verify.verifyNotNull(this.name, "Missing name");

      return new Permission(this.name, this.potency, this.timeOutMillis);
    }
  }
}
