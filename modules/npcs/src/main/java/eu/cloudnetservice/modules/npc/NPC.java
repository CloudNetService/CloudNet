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

package eu.cloudnetservice.modules.npc;

import com.google.common.base.Verify;
import com.google.common.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NPC extends JsonDocPropertyHolder {

  public static final Type COLLECTION_NPC = new TypeToken<Collection<NPC>>() {
  }.getType();
  private static final Type PROPERTIES = new TypeToken<Set<ProfileProperty>>() {
  }.getType();

  private final NPCType npcType;
  private final String targetGroup;

  private final String displayName;
  private final List<String> infoLines;

  private final WorldPosition location;

  private final boolean lookAtPlayer;
  private final boolean imitatePlayer;
  private final boolean usePlayerSkin;

  private final boolean glowing;
  private final String glowingColor;

  private final boolean flyingWithElytra;

  private final String floatingItem;

  private final ClickAction leftClickAction;
  private final ClickAction rightClickAction;

  private final Map<Integer, String> items;

  protected NPC(
    @NotNull NPCType npcType,
    @NotNull String targetGroup,
    @NotNull String displayName,
    @NotNull List<String> infoLines,
    @NotNull WorldPosition location,
    boolean lookAtPlayer,
    boolean imitatePlayer,
    boolean usePlayerSkin,
    boolean glowing,
    @NotNull String glowingColor,
    boolean flyingWithElytra,
    @Nullable String floatingItem,
    @NotNull ClickAction leftClickAction,
    @NotNull ClickAction rightClickAction,
    @NotNull Map<Integer, String> items,
    @NotNull JsonDocument properties
  ) {
    this.npcType = npcType;
    this.targetGroup = targetGroup;
    this.displayName = displayName;
    this.infoLines = infoLines;
    this.location = location;
    this.lookAtPlayer = lookAtPlayer;
    this.imitatePlayer = imitatePlayer;
    this.usePlayerSkin = usePlayerSkin;
    this.glowing = glowing;
    this.glowingColor = glowingColor;
    this.flyingWithElytra = flyingWithElytra;
    this.floatingItem = floatingItem;
    this.leftClickAction = leftClickAction;
    this.rightClickAction = rightClickAction;
    this.items = items;
    this.properties = properties;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull NPC npc) {
    var builder = builder()
      .targetGroup(npc.getTargetGroup())
      .displayName(npc.getDisplayName())
      .infoLines(npc.getInfoLines())
      .location(npc.getLocation())
      .lookAtPlayer(npc.isLookAtPlayer())
      .imitatePlayer(npc.isImitatePlayer())
      .usePlayerSkin(npc.isUsePlayerSkin())
      .glowing(npc.isGlowing())
      .glowingColor(npc.getGlowingColor())
      .flyingWithElytra(npc.isFlyingWithElytra())
      .floatingItem(npc.getFloatingItem())
      .leftClickAction(npc.getLeftClickAction())
      .rightClickAction(npc.getRightClickAction())
      .items(npc.getItems());
    // copy the entity type
    if (npc.getNpcType() == NPCType.ENTITY) {
      builder.entityType(npc.getEntityType());
    } else {
      builder.profileProperties(npc.getProfileProperties());
    }
    return builder;
  }

  public @NotNull String getEntityType() {
    Verify.verify(this.npcType == NPCType.ENTITY, "type must be entity to get the entity type");
    return this.properties.getString("entityType");
  }

  public @NotNull Set<ProfileProperty> getProfileProperties() {
    Verify.verify(this.npcType == NPCType.PLAYER, "type must be player to the profile properties");
    return this.properties.get("profileProperties", PROPERTIES);
  }

  public @NotNull NPCType getNpcType() {
    return this.npcType;
  }

  public @NotNull String getTargetGroup() {
    return this.targetGroup;
  }

  public @NotNull String getDisplayName() {
    return this.displayName;
  }

  public @NotNull List<String> getInfoLines() {
    return this.infoLines;
  }

  public @NotNull WorldPosition getLocation() {
    return this.location;
  }

  public boolean isLookAtPlayer() {
    return this.lookAtPlayer;
  }

  public boolean isImitatePlayer() {
    return this.imitatePlayer;
  }

  public boolean isUsePlayerSkin() {
    return this.usePlayerSkin;
  }

  public boolean isGlowing() {
    return this.glowing;
  }

  public @NotNull String getGlowingColor() {
    return this.glowingColor;
  }

  public boolean isFlyingWithElytra() {
    return this.flyingWithElytra;
  }

  public @Nullable String getFloatingItem() {
    return this.floatingItem;
  }

  public @NotNull ClickAction getLeftClickAction() {
    return this.leftClickAction;
  }

  public @NotNull ClickAction getRightClickAction() {
    return this.rightClickAction;
  }

  public @NotNull Map<Integer, String> getItems() {
    return this.items;
  }

  public enum NPCType {

    PLAYER,
    ENTITY
  }

  public enum ClickAction {

    OPEN_INVENTORY,
    DIRECT_CONNECT_RANDOM,
    DIRECT_CONNECT_LOWEST_PLAYERS,
    DIRECT_CONNECT_HIGHEST_PLAYERS,
    NOTHING
  }

  public static final class Builder {

    private NPCType npcType;
    private String targetGroup;

    private String displayName;
    private List<String> infoLines = new ArrayList<>();

    private WorldPosition location;

    private boolean lookAtPlayer = true;
    private boolean imitatePlayer = true;
    private boolean usePlayerSkin = false;

    private boolean glowing = false;
    private String glowingColor = "§f";

    private boolean flyingWithElytra = false;

    private String floatingItem;

    private ClickAction leftClickAction = ClickAction.DIRECT_CONNECT_HIGHEST_PLAYERS;
    private ClickAction rightClickAction = ClickAction.OPEN_INVENTORY;

    private Map<Integer, String> items = new HashMap<>();
    private JsonDocument properties = JsonDocument.newDocument();

    public @NotNull Builder entityType(@NotNull String entityType) {
      this.npcType = NPCType.ENTITY;
      this.properties.append("entityType", entityType);
      return this;
    }

    public @NotNull Builder profileProperties(@NotNull Set<ProfileProperty> profileProperties) {
      this.npcType = NPCType.PLAYER;
      this.properties.append("profileProperties", profileProperties);
      return this;
    }

    public @NotNull Builder usePlayerSkin(boolean usePlayerSkin) {
      this.usePlayerSkin = usePlayerSkin;
      this.npcType = usePlayerSkin ? NPCType.PLAYER : this.npcType;
      return this;
    }

    public @NotNull Builder targetGroup(@NotNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NotNull Builder displayName(@NotNull String displayName) {
      this.displayName = displayName;
      return this;
    }

    public @NotNull Builder infoLines(@NotNull List<String> infoLines) {
      this.infoLines = new ArrayList<>(infoLines);
      return this;
    }

    public @NotNull Builder location(@NotNull WorldPosition location) {
      this.location = location;
      return this;
    }

    public @NotNull Builder lookAtPlayer(boolean lookAtPlayer) {
      this.lookAtPlayer = lookAtPlayer;
      return this;
    }

    public @NotNull Builder imitatePlayer(boolean imitatePlayer) {
      this.imitatePlayer = imitatePlayer;
      return this;
    }

    public @NotNull Builder glowing(boolean glowing) {
      this.glowing = glowing;
      return this;
    }

    public @NotNull Builder glowingColor(@NotNull String glowingColor) {
      this.glowingColor = glowingColor;
      return this;
    }

    public @NotNull Builder flyingWithElytra(boolean flyingWithElytra) {
      this.flyingWithElytra = flyingWithElytra;
      return this;
    }

    public @NotNull Builder floatingItem(@Nullable String floatingItem) {
      this.floatingItem = floatingItem;
      return this;
    }

    public @NotNull Builder leftClickAction(@NotNull ClickAction leftClickAction) {
      this.leftClickAction = leftClickAction;
      return this;
    }

    public @NotNull Builder rightClickAction(@NotNull ClickAction rightClickAction) {
      this.rightClickAction = rightClickAction;
      return this;
    }

    public @NotNull Builder items(@NotNull Map<Integer, String> items) {
      this.items = new HashMap<>(items);
      return this;
    }

    public @NotNull NPC build() {
      Verify.verifyNotNull(this.npcType, "unable to determine npc type");
      Verify.verifyNotNull(this.targetGroup, "no target group given");
      Verify.verifyNotNull(this.displayName, "no display name given");
      Verify.verifyNotNull(this.location, "no location given");

      return new NPC(
        this.npcType,
        this.targetGroup,
        this.displayName,
        this.infoLines,
        this.location,
        this.lookAtPlayer,
        this.imitatePlayer,
        this.usePlayerSkin,
        this.glowing,
        this.glowingColor,
        this.flyingWithElytra,
        this.floatingItem,
        this.leftClickAction,
        this.rightClickAction,
        this.items,
        this.properties);
    }
  }

  public static class ProfileProperty {

    private final String name;
    private final String value;
    private final String signature;

    public ProfileProperty(String name, String value, String signature) {
      this.name = name;
      this.value = value;
      this.signature = signature;
    }

    public @NotNull String getName() {
      return this.name;
    }

    public @NotNull String getValue() {
      return this.value;
    }

    public @Nullable String getSignature() {
      return this.signature;
    }
  }
}
