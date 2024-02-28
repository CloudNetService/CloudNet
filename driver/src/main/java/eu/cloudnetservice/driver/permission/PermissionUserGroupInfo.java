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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * The permission user group info specifies the time-out of a permission group for a permission user.
 *
 * @see PermissionUser
 * @see PermissionGroup
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public class PermissionUserGroupInfo implements DefaultedDocPropertyHolder {

  private final String group;
  private final long timeOutMillis;

  private final Document properties;

  /**
   * Creates a new permission user group info, that creates a relation between a permission user and a permission
   * group.
   *
   * @param group         the name of the group.
   * @param timeOutMillis the timestamp for the timeout of the group.
   * @param properties    extra properties for the group info.
   */
  protected PermissionUserGroupInfo(@NonNull String group, long timeOutMillis, @NonNull Document properties) {
    this.group = group;
    this.timeOutMillis = timeOutMillis;
    this.properties = properties;
  }

  /**
   * Creates a new permission group info builder instance with all default values.
   *
   * @return the new builder instance.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new permission group info builder instance and copies all values from the given permission group info.
   *
   * @param info the group info to copy from.
   * @return the new builder instance.
   * @throws NullPointerException if the given info is null.
   */
  public static @NonNull Builder builder(@NonNull PermissionUserGroupInfo info) {
    return builder().group(info.group()).timeOutMillis(info.timeOutMillis()).properties(info.propertyHolder());
  }

  /**
   * Gets the name of the permission user group info.
   *
   * @return the group name.
   */
  public @NonNull String group() {
    return this.group;
  }

  /**
   * Gets the expiry timeout for this group.
   * <p>
   * The timeout is a unix timestamp, which specifies at which absolute time the given permission should expire. A
   * timeout less than 1 indicated that this group does not expire.
   *
   * @return the timeout timestamp.
   */
  public long timeOutMillis() {
    return this.timeOutMillis;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
  }

  /**
   * A builder for permission user group infos.
   *
   * @since 4.0
   */
  public static final class Builder {

    private String group;
    private long timeOutMillis = 0;
    private Document properties = Document.emptyDocument();

    /**
     * Sets the group of this group info.
     *
     * @param group the name of the group info.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group is null.
     */
    public @NonNull Builder group(@NonNull String group) {
      this.group = group;
      return this;
    }

    /**
     * Sets the given milliseconds to the absolute time at which this permission should expire.
     *
     * @param timeOutMillis the time-out for this user group.
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
     * @param timeout the time-out for this group info.
     * @param unit    the unit of the given time out.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given unit is null.
     */
    public @NonNull Builder timeOut(long timeout, @NonNull TimeUnit unit) {
      return this.timeOutMillis(System.currentTimeMillis() + unit.toMillis(timeout));
    }

    /**
     * Sets the time-out for this group info. The time-out is added to the current time millis
     * {@link System#currentTimeMillis()}.
     *
     * @param duration the duration the group lasts for.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given duration is null.
     */
    public @NonNull Builder timeOut(@NonNull Duration duration) {
      return this.timeOutMillis(System.currentTimeMillis() + duration.toMillis());
    }

    /**
     * Sets the properties of the new permission user.
     *
     * @param properties the properties for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given properties are null.
     */
    public @NonNull Builder properties(@NonNull Document properties) {
      this.properties = properties.immutableCopy();
      return this;
    }

    /**
     * Constructs the new permission user group info from this builder.
     *
     * @return the new user group info.
     * @throws NullPointerException if the group name is missing.
     */
    public @NonNull PermissionUserGroupInfo build() {
      Preconditions.checkNotNull(this.group, "Missing group");
      return new PermissionUserGroupInfo(this.group, this.timeOutMillis, this.properties);
    }
  }
}
