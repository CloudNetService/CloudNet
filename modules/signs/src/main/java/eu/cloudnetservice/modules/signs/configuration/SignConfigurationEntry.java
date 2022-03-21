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

package eu.cloudnetservice.modules.signs.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record SignConfigurationEntry(
  @NonNull String targetGroup,
  boolean switchToSearchingWhenServiceIsFull,
  @NonNull eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry.KnockbackConfiguration knockbackConfiguration,
  @NonNull List<SignGroupConfiguration> groupConfigurations,
  @NonNull SignLayoutsHolder searchingLayout,
  @NonNull SignLayoutsHolder startingLayout,
  @NonNull SignLayoutsHolder emptyLayout,
  @NonNull SignLayoutsHolder onlineLayout,
  @NonNull SignLayoutsHolder fullLayout
) implements Cloneable {

  public static @NonNull SignConfigurationEntry createDefault(String targetGroup, String onlineBlockType,
    String fullBlockType, String startingBlock, String searchingBlock) {
    return new SignConfigurationEntry(
      targetGroup,
      false,
      KnockbackConfiguration.DEFAULT,
      new ArrayList<>(Collections.singleton(new SignGroupConfiguration(
        "Target_Group",
        false,
        new SignLayoutsHolder(1, new ArrayList<>(Collections.singleton(new SignLayout(
          new String[]{
            "&7Lobby &0- &7%task_id%",
            "&8[&7LOBBY&8]",
            "%online_players% / %max_players%",
            "%motd%"
          }, onlineBlockType, -1, null)
        ))), new SignLayoutsHolder(1, new ArrayList<>(Collections.singleton(new SignLayout(
        new String[]{
          "&eLobby &0- &e%task_id%",
          "&8[&eLOBBY&8]",
          "%online_players% / %max_players%",
          "%motd%"
        }, onlineBlockType, -1, "LIME")
      ))), new SignLayoutsHolder(1, new ArrayList<>(Collections.singleton(new SignLayout(
        new String[]{
          "&6Lobby &0- &6%task_id%",
          "&8[&6PRIME&8]",
          "%online_players% / %max_players%",
          "%motd%"
        }, fullBlockType, -1, "ORANGE")
      )))))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("Waiting", searchingBlock, 1),
        createLayout("Waiting", searchingBlock, 1),
        createLayout("Waiting", searchingBlock, 2),
        createLayout("Waiting", searchingBlock, 2),
        createLayout("Waiting", searchingBlock, 3),
        createLayout("Waiting", searchingBlock, 3)
      ))
    ), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("Starting", startingBlock, 1),
        createLayout("Starting", startingBlock, 1),
        createLayout("Starting", startingBlock, 2),
        createLayout("Starting", startingBlock, 2),
        createLayout("Starting", startingBlock, 3),
        createLayout("Starting", startingBlock, 3)
      ))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("&8[&7LOBBY&8]", startingBlock, 1),
        createLayout("&8[&7LOBBY&8]", startingBlock, 1),
        createLayout("&8[&7LOBBY&8]", startingBlock, 2),
        createLayout("&8[&7LOBBY&8]", startingBlock, 2),
        createLayout("&8[&7LOBBY&8]", startingBlock, 3),
        createLayout("&8[&7LOBBY&8]", startingBlock, 3)
      ))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("&8[&eLOBBY&8]", startingBlock, 1),
        createLayout("&8[&eLOBBY&8]", startingBlock, 1),
        createLayout("&8[&eLOBBY&8]", startingBlock, 2),
        createLayout("&8[&eLOBBY&8]", startingBlock, 2),
        createLayout("&8[&eLOBBY&8]", startingBlock, 3),
        createLayout("&8[&eLOBBY&8]", startingBlock, 3)
      ))), new SignLayoutsHolder(
      2,
      new ArrayList<>(Arrays.asList(
        createLayout("&8[&6&lLOBBY&8]", startingBlock, 1),
        createLayout("&8[&6&lLOBBY&8]", startingBlock, 1),
        createLayout("&8[&6&lLOBBY&8]", startingBlock, 2),
        createLayout("&8[&6&lLOBBY&8]", startingBlock, 2),
        createLayout("&8[&6&lLOBBY&8]", startingBlock, 3),
        createLayout("&8[&6&lLOBBY&8]", startingBlock, 3)
      )))
    );
  }

  private static @NonNull SignLayout createLayout(String firstLine, String block, int amount) {
    return new SignLayout(
      new String[]{
        "",
        firstLine,
        ".".repeat(amount),
        ""
      }, block, -1, null);
  }

  @Override
  public SignConfigurationEntry clone() {
    try {
      return (SignConfigurationEntry) super.clone();
    } catch (CloneNotSupportedException e) {
      return new SignConfigurationEntry(
        this.targetGroup,
        this.switchToSearchingWhenServiceIsFull,
        this.knockbackConfiguration,
        new ArrayList<>(this.groupConfigurations),
        this.searchingLayout,
        this.startingLayout,
        this.emptyLayout,
        this.onlineLayout,
        this.fullLayout
      );
    }
  }

  public record KnockbackConfiguration(
    boolean enabled,
    double distance,
    double strength,
    @Nullable String bypassPermission
  )  {

    public static final KnockbackConfiguration DEFAULT = new KnockbackConfiguration(true, 1,
      0.8, "cloudnet.signs.knockback.bypass");

    public boolean validAndEnabled() {
      return this.enabled && this.strength > 0 && this.distance > 0;
    }
  }
}
