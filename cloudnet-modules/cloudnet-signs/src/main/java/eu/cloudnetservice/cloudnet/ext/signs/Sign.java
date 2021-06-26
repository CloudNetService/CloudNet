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

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.util.PriorityUtil;
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
public class Sign implements SerializableObject, Comparable<Sign> {

  protected String targetGroup;
  protected String createdGroup;
  protected String templatePath;

  @EqualsAndHashCode.Include
  protected WorldPosition worldPosition;

  protected transient AtomicReference<ServiceInfoSnapshot> currentTarget;

  public Sign() {
  }

  /**
   * Creates a new sign object
   *
   * @param targetGroup   the group the sign is targeting
   * @param createdGroup  the group the sign was created on
   * @param worldPosition the position of the sign in the world
   */
  public Sign(String targetGroup, String createdGroup, WorldPosition worldPosition) {
    this(targetGroup, createdGroup, null, worldPosition);
  }

  /**
   * Creates a new sign object
   *
   * @param targetGroup   the group the sign is targeting
   * @param createdGroup  the group the sign was created on
   * @param templatePath  the template of this
   * @param worldPosition the position of the sign in the world
   */
  public Sign(String targetGroup, String createdGroup, String templatePath, WorldPosition worldPosition) {
    this.targetGroup = targetGroup;
    this.createdGroup = createdGroup;
    this.templatePath = templatePath;
    this.worldPosition = worldPosition;
  }

  public String getTargetGroup() {
    return targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public String getCreatedGroup() {
    return createdGroup;
  }

  public void setCreatedGroup(String createdGroup) {
    this.createdGroup = createdGroup;
  }

  public String getTemplatePath() {
    return templatePath;
  }

  public void setTemplatePath(String templatePath) {
    this.templatePath = templatePath;
  }

  public WorldPosition getWorldPosition() {
    return worldPosition;
  }

  public void setWorldPosition(WorldPosition worldPosition) {
    this.worldPosition = worldPosition;
  }

  public ServiceInfoSnapshot getCurrentTarget() {
    return this.currentTarget == null ? null : this.currentTarget.get();
  }

  public void setCurrentTarget(ServiceInfoSnapshot currentTarget) {
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
    return getPriority(false);
  }

  /**
   * Get the priority of the sign to be on the sign wall
   *
   * @param entry the signs configuration entry to get additional configuration from
   * @return the priority of the sign to be on the sign wall
   */
  public int getPriority(@Nullable SignConfigurationEntry entry) {
    return getPriority(entry != null && entry.isSwitchToSearchingWhenServiceIsFull());
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
    return target == null ? 0 : PriorityUtil.getPriority(target);
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.targetGroup);
    buffer.writeString(this.createdGroup);
    buffer.writeOptionalString(this.templatePath);
    buffer.writeObject(this.worldPosition);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.targetGroup = buffer.readString();
    this.createdGroup = buffer.readString();
    this.templatePath = buffer.readOptionalString();
    this.worldPosition = buffer.readObject(WorldPosition.class);
  }

  @Override
  public int compareTo(@NotNull Sign sign) {
    return Integer.compare(this.getPriority(), sign.getPriority());
  }
}
