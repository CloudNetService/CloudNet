/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit;

import io.vavr.CheckedFunction1;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import lombok.NonNull;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to ensure compatibility with a variety of minecraft versions.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class BukkitCompatibility {

  private static final Logger LOGGER = LoggerFactory.getLogger(BukkitCompatibility.class);

  // get the hand used in an interaction event, always returns EquipmentSlot.HAND on 1.8
  private static final CheckedFunction1<PlayerInteractEntityEvent, EquipmentSlot> GET_INTERACTION_HAND;

  static {
    var lookup = MethodHandles.publicLookup();

    // resolve a method handle to get the hand used in a player interact event (introduced in 1.9)
    CheckedFunction1<PlayerInteractEntityEvent, EquipmentSlot> getInteractionHand;
    try {
      var getHand = lookup.findVirtual(
        PlayerInteractEntityEvent.class,
        "getHand",
        MethodType.methodType(EquipmentSlot.class));
      getInteractionHand = event -> (EquipmentSlot) getHand.invokeExact(event);
      LOGGER.debug("org.bukkit.event.player.PlayerInteractEntityEvent.getHand(): available");
    } catch (Exception ex) {
      getInteractionHand = _ -> EquipmentSlot.HAND;
      LOGGER.debug("org.bukkit.event.player.PlayerInteractEntityEvent.getHand(): unavailable ({})", ex.getMessage());
    }

    GET_INTERACTION_HAND = getInteractionHand;
  }

  private BukkitCompatibility() {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves the hand that triggered the given interact event. Returns {@link EquipmentSlot#HAND} in case the hand
   * method doesn't exist or the hand couldn't be resolved.
   *
   * @param event the event to get the used hand from.
   * @return the equipment slot of the hand that triggered the given event.
   * @throws NullPointerException if the given interact event is null.
   */
  public static @NonNull EquipmentSlot usedHand(@NonNull PlayerInteractEntityEvent event) {
    try {
      return GET_INTERACTION_HAND.apply(event);
    } catch (Throwable throwable) {
      LOGGER.warn("could not resolve interaction hand from event {}", event, throwable);
      return EquipmentSlot.HAND;
    }
  }
}
