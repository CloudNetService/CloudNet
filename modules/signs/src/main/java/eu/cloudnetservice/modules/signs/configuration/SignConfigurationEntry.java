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

public class SignConfigurationEntry implements Cloneable {

  protected String targetGroup;
  protected boolean switchToSearchingWhenServiceIsFull;
  protected KnockbackConfiguration knockbackConfiguration;

  protected List<SignGroupConfiguration> groupConfigurations;

  protected SignLayoutsHolder searchingLayout;
  protected SignLayoutsHolder startingLayout;
  protected SignLayoutsHolder emptyLayout;
  protected SignLayoutsHolder onlineLayout;
  protected SignLayoutsHolder fullLayout;

  public SignConfigurationEntry() {
  }

  public SignConfigurationEntry(String targetGroup, boolean switchToSearchingWhenServiceIsFull,
    KnockbackConfiguration knockbackConfiguration,
    List<SignGroupConfiguration> groupConfigurations, SignLayoutsHolder searchingLayout,
    SignLayoutsHolder startingLayout,
    SignLayoutsHolder emptyLayout, SignLayoutsHolder onlineLayout, SignLayoutsHolder fullLayout) {
    this.targetGroup = targetGroup;
    this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
    this.knockbackConfiguration = knockbackConfiguration;
    this.groupConfigurations = groupConfigurations;
    this.searchingLayout = searchingLayout;
    this.startingLayout = startingLayout;
    this.emptyLayout = emptyLayout;
    this.onlineLayout = onlineLayout;
    this.fullLayout = fullLayout;
  }

  public static @NonNull SignConfigurationEntry createDefault(String targetGroup, String onlineBlockType,
    String fullBlockType, String startingBlock, String searchingBlock) {
    return new SignConfigurationEntry(
      targetGroup,
      false,
      new KnockbackConfiguration(1, 0.8),
      new ArrayList<>(Collections.singleton(new SignGroupConfiguration(
        "Target_Group",
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

  protected static @NonNull SignLayout createLayout(String firstLine, String block, int amount) {
    return new SignLayout(
      new String[]{
        "",
        firstLine,
        ".".repeat(amount),
        ""
      }, block, -1, null);
  }

  public String targetGroup() {
    return this.targetGroup;
  }

  public void targetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public boolean switchToSearchingWhenServiceIsFull() {
    return this.switchToSearchingWhenServiceIsFull;
  }

  public void switchToSearchingWhenServiceIsFull(boolean switchToSearchingWhenServiceIsFull) {
    this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
  }

  public KnockbackConfiguration knockbackConfiguration() {
    return this.knockbackConfiguration;
  }

  public void knockbackConfiguration(KnockbackConfiguration knockbackConfiguration) {
    this.knockbackConfiguration = knockbackConfiguration;
  }

  public List<SignGroupConfiguration> groupConfigurations() {
    return this.groupConfigurations;
  }

  public void groupConfigurations(List<SignGroupConfiguration> groupConfigurations) {
    this.groupConfigurations = groupConfigurations;
  }

  public SignLayoutsHolder searchingLayout() {
    return this.searchingLayout;
  }

  public void searchingLayout(SignLayoutsHolder searchingLayout) {
    this.searchingLayout = searchingLayout;
  }

  public SignLayoutsHolder startingLayout() {
    return this.startingLayout;
  }

  public void startingLayout(SignLayoutsHolder startingLayout) {
    this.startingLayout = startingLayout;
  }

  public SignLayoutsHolder emptyLayout() {
    return this.emptyLayout;
  }

  public void emptyLayout(SignLayoutsHolder emptyLayout) {
    this.emptyLayout = emptyLayout;
  }

  public SignLayoutsHolder onlineLayout() {
    return this.onlineLayout;
  }

  public void onlineLayout(SignLayoutsHolder onlineLayout) {
    this.onlineLayout = onlineLayout;
  }

  public SignLayoutsHolder fullLayout() {
    return this.fullLayout;
  }

  public void fullLayout(SignLayoutsHolder fullLayout) {
    this.fullLayout = fullLayout;
  }

  @Override
  public SignConfigurationEntry clone() {
    try {
      var clone = (SignConfigurationEntry) super.clone();
      return clone;
    } catch (CloneNotSupportedException e) {
      return new SignConfigurationEntry(
        this.targetGroup,
        this.switchToSearchingWhenServiceIsFull,
        this.knockbackConfiguration.clone(),
        new ArrayList<>(this.groupConfigurations),
        this.searchingLayout,
        this.startingLayout,
        this.emptyLayout,
        this.onlineLayout,
        this.fullLayout
      );
    }
  }

  public static class KnockbackConfiguration implements Cloneable {

    protected static final KnockbackConfiguration DEFAULT = new KnockbackConfiguration(true, 1,
      0.8, "cloudnet.signs.knockback.bypass");
    protected static final KnockbackConfiguration DISABLED = new KnockbackConfiguration(false, 1, 0.8);

    protected boolean enabled;
    protected double distance;
    protected double strength;
    protected String bypassPermission;

    public KnockbackConfiguration() {
    }

    public KnockbackConfiguration(double distance, double strength) {
      this(true, distance, strength);
    }

    public KnockbackConfiguration(boolean enabled, double distance, double strength) {
      this(enabled, distance, strength, "cloudnet.signs.knockback.bypass");
    }

    public KnockbackConfiguration(boolean enabled, double distance, double strength, String bypassPermission) {
      this.enabled = enabled;
      this.distance = distance;
      this.strength = strength;
      this.bypassPermission = bypassPermission;
    }

    public boolean enabled() {
      return this.enabled;
    }

    public void enabled(boolean enabled) {
      this.enabled = enabled;
    }

    public double distance() {
      return this.distance;
    }

    public void distance(double distance) {
      this.distance = distance;
    }

    public double strength() {
      return this.strength;
    }

    public void strength(double strength) {
      this.strength = strength;
    }

    public String bypassPermission() {
      return this.bypassPermission;
    }

    public void bypassPermission(String bypassPermission) {
      this.bypassPermission = bypassPermission;
    }

    public boolean validAndEnabled() {
      return this.enabled && this.strength > 0 && this.distance > 0;
    }

    @Override
    public KnockbackConfiguration clone() {
      try {
        return (KnockbackConfiguration) super.clone();
      } catch (CloneNotSupportedException exception) {
        return new KnockbackConfiguration(this.enabled, this.distance, this.strength, this.bypassPermission);
      }
    }
  }
}
