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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit.entity;

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.impl.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.impl.platform.bukkit.util.ReflectionUtil;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

public class EntityBukkitPlatformSelectorEntity extends BukkitPlatformSelectorEntity {

  protected static final float ARMOR_STAND_HEIGHT = 0.9875f;
  protected static final Function<LivingEntity, Double> ENTITY_HEIGHT_GETTER;
  protected static final Function<LivingEntity, Double> FALLBACK_HEIGHT_GETTER =
    entity -> entity.getEyeHeight() - ARMOR_STAND_HEIGHT + 0.45;

  protected static final Consumer<LivingEntity> ENTITY_SILENT_NO_AI;

  protected static final PotionEffect GLOWING_EFFECT;

  static {
    var modernAiSetter = Reflexion.on(LivingEntity.class)
      .findMethod("setAI", boolean.class)
      .orElse(null);
    var modernSilentSetter = Reflexion.on(Entity.class)
      .findMethod("setSilent", boolean.class)
      .orElse(null);

    if (modernAiSetter != null && modernSilentSetter != null) {
      ENTITY_SILENT_NO_AI = entity -> {
        modernAiSetter.invoke(entity, false);
        modernSilentSetter.invoke(entity, true);
      };
    } else {
      var nbt = ReflectionUtil.findNmsClass("nbt.NBTTagCompound", "NBTTagCompound");
      var load = ReflectionUtil.findMethod(LazyLegacyNmsReflection.ENTITY, new Class[]{nbt}, "g", "f", "load");
      var save = ReflectionUtil.findMethod(LazyLegacyNmsReflection.ENTITY, new Class[]{nbt}, "e", "save");
      var set = ReflectionUtil.findMethod(nbt, new Class[]{String.class, int.class}, "setInt", "a");
      var newNbt = ReflectionUtil.findConstructor(nbt);

      ENTITY_SILENT_NO_AI = entity -> {
        if (LazyLegacyNmsReflection.GET_HANDLE != null) {
          var compound = newNbt.invoke().getOrElse(null);
          var nmsEntity = LazyLegacyNmsReflection.GET_HANDLE.invoke(entity).getOrElse(null);
          if (compound != null && nmsEntity != null) {
            save.invoke(nmsEntity, compound);
            set.invoke(compound, "NoAI", 1);
            set.invoke(compound, "Silent", 1);
            load.invoke(nmsEntity, compound);
          }
        }
      };
    }

    ENTITY_HEIGHT_GETTER = Reflexion.on(Entity.class).findMethod("getHeight")
      // use the modern "getHeight" method (1.11+) to get the entity height
      .map(accessor -> (Function<LivingEntity, Double>) entity -> accessor.<Double>invoke(entity)
        .map(height -> height - ARMOR_STAND_HEIGHT)
        .getOrElse(FALLBACK_HEIGHT_GETTER.apply(entity)))
      // height method is not present, fall back to use the "height" field which is present in 1.8 - 1.10
      .orElseGet(() -> {
        var lengthFieldAccessor = Reflexion.on(LazyLegacyNmsReflection.ENTITY).findField("length");
        return entity -> lengthFieldAccessor
          .map(accessor -> LazyLegacyNmsReflection.GET_HANDLE.invoke(entity)
            .flatMap(accessor::<Float>getValue)
            .map(height -> height - ARMOR_STAND_HEIGHT)
            .map(Float::doubleValue)
            .getOrElse(FALLBACK_HEIGHT_GETTER.apply(entity)))
          .orElse(FALLBACK_HEIGHT_GETTER.apply(entity));
      });

    GLOWING_EFFECT = Reflexion.on(PotionEffectType.class)
      .findField("GLOWING")
      .map(acc -> acc.<PotionEffectType>getValue().getOrElse(null))
      .map(glowingEffect -> new PotionEffect(glowingEffect, Integer.MAX_VALUE, 1, false, false))
      .orElse(null);
  }

  protected double entityHeight;
  protected volatile LivingEntity entity;

  public EntityBukkitPlatformSelectorEntity(
    @NonNull NPC npc,
    @NonNull Plugin plugin,
    @NonNull Server server,
    @NonNull BukkitScheduler scheduler,
    @NonNull PlayerManager playerManager,
    @NonNull BukkitPlatformNPCManagement npcManagement
  ) {
    super(npc, plugin, server, scheduler, playerManager, npcManagement);
  }

  @Override
  public int entityId() {
    return this.entity == null ? -1 : this.entity.getEntityId();
  }

  @Override
  public @NonNull String scoreboardRepresentation() {
    return this.entity.getUniqueId().toString();
  }

  @Override
  public boolean spawned() {
    return this.entity != null;
  }

  @Override
  protected void spawn0() {
    var type = EntityType.valueOf(this.npc.entityType());
    if (!type.isAlive()) {
      return;
    }
    // spawn the entity
    this.entity = (LivingEntity) this.npcLocation.getWorld().spawnEntity(this.npcLocation, type);
    // clear the inventory - some entities spawn with items
    this.entity.getEquipment().clear();
    this.entityHeight = ENTITY_HEIGHT_GETTER.apply(this.entity);
    this.entity.setRemoveWhenFarAway(false);
    this.entity.setFireTicks(0);
    this.entity.setCustomNameVisible(false);

    // some entities can age, we want to spawn adults only
    if (this.entity instanceof Ageable ageable) {
      ageable.setAdult();
    }

    // apply inventory items
    for (var entry : this.npc.items().entrySet()) {
      var item = new ItemStack(Material.matchMaterial(entry.getValue()));
      switch (entry.getKey()) {
        case 0 -> this.entity.getEquipment().setItemInHand(item);
        // cannot set offhand item - skip index
        case 2 -> this.entity.getEquipment().setBoots(item);
        case 3 -> this.entity.getEquipment().setLeggings(item);
        case 4 -> this.entity.getEquipment().setChestplate(item);
        case 5 -> this.entity.getEquipment().setHelmet(item);
        default -> {
        }
      }
    }

    ENTITY_SILENT_NO_AI.accept(this.entity);
  }

  @Override
  protected void remove0() {
    if (this.entity != null) {
      this.entity.remove();
      this.entity = null;
    }
  }

  @Override
  protected void addGlowingEffect() {
    if (GLOWING_EFFECT != null) {
      this.entity.addPotionEffect(GLOWING_EFFECT);
    }
  }

  @Override
  protected double heightAddition(int lineNumber) {
    var initialAddition = super.heightAddition(lineNumber);
    return this.entityHeight + initialAddition;
  }

  private static final class LazyLegacyNmsReflection {

    static final Class<?> ENTITY = ReflectionUtil.findNmsClass("world.entity.Entity", "Entity");
    static final Class<?> CRAFT_ENTITY = ReflectionUtil.findCraftBukkitClass("entity.CraftEntity");

    static final MethodAccessor<?> GET_HANDLE = ReflectionUtil.findMethod(CRAFT_ENTITY, "getHandle");
  }
}
