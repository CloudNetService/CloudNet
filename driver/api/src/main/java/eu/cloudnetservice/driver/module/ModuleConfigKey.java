/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.module;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufable;
import eu.cloudnetservice.driver.service.ServiceTask;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;

/**
 * A key for a configuration. It can either point to one specific configuration or to a set of configurations.
 *
 * @since 4.0
 */
public final class ModuleConfigKey implements DataBufable {

  /**
   * Suffix to indicate that a config id is a composite id.
   */
  public static final String COMPOSITE_ID_SUFFIX = "*";
  /**
   * Pattern to validate a configuration id.
   */
  public static final Pattern CONFIG_ID_PATTERN = ServiceTask.NAMING_PATTERN;

  // impl note: fields are monotonically non-null, must get initialized using init(String,String)
  private String moduleId;
  private String configId;
  private boolean isCompositePrefix;

  /**
   * Private constructor to construct a module config key. For internal use only.
   */
  // impl note: this constructor is also used for deserializing this class from a data buffer
  @ApiStatus.Internal
  private ModuleConfigKey() {
  }

  /**
   * Constructs a new configuration key targeting the given module using the given configuration id. If the given
   * configuration id ends with a composite id prefix, the returned config will be a composite key that can be targeted
   * to one specific configuration if needed.
   *
   * @param moduleId the id of the module to which the config key should belong.
   * @param configId the config id that should be targeted by this config key.
   * @return a new config key based on the given module id and config id.
   * @throws NullPointerException     if the given module id or config id is null.
   * @throws IllegalArgumentException if the given module id or config id does not match the naming requirements.
   */
  public static @NonNull ModuleConfigKey of(@NonNull String moduleId, @NonNull String configId) {
    var id = new ModuleConfigKey();
    id.init(moduleId, configId);
    return id;
  }

  /**
   * Inits this config id with the given module id and config id. For internal use only.
   *
   * @param moduleId the module id to use in this config key.
   * @param configId the config id to use in this config key.
   * @throws NullPointerException     if the given module id or config id is null.
   * @throws IllegalStateException    if this config key is already initialized.
   * @throws IllegalArgumentException if the given module or config id does not match the naming requirements.
   */
  @ApiStatus.Internal
  private void init(@NonNull String moduleId, @NonNull String configId) {
    Preconditions.checkState(this.moduleId == null && this.configId == null, "Id is already initialized");
    Preconditions.checkArgument(!moduleId.isBlank(), "Module id must not be empty");

    // update config id in case it is a composite id
    if (configId.endsWith(COMPOSITE_ID_SUFFIX)) {
      this.isCompositePrefix = true;
      configId = configId.substring(0, configId.length() - COMPOSITE_ID_SUFFIX.length());
    }

    // validate the config id
    var matcher = CONFIG_ID_PATTERN.matcher(configId);
    Preconditions.checkArgument(
      matcher.matches(),
      "Config id \"%s\" must match pattern \"%s\"",
      configId, ServiceTask.NAMING_REGEX);

    // init field after checking all preconditions to disallow creation of invalid module ids
    this.moduleId = moduleId;
    this.configId = configId;
  }

  /**
   * Returns a new config key that uses the given target module id instead of the current one.
   *
   * @param moduleId the module id to use in the new config key.
   * @return a new config key using the given target module id.
   * @throws NullPointerException     if the given module id is null.
   * @throws IllegalArgumentException if the given module id does not match the naming requirements.
   */
  @CheckReturnValue
  public @NonNull ModuleConfigKey withModuleId(@NonNull String moduleId) {
    var id = new ModuleConfigKey();
    id.init(moduleId, this.configId);
    id.isCompositePrefix = this.isCompositePrefix;
    return id;
  }

  /**
   * Returns a new config key that uses the given target config id instead of the current one.
   *
   * @param configId the config id to use in the new config key.
   * @return a new config key using the given target config id.
   * @throws NullPointerException     if the given config id is null.
   * @throws IllegalArgumentException if the given config id does not match the naming requirements.
   */
  @CheckReturnValue
  public @NonNull ModuleConfigKey withConfigId(@NonNull String configId) {
    return ModuleConfigKey.of(this.moduleId, configId);
  }

  /**
   * Returns a new config key with the given suffix added to the current config id. This can be used to target a
   * composite key to a single configuration.
   *
   * @param configIdSuffix the suffix to use for the current config id.
   * @return a new config key with the current config id suffixed with the given id suffix.
   * @throws NullPointerException     if the given config id suffix is null.
   * @throws IllegalArgumentException if this is not a composite config key or the given config id is invalid.
   */
  @CheckReturnValue
  public @NonNull ModuleConfigKey withConfigIdSuffix(@NonNull String configIdSuffix) {
    Preconditions.checkArgument(this.isCompositePrefix, "Cannot add suffix to non-composite config key");
    var configId = this.configId + configIdSuffix;
    return ModuleConfigKey.of(this.moduleId, configId);
  }

