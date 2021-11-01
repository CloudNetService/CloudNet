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

package de.dytanic.cloudnet.ext.syncproxy.configuration;

import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SyncProxyTabListConfiguration {

  protected String targetGroup;

  protected List<SyncProxyTabList> entries;

  protected double animationsPerSecond;

  public SyncProxyTabListConfiguration(String targetGroup, List<SyncProxyTabList> entries, double animationsPerSecond) {
    this.targetGroup = targetGroup;
    this.entries = entries;
    this.animationsPerSecond = animationsPerSecond;
  }

  public static SyncProxyTabListConfiguration createDefaultTabListConfiguration(String targetGroup) {
    return new SyncProxyTabListConfiguration(
      targetGroup,
      Collections.singletonList(
        new SyncProxyTabList(
          " \n &b&o■ &8┃ &3&lCloudNet &8● &cBlizzard &8&l» &7&o%online_players%&8/&7&o%max_players% &8┃ &b&o■ "
            + "\n &8► &7Current server &8● &b%server% &8◄ \n ",
          " \n &7Discord &8&l» &bdiscord.cloudnetservice.eu \n &7&onext &3&l&ogeneration &7&onetwork \n "
        )
      ),
      1
    );
  }

  public String getTargetGroup() {
    return this.targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public List<SyncProxyTabList> getEntries() {
    return this.entries;
  }

  public void setEntries(List<SyncProxyTabList> entries) {
    this.entries = entries;
  }

  public double getAnimationsPerSecond() {
    return this.animationsPerSecond;
  }

  public void setAnimationsPerSecond(double animationsPerSecond) {
    this.animationsPerSecond = animationsPerSecond;
  }

}
