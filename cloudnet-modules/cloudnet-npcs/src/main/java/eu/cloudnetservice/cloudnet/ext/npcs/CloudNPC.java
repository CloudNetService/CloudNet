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

package eu.cloudnetservice.cloudnet.ext.npcs;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CloudNPC {

  @EqualsAndHashCode.Include
  private UUID uuid;

  private String displayName;

  private String infoLine;

  private Set<NPCProfileProperty> profileProperties;

  private WorldPosition position;

  private String targetGroup;

  private String itemInHand;

  private boolean lookAtPlayer;

  private boolean imitatePlayer;

  private NPCAction rightClickAction = NPCAction.OPEN_INVENTORY;

  private NPCAction leftClickAction = NPCAction.DIRECT_CONNECT_HIGHEST_PLAYERS;

  public CloudNPC() {
  }

  public CloudNPC(UUID uuid, String displayName, String infoLine, Set<NPCProfileProperty> profileProperties,
    WorldPosition position, String targetGroup, String itemInHand, boolean lookAtPlayer, boolean imitatePlayer) {
    this.uuid = uuid;
    this.displayName = displayName;
    this.infoLine = infoLine;
    this.profileProperties = profileProperties;
    this.position = position;
    this.targetGroup = targetGroup;
    this.itemInHand = itemInHand;
    this.lookAtPlayer = lookAtPlayer;
    this.imitatePlayer = imitatePlayer;
  }

  public UUID getUUID() {
    return this.uuid;
  }

  public void setUUID(UUID uuid) {
    this.uuid = uuid;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getInfoLine() {
    return this.infoLine;
  }

  public void setInfoLine(String infoLine) {
    this.infoLine = infoLine;
  }

  public Set<NPCProfileProperty> getProfileProperties() {
    return this.profileProperties;
  }

  public void setProfileProperties(Set<NPCProfileProperty> profileProperties) {
    this.profileProperties = profileProperties;
  }

  public WorldPosition getPosition() {
    return this.position;
  }

  public void setPosition(WorldPosition position) {
    this.position = position;
  }

  public String getTargetGroup() {
    return this.targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public String getItemInHand() {
    return this.itemInHand;
  }

  public void setItemInHand(String itemInHand) {
    this.itemInHand = itemInHand;
  }

  public boolean isLookAtPlayer() {
    return this.lookAtPlayer;
  }

  public void setLookAtPlayer(boolean lookAtPlayer) {
    this.lookAtPlayer = lookAtPlayer;
  }

  public boolean isImitatePlayer() {
    return this.imitatePlayer;
  }

  public void setImitatePlayer(boolean imitatePlayer) {
    this.imitatePlayer = imitatePlayer;
  }

  public NPCAction getRightClickAction() {
    return this.rightClickAction;
  }

  public void setRightClickAction(NPCAction rightClickAction) {
    this.rightClickAction = rightClickAction;
  }

  public NPCAction getLeftClickAction() {
    return this.leftClickAction;
  }

  public void setLeftClickAction(NPCAction leftClickAction) {
    this.leftClickAction = leftClickAction;
  }

  public static class NPCProfileProperty {

    private final String name;

    private final String value;

    private final String signature;

    public NPCProfileProperty(String name, String value, String signature) {
      this.name = name;
      this.value = value;
      this.signature = signature;
    }

    public String getName() {
      return this.name;
    }

    public String getValue() {
      return this.value;
    }

    public String getSignature() {
      return this.signature;
    }

  }

}
