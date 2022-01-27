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

package eu.cloudnetservice.modules.npc.platform.bukkit.entity;

import static eu.cloudnetservice.modules.npc.platform.bukkit.util.ReflectionUtil.findMethod;
import static eu.cloudnetservice.modules.npc.platform.bukkit.util.ReflectionUtil.staticFieldValue;

import com.google.errorprone.annotations.concurrent.LazyInit;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.bukkit.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import lombok.NonNull;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wither;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EntityBukkitPlatformSelectorEntity extends BukkitPlatformSelectorEntity {

  protected static final PotionEffectType GLOWING = staticFieldValue(PotionEffectType.class, "GLOWING");

  protected static final Class<?> ENTITY = ReflectionUtil.findNmsClass("world.entity.Entity", "Entity");
  protected static final Class<?> NBT = ReflectionUtil.findNmsClass("nbt.NBTTagCompound", "NBTTagCompound");
  protected static final Class<?> CRAFT_ENTITY = ReflectionUtil.findCraftBukkitClass("entity.CraftEntity");

  protected static final MethodHandle GET_HANDLE = findMethod(CRAFT_ENTITY, "getHandle");
  protected static final MethodHandle LOAD = findMethod(ENTITY, new Class[]{NBT}, "g", "f", "load");
  protected static final MethodHandle SAVE = findMethod(ENTITY, new Class[]{NBT}, "e", "save");
  protected static final MethodHandle SET = findMethod(NBT, new Class[]{String.class, int.class}, "setInt", "a");

  protected static final MethodHandle NEW_NBT = ReflectionUtil.findConstructor(NBT);

  @LazyInit
  protected static PotionEffect glowingEffect;

  protected volatile LivingEntity entity;

  public EntityBukkitPlatformSelectorEntity(
    @NonNull BukkitPlatformNPCManagement npcManagement,
    @NonNull Plugin plugin,
    @NonNull NPC npc
  ) {
    super(npcManagement, plugin, npc);
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
    this.entity.setFireTicks(0);
    this.entity.setCustomName(this.npc.displayName());
    this.entity.setCustomNameVisible(!this.npc.hideEntityName());
    // set the profession of the villager to prevent inconsistency
    if (this.entity instanceof Villager) {
      ((Villager) this.entity).setProfession(Profession.FARMER);
    }
    // uhhh nms reflection :(
    try {
      // create a new nbt tag compound
      var compound = NEW_NBT.invoke();
      // get the nms entity
      var nmsEntity = GET_HANDLE.invoke(this.entity);
      // save the entity data to the compound
      SAVE.invoke(nmsEntity, compound);
      // rewrite NoAi and Silent values
      SET.invoke(compound, "NoAI", 1);
      SET.invoke(compound, "Silent", 1);
      // load the entity data from the compound again
      LOAD.invoke(nmsEntity, compound);
    } catch (Throwable throwable) {
      throw new RuntimeException("Unable to use bleeding reflections on nms entity:", throwable);
    }
  }

  @Override
  protected void remove0() {
    this.entity.remove();
    this.entity = null;
  }

  @Override
  protected void addGlowingEffect() {
    if (GLOWING != null) {
      // init the potion effect if needed
      if (glowingEffect == null) {
        glowingEffect = new PotionEffect(GLOWING, Integer.MAX_VALUE, 1, false, false);
      }
      // apply to the entity
      this.entity.addPotionEffect(glowingEffect);
    }
  }

  @Override
  protected double heightAddition(int lineNumber) {
    var initialAddition = super.heightAddition(lineNumber);
    return (this.entity.getEyeHeight() - (this.entity instanceof Wither ? 0.4 : 0.55)) + initialAddition;
  }
}
