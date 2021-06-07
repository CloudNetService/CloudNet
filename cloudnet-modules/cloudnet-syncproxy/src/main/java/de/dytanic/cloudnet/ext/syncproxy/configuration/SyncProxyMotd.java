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

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SyncProxyMotd {

  protected String firstLine;
  protected String secondLine;

  protected boolean autoSlot;

  protected int autoSlotMaxPlayersDistance;

  protected String[] playerInfo;

  protected String protocolText;

  public SyncProxyMotd(String firstLine, String secondLine, boolean autoSlot, int autoSlotMaxPlayersDistance,
    String[] playerInfo, String protocolText) {
    this.firstLine = firstLine;
    this.secondLine = secondLine;
    this.autoSlot = autoSlot;
    this.autoSlotMaxPlayersDistance = autoSlotMaxPlayersDistance;
    this.playerInfo = playerInfo;
    this.protocolText = protocolText;
  }

  public SyncProxyMotd() {
  }

  public String getFirstLine() {
    return this.firstLine;
  }

  public void setFirstLine(String firstLine) {
    this.firstLine = firstLine;
  }

  public String getSecondLine() {
    return this.secondLine;
  }

  public void setSecondLine(String secondLine) {
    this.secondLine = secondLine;
  }

  public boolean isAutoSlot() {
    return this.autoSlot;
  }

  public void setAutoSlot(boolean autoSlot) {
    this.autoSlot = autoSlot;
  }

  public int getAutoSlotMaxPlayersDistance() {
    return this.autoSlotMaxPlayersDistance;
  }

  public void setAutoSlotMaxPlayersDistance(int autoSlotMaxPlayersDistance) {
    this.autoSlotMaxPlayersDistance = autoSlotMaxPlayersDistance;
  }

  public String[] getPlayerInfo() {
    return this.playerInfo;
  }

  public void setPlayerInfo(String[] playerInfo) {
    this.playerInfo = playerInfo;
  }

  public String getProtocolText() {
    return this.protocolText;
  }

  public void setProtocolText(String protocolText) {
    this.protocolText = protocolText;
  }

}
