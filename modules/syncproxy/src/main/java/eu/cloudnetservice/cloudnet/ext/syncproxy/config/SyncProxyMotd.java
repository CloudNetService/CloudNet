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

package eu.cloudnetservice.cloudnet.ext.syncproxy.config;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SyncProxyMotd(
  @NotNull String firstLine,
  @NotNull String secondLine,
  boolean autoSlot,
  int autoSlotMaxPlayersDistance,
  @NotNull String[] playerInfo,
  @Nullable String protocolText
) {

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull SyncProxyMotd motd) {
    return builder()
      .firstLine(motd.firstLine())
      .secondLine(motd.secondLine())
      .autoSlot(motd.autoSlot())
      .autoSlotDistance(motd.autoSlotMaxPlayersDistance())
      .playerInfo(motd.playerInfo())
      .protocolText(motd.protocolText());
  }

  @Contract("null, _, _ -> null; !null, _, _ -> !null")
  public @Nullable String format(@Nullable String input, int onlinePlayers, int maxPlayers) {
    if (input == null) {
      return null;
    }

    return input
      .replace("%proxy%", Wrapper.instance().serviceId().name())
      .replace("%proxy_uniqueId%", String.valueOf(Wrapper.instance().serviceId().uniqueId()))
      .replace("%task%", Wrapper.instance().serviceId().taskName())
      .replace("%node%", Wrapper.instance().serviceId().nodeUniqueId())
      .replace("%online_players%", String.valueOf(onlinePlayers))
      .replace("%max_players%", String.valueOf(maxPlayers))
      .replace("&", "§");
  }

  public static class Builder {

    private String firstLine;
    private String secondLine;

    private boolean autoSlot;
    private int autoSlotMaxPlayersDistance;

    private String[] playerInfo = new String[0];
    private String protocolText;

    public @NotNull Builder firstLine(@NotNull String firstLine) {
      this.firstLine = firstLine;
      return this;
    }

    public @NotNull Builder secondLine(@NotNull String secondLine) {
      this.secondLine = secondLine;
      return this;
    }

    public @NotNull Builder autoSlot(boolean autoSlot) {
      this.autoSlot = autoSlot;
      return this;
    }

    public @NotNull Builder autoSlotDistance(int autoSlotDistance) {
      this.autoSlotMaxPlayersDistance = autoSlotDistance;
      return this;
    }

    public @NotNull Builder playerInfo(String @NotNull [] playerInfo) {
      this.playerInfo = playerInfo;
      return this;
    }

    public @NotNull Builder protocolText(@Nullable String protocolText) {
      this.protocolText = protocolText;
      return this;
    }

    public @NotNull SyncProxyMotd build() {
      Verify.verifyNotNull(this.firstLine, "Missing first line");
      Verify.verifyNotNull(this.secondLine, "Missing second line");

      return new SyncProxyMotd(this.firstLine,
        this.secondLine,
        this.autoSlot,
        this.autoSlotMaxPlayersDistance,
        this.playerInfo,
        this.protocolText);
    }
  }
}