  /**
   * Checks is this config key is a parent of the given other config key. That is if the module id of this and the other
   * key is equal and the other config id equals this config id (for specific keys) or the other config id starts with
   * this config id (for composite keys).
   *
   * @param other the possible child of this config key to check.
   * @return true if this config key is a parent of the given other key, false otherwise.
   * @throws NullPointerException if the given other key is null.
   */
  public boolean parentOf(@NonNull ModuleConfigKey other) {
    if (!this.moduleId.equals(other.moduleId)) {
      return false;
    }

    return this.isCompositePrefix
      ? this.configId.startsWith(other.configId)
      : this.configId.equals(other.configId);
  }

  /**
   * Checks if this config key is a child of the given other config key.
   *
   * @param other the possible parent of this config key to check.
   * @return true if this key is a child of the given other key, false otherwise.
   * @throws NullPointerException if the given other key is null.
   */
  public boolean childOf(@NonNull ModuleConfigKey other) {
    return other.parentOf(this);
  }

  /**
   * Get the module id that this config key is targeting.
   *
   * @return the module id that this config key is targeting.
   */
  public @NonNull String moduleId() {
    return this.moduleId;
  }

  /**
   * Get the config id that is targeted by this config key.
   *
   * @return the config id that is targeted by this config key.
   */
  public @NonNull String configId() {
    return this.configId;
  }

  /**
   * Get if this config key is targeting a composite configuration or a single, specific configuration.
   *
   * @return true if this key is targeting a composite configuration, false otherwise.
   */
  public boolean compositeKey() {
    return this.isCompositePrefix;
  }

  /**
   * Joins the module id and the config id of this config key with the composite id suffix.
   * <table>
   *   <caption><b>Example join operations with different key types</b></caption>
   *   <tr>
   *     <th>Module Id</th>
   *     <th>Config Id</th>
   *     <th>Composite</th>
   *     <th>Delimiter</th>
   *     <th>Result</th>
   *   </tr>
   *   <tr>
   *     <td>module_id</td>
   *     <td>config_id</td>
   *     <td>false</td>
   *     <td>__</td>
   *     <td>module_id__config_id</td>
   *   </tr>
   *   <tr>
   *     <td>module_id</td>
   *     <td>config_id_</td>
   *     <td>true</td>
   *     <td>-_-</td>
   *     <td>module_id-_-config_id_*</td>
   *   </tr>
   * </table>
   *
   * @param delimiter the delimiter to join the module id and the config id with.
   * @return the joined module id and config id, with the composite id suffix.
   * @throws NullPointerException if the given delimiter is null.
   */
  public @NonNull String join(@NonNull String delimiter) {
    var joinedId = this.joinWithoutCompositeSuffix(delimiter);
    if (this.isCompositePrefix) {
      joinedId += COMPOSITE_ID_SUFFIX;
    }

    return joinedId;
  }

  /**
   * Joins the module id and the config id of this config key without the composite id suffix.
   * <table>
   *   <caption><b>Example join operations with different key types</b></caption>
   *   <tr>
   *     <th>Module Id</th>
   *     <th>Config Id</th>
   *     <th>Composite</th>
   *     <th>Delimiter</th>
   *     <th>Result</th>
   *   </tr>
   *   <tr>
   *     <td>module_id</td>
   *     <td>config_id</td>
   *     <td>false</td>
   *     <td>__</td>
   *     <td>module_id__config_id</td>
   *   </tr>
   *   <tr>
   *     <td>module_id</td>
   *     <td>config_id_</td>
   *     <td>true</td>
   *     <td>-_-</td>
   *     <td>module_id-_-config_id_</td>
   *   </tr>
   * </table>
   *
   * @param delimiter the delimiter to join the module id and the config id with.
   * @return the joined module id and config id, without the composite id suffix.
   * @throws NullPointerException if the given delimiter is null.
   */
  public @NonNull String joinWithoutCompositeSuffix(@NonNull String delimiter) {
    return this.moduleId + delimiter + this.configId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeData(@NonNull DataBuf.Mutable dataBuf) {
    dataBuf.writeString(this.moduleId);
    dataBuf.writeString(this.configId);
    dataBuf.writeBoolean(this.isCompositePrefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readData(@NonNull DataBuf dataBuf) {
    var moduleId = dataBuf.readString();
    var configId = dataBuf.readString();
    this.isCompositePrefix = dataBuf.readBoolean();
    this.init(moduleId, configId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return String.format(
      "ModuleConfigKey[moduleId=%s, configId=%s, composite=%s]",
      this.moduleId, this.configId, this.isCompositePrefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.moduleId, this.configId, this.isCompositePrefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof ModuleConfigKey that)) {
      return false;
    }

    return this.isCompositePrefix == that.isCompositePrefix
      && Objects.equals(this.moduleId, that.moduleId)
      && Objects.equals(this.configId, that.configId);
  }
}
