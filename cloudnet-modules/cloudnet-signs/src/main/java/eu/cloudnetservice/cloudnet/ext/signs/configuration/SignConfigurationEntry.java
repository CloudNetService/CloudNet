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

package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jdk.internal.joptsimple.internal.Strings;
import org.jetbrains.annotations.NotNull;

public class SignConfigurationEntry implements SerializableObject, Cloneable {

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

  public static @NotNull SignConfigurationEntry createDefault(String targetGroup, String onlineBlockType,
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
          }, onlineBlockType, -1)
        ))), new SignLayoutsHolder(1, new ArrayList<>(Collections.singleton(new SignLayout(
        new String[]{
          "&eLobby &0- &e%task_id%",
          "&8[&eLOBBY&8]",
          "%online_players% / %max_players%",
          "%motd%"
        }, onlineBlockType, -1)
      ))), new SignLayoutsHolder(1, new ArrayList<>(Collections.singleton(new SignLayout(
        new String[]{
          "&6Lobby &0- &6%task_id%",
          "&8[&6PRIME&8]",
          "%online_players% / %max_players%",
          "%motd%"
        }, fullBlockType, -1)
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

  protected static @NotNull SignLayout createLayout(String firstLine, String block, int amount) {
    return new SignLayout(
      new String[]{
        "",
        firstLine,
        Strings.repeat('.', amount),
        ""
      }, block, -1
    );
  }

  public String getTargetGroup() {
    return this.targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public boolean isSwitchToSearchingWhenServiceIsFull() {
    return this.switchToSearchingWhenServiceIsFull;
  }

  public void setSwitchToSearchingWhenServiceIsFull(boolean switchToSearchingWhenServiceIsFull) {
    this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
  }

  public KnockbackConfiguration getKnockbackConfiguration() {
    return this.knockbackConfiguration;
  }

  public void setKnockbackConfiguration(KnockbackConfiguration knockbackConfiguration) {
    this.knockbackConfiguration = knockbackConfiguration;
  }

  public List<SignGroupConfiguration> getGroupConfigurations() {
    return this.groupConfigurations;
  }

  public void setGroupConfigurations(List<SignGroupConfiguration> groupConfigurations) {
    this.groupConfigurations = groupConfigurations;
  }

  public SignLayoutsHolder getSearchingLayout() {
    return this.searchingLayout;
  }

  public void setSearchingLayout(SignLayoutsHolder searchingLayout) {
    this.searchingLayout = searchingLayout;
  }

  public SignLayoutsHolder getStartingLayout() {
    return this.startingLayout;
  }

  public void setStartingLayout(SignLayoutsHolder startingLayout) {
    this.startingLayout = startingLayout;
  }

  public SignLayoutsHolder getEmptyLayout() {
    return this.emptyLayout;
  }

  public void setEmptyLayout(SignLayoutsHolder emptyLayout) {
    this.emptyLayout = emptyLayout;
  }

  public SignLayoutsHolder getOnlineLayout() {
    return this.onlineLayout;
  }

  public void setOnlineLayout(SignLayoutsHolder onlineLayout) {
    this.onlineLayout = onlineLayout;
  }

  public SignLayoutsHolder getFullLayout() {
    return this.fullLayout;
  }

  public void setFullLayout(SignLayoutsHolder fullLayout) {
    this.fullLayout = fullLayout;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.targetGroup);
    buffer.writeBoolean(this.switchToSearchingWhenServiceIsFull);
    buffer.writeObject(this.knockbackConfiguration);

    buffer.writeObjectCollection(this.groupConfigurations);

    buffer.writeObject(this.searchingLayout);
    buffer.writeObject(this.startingLayout);
    buffer.writeObject(this.emptyLayout);
    buffer.writeObject(this.onlineLayout);
    buffer.writeObject(this.fullLayout);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.targetGroup = buffer.readString();
    this.switchToSearchingWhenServiceIsFull = buffer.readBoolean();
    this.knockbackConfiguration = buffer.readObject(KnockbackConfiguration.class);

    this.groupConfigurations = buffer.readObjectCollection(SignGroupConfiguration.class);

    this.searchingLayout = buffer.readObject(SignLayoutsHolder.class);
    this.startingLayout = buffer.readObject(SignLayoutsHolder.class);
    this.emptyLayout = buffer.readObject(SignLayoutsHolder.class);
    this.onlineLayout = buffer.readObject(SignLayoutsHolder.class);
    this.fullLayout = buffer.readObject(SignLayoutsHolder.class);
  }

  public static class KnockbackConfiguration implements SerializableObject, Cloneable {

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

    public boolean isEnabled() {
      return this.enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public double getDistance() {
      return this.distance;
    }

    public void setDistance(double distance) {
      this.distance = distance;
    }

    public double getStrength() {
      return this.strength;
    }

    public void setStrength(double strength) {
      this.strength = strength;
    }

    public String getBypassPermission() {
      return this.bypassPermission;
    }

    public void setBypassPermission(String bypassPermission) {
      this.bypassPermission = bypassPermission;
    }

    public boolean isValidAndEnabled() {
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

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
      buffer.writeBoolean(this.enabled);
      buffer.writeDouble(this.distance);
      buffer.writeDouble(this.strength);
      buffer.writeOptionalString(this.bypassPermission);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
      this.enabled = buffer.readBoolean();
      this.distance = buffer.readDouble();
      this.strength = buffer.readDouble();
      this.bypassPermission = buffer.readOptionalString();
    }
  }
}
