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

package eu.cloudnetservice.modules.npc;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class NPC implements DefaultedDocPropertyHolder {

  public static final Type COLLECTION_NPC = TypeFactory.parameterizedClass(Collection.class, NPC.class);
  private static final Type PROPERTIES = TypeFactory.parameterizedClass(Set.class, ProfileProperty.class);

  private final NPCType npcType;
  private final String targetGroup;

  private final String inventoryName;
  private final List<String> infoLines;

  private final WorldPosition location;

  private final boolean lookAtPlayer;
  private final boolean imitatePlayer;
  private final boolean usePlayerSkin;

  private final boolean showIngameServices;
  private final boolean showFullServices;

  private final boolean glowing;
  private final String glowingColor;

  private final boolean flyingWithElytra;
  private final boolean burning;

  private final String floatingItem;

  private final ClickAction leftClickAction;
  private final ClickAction rightClickAction;

  private final Map<Integer, String> items;
  private final Document properties;

  private NPC(
    @NonNull NPCType npcType,
    @NonNull String targetGroup,
    @Nullable String inventoryName,
    @NonNull List<String> infoLines,
    @NonNull WorldPosition location,
    boolean lookAtPlayer,
    boolean imitatePlayer,
    boolean usePlayerSkin,
    boolean showIngameServices,
    boolean showFullServices,
    boolean glowing,
    @NonNull String glowingColor,
    boolean flyingWithElytra,
    boolean burning,
    @Nullable String floatingItem,
    @NonNull ClickAction leftClickAction,
    @NonNull ClickAction rightClickAction,
    @NonNull Map<Integer, String> items,
    @NonNull Document properties
  ) {
    this.npcType = npcType;
    this.targetGroup = targetGroup;
    this.inventoryName = inventoryName;
    this.infoLines = infoLines;
    this.location = location;
    this.lookAtPlayer = lookAtPlayer;
    this.imitatePlayer = imitatePlayer;
    this.usePlayerSkin = usePlayerSkin;
    this.showIngameServices = showIngameServices;
    this.showFullServices = showFullServices;
    this.glowing = glowing;
    this.glowingColor = glowingColor;
    this.flyingWithElytra = flyingWithElytra;
    this.burning = burning;
    this.floatingItem = floatingItem;
    this.leftClickAction = leftClickAction;
    this.rightClickAction = rightClickAction;
    this.items = items;
    this.properties = properties;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull NPC npc) {
    var builder = builder()
      .targetGroup(npc.targetGroup())
      .inventoryName(npc.inventoryName())
      .infoLines(npc.infoLines())
      .location(npc.location())
      .lookAtPlayer(npc.lookAtPlayer())
      .imitatePlayer(npc.imitatePlayer())
      .usePlayerSkin(npc.usePlayerSkin())
      .showIngameServices(npc.showIngameServices())
      .showFullServices(npc.showFullServices())
      .glowing(npc.glowing())
      .glowingColor(npc.glowingColor())
      .flyingWithElytra(npc.flyingWithElytra())
      .burning(npc.burning())
      .floatingItem(npc.floatingItem())
      .leftClickAction(npc.leftClickAction())
      .rightClickAction(npc.rightClickAction())
      .items(npc.items());
    // copy the entity type
    if (npc.npcType() == NPCType.ENTITY) {
      builder.entityType(npc.entityType());
    } else {
      builder.profileProperties(npc.profileProperties());
    }
    return builder;
  }

  public @NonNull String entityType() {
    Preconditions.checkState(this.npcType == NPCType.ENTITY, "type must be entity to get the entity type");
    return this.properties.getString("entityType");
  }

  public @NonNull Set<ProfileProperty> profileProperties() {
    Preconditions.checkState(this.npcType == NPCType.PLAYER, "type must be player to get the profile properties");
    return this.properties.readObject("profileProperties", PROPERTIES);
  }

  public @NonNull NPCType npcType() {
    return this.npcType;
  }

  public @NonNull String targetGroup() {
    return this.targetGroup;
  }

  public @Nullable String inventoryName() {
    return this.inventoryName;
  }

  public @NonNull List<String> infoLines() {
    return this.infoLines;
  }

  public @NonNull WorldPosition location() {
    return this.location;
  }

  public boolean lookAtPlayer() {
    return this.lookAtPlayer;
  }

  public boolean imitatePlayer() {
    return this.imitatePlayer;
  }

  public boolean usePlayerSkin() {
    return this.usePlayerSkin;
  }

  public boolean showIngameServices() {
    return this.showIngameServices;
  }

  public boolean showFullServices() {
    return this.showFullServices;
  }

  public boolean glowing() {
    return this.glowing;
  }

  public @NonNull String glowingColor() {
    return this.glowingColor;
  }

  public boolean flyingWithElytra() {
    return this.flyingWithElytra;
  }

  public boolean burning() {
    return this.burning;
  }

  public @Nullable String floatingItem() {
    return this.floatingItem;
  }

  public @NonNull ClickAction leftClickAction() {
    return this.leftClickAction;
  }

  public @NonNull ClickAction rightClickAction() {
    return this.rightClickAction;
  }

  public @NonNull Map<Integer, String> items() {
    return this.items;
  }

  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
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

  public static final class Builder implements Mutable<Builder> {

    private final Document.Mutable properties = Document.newJsonDocument();

    private NPCType npcType;
    private String targetGroup;

    private String inventoryName;
    private List<String> infoLines = new ArrayList<>();

    private WorldPosition location;

    private boolean lookAtPlayer = true;
    private boolean imitatePlayer = true;
    private boolean usePlayerSkin = false;

    private boolean showIngameServices = false;
    private boolean showFullServices = false;

    private boolean glowing = false;
    private String glowingColor = "§f";

    private boolean flyingWithElytra = false;
    private boolean burning = false;

    private String floatingItem;

    private ClickAction leftClickAction = ClickAction.DIRECT_CONNECT_HIGHEST_PLAYERS;
    private ClickAction rightClickAction = ClickAction.OPEN_INVENTORY;

    private Map<Integer, String> items = new HashMap<>();

    public @NonNull Builder entityType(@NonNull String entityType) {
      this.npcType = NPCType.ENTITY;
      this.properties.append("entityType", entityType);
      return this;
    }

    public @NonNull Builder profileProperties(@NonNull Set<ProfileProperty> profileProperties) {
      this.npcType = NPCType.PLAYER;
      this.properties.append("profileProperties", profileProperties);
      return this;
    }

    public @NonNull Builder usePlayerSkin(boolean usePlayerSkin) {
      this.usePlayerSkin = usePlayerSkin;
      this.npcType = usePlayerSkin ? NPCType.PLAYER : this.npcType;
      return this;
    }

    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NonNull Builder inventoryName(@Nullable String inventoryName) {
      this.inventoryName = inventoryName;
      return this;
    }

    public @NonNull Builder infoLines(@NonNull List<String> infoLines) {
      this.infoLines = new ArrayList<>(infoLines);
      return this;
    }

    public @NonNull Builder location(@NonNull WorldPosition location) {
      this.location = location;
      return this;
    }

    public @NonNull Builder lookAtPlayer(boolean lookAtPlayer) {
      this.lookAtPlayer = lookAtPlayer;
      return this;
    }

    public @NonNull Builder imitatePlayer(boolean imitatePlayer) {
      this.imitatePlayer = imitatePlayer;
      return this;
    }

    public @NonNull Builder showIngameServices(boolean showIngameServices) {
      this.showIngameServices = showIngameServices;
      return this;
    }

    public @NonNull Builder showFullServices(boolean showFullServices) {
      this.showFullServices = showFullServices;
      return this;
    }

    public @NonNull Builder glowing(boolean glowing) {
      this.glowing = glowing;
      return this;
    }

    public @NonNull Builder glowingColor(@NonNull String glowingColor) {
      this.glowingColor = glowingColor;
      return this;
    }

    public @NonNull Builder flyingWithElytra(boolean flyingWithElytra) {
      this.flyingWithElytra = flyingWithElytra;
      return this;
    }

    public @NonNull Builder burning(boolean burning) {
      this.burning = burning;
      return this;
    }

    public @NonNull Builder floatingItem(@Nullable String floatingItem) {
      this.floatingItem = floatingItem;
      return this;
    }

    public @NonNull Builder leftClickAction(@NonNull ClickAction leftClickAction) {
      this.leftClickAction = leftClickAction;
      return this;
    }

    public @NonNull Builder rightClickAction(@NonNull ClickAction rightClickAction) {
      this.rightClickAction = rightClickAction;
      return this;
    }

    public @NonNull Builder items(@NonNull Map<Integer, String> items) {
      this.items = new HashMap<>(items);
      return this;
    }

    public @NonNull NPC build() {
      Preconditions.checkNotNull(this.npcType, "unable to determine npc type");
      Preconditions.checkNotNull(this.targetGroup, "no target group given");
      Preconditions.checkNotNull(this.location, "no location given");

      return new NPC(
        this.npcType,
        this.targetGroup,
        this.inventoryName,
        this.infoLines,
        this.location,
        this.lookAtPlayer,
        this.imitatePlayer,
        this.usePlayerSkin,
        this.showIngameServices,
        this.showFullServices,
        this.glowing,
        this.glowingColor,
        this.flyingWithElytra,
        this.burning,
        this.floatingItem,
        this.leftClickAction,
        this.rightClickAction,
        this.items,
        this.properties);
    }

    @Override
    public Document.@NonNull Mutable propertyHolder() {
      return this.properties;
    }
  }

  public record ProfileProperty(@NonNull String name, @NonNull String value, @Nullable String signature) {

  }
}
