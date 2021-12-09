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

package eu.cloudnetservice.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.util.PriorityUtil;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A sign object representation. It's used for database entries and general handling in the api.
 */
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sign implements Comparable<Sign> {

  public static final Type COLLECTION_TYPE = new TypeToken<Collection<Sign>>() {
  }.getType();

  protected final String targetGroup;
  protected final String templatePath;

  @EqualsAndHashCode.Include
  protected final WorldPosition worldPosition;

  protected transient AtomicReference<ServiceInfoSnapshot> currentTarget;

  /**
   * Creates a new sign object
   *
   * @param targetGroup   the group the sign is targeting
   * @param worldPosition the position of the sign in the world
   */
  public Sign(@NotNull String targetGroup, @NotNull WorldPosition worldPosition) {
    this(targetGroup, null, worldPosition);
  }

  /**
   * Creates a new sign object
   *
   * @param targetGroup   the group the sign is targeting
   * @param templatePath  the template of this
   * @param worldPosition the position of the sign in the world
   */
  public Sign(@NotNull String targetGroup, @Nullable String templatePath, @NotNull WorldPosition worldPosition) {
    this.targetGroup = targetGroup;
    this.templatePath = templatePath;
    this.worldPosition = worldPosition;
  }

  public @NotNull String getTargetGroup() {
    return this.targetGroup;
  }

  public @Nullable String getTemplatePath() {
    return this.templatePath;
  }

  public @NotNull WorldPosition getLocation() {
    return this.worldPosition;
  }

  public @Nullable ServiceInfoSnapshot getCurrentTarget() {
    return this.currentTarget == null ? null : this.currentTarget.get();
  }

  public void setCurrentTarget(@Nullable ServiceInfoSnapshot currentTarget) {
    if (this.currentTarget == null) {
      this.currentTarget = new AtomicReference<>(currentTarget);
    } else {
      this.currentTarget.lazySet(currentTarget);
    }
  }

  /**
   * Get the priority of the sign to be on the sign wall
   *
   * @return the priority of the sign to be on the sign wall
   */
  public int getPriority() {
    return this.getPriority(false);
  }

  /**
   * Get the priority of the sign to be on the sign wall
   *
   * @param entry the signs configuration entry to get additional configuration from
   * @return the priority of the sign to be on the sign wall
   */
  public int getPriority(@Nullable SignConfigurationEntry entry) {
    return this.getPriority(entry != null && entry.isSwitchToSearchingWhenServiceIsFull());
  }

  /**
   * Get the priority of the sign to be on the sign wall
   *
   * @param lowerFullToSearching If true the priority of a full service will be synced to the of a searching sign
   * @return the priority of the sign to be on the sign wall
   */
  public int getPriority(boolean lowerFullToSearching) {
    // check if the service has a snapshot
    ServiceInfoSnapshot target = this.getCurrentTarget();
    // no target has the lowest priority
    return target == null ? 0 : PriorityUtil.getPriority(target, lowerFullToSearching);
  }

  @Override
  public int compareTo(@NotNull Sign sign) {
    return Integer.compare(this.getPriority(), sign.getPriority());
  }
}
