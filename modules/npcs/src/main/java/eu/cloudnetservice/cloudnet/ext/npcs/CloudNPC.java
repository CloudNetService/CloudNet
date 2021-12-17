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
  private UUID uniqueId;

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

  public CloudNPC(
    UUID uniqueId,
    String displayName,
    String infoLine,
    Set<NPCProfileProperty> profileProperties,
    WorldPosition position,
    String targetGroup,
    String itemInHand,
    boolean lookAtPlayer,
    boolean imitatePlayer
  ) {
    this.uniqueId = uniqueId;
    this.displayName = displayName;
    this.infoLine = infoLine;
    this.profileProperties = profileProperties;
    this.position = position;
    this.targetGroup = targetGroup;
    this.itemInHand = itemInHand;
    this.lookAtPlayer = lookAtPlayer;
    this.imitatePlayer = imitatePlayer;
  }

  public UUID uniqueId() {
    return this.uniqueId;
  }

  public void uniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String displayName() {
    return this.displayName;
  }

  public void displayName(String displayName) {
    this.displayName = displayName;
  }

  public String infoLine() {
    return this.infoLine;
  }

  public void infoLine(String infoLine) {
    this.infoLine = infoLine;
  }

  public Set<NPCProfileProperty> profileProperties() {
    return this.profileProperties;
  }

  public void profileProperties(Set<NPCProfileProperty> profileProperties) {
    this.profileProperties = profileProperties;
  }

  public WorldPosition position() {
    return this.position;
  }

  public void position(WorldPosition position) {
    this.position = position;
  }

  public String targetGroup() {
    return this.targetGroup;
  }

  public void targetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public String itemInHand() {
    return this.itemInHand;
  }

  public void itemInHand(String itemInHand) {
    this.itemInHand = itemInHand;
  }

  public boolean lookAtPlayer() {
    return this.lookAtPlayer;
  }

  public void lookAtPlayer(boolean lookAtPlayer) {
    this.lookAtPlayer = lookAtPlayer;
  }

  public boolean imitatePlayer() {
    return this.imitatePlayer;
  }

  public void imitatePlayer(boolean imitatePlayer) {
    this.imitatePlayer = imitatePlayer;
  }

  public NPCAction rightClickAction() {
    return this.rightClickAction;
  }

  public void rightClickAction(NPCAction rightClickAction) {
    this.rightClickAction = rightClickAction;
  }

  public NPCAction leftClickAction() {
    return this.leftClickAction;
  }

  public void leftClickAction(NPCAction leftClickAction) {
    this.leftClickAction = leftClickAction;
  }

  public record NPCProfileProperty(String name, String value, String signature) {

  }

}
