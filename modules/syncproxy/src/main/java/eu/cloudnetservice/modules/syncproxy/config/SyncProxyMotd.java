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

package eu.cloudnetservice.modules.syncproxy.config;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public record SyncProxyMotd(
  @NonNull String firstLine,
  @NonNull String secondLine,
  boolean autoSlot,
  int autoSlotMaxPlayersDistance,
  @NonNull String[] playerInfo,
  @Nullable String protocolText
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SyncProxyMotd motd) {
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

    public @NonNull Builder firstLine(@NonNull String firstLine) {
      this.firstLine = firstLine;
      return this;
    }

    public @NonNull Builder secondLine(@NonNull String secondLine) {
      this.secondLine = secondLine;
      return this;
    }

    public @NonNull Builder autoSlot(boolean autoSlot) {
      this.autoSlot = autoSlot;
      return this;
    }

    public @NonNull Builder autoSlotDistance(int autoSlotDistance) {
      this.autoSlotMaxPlayersDistance = autoSlotDistance;
      return this;
    }

    public @NonNull Builder playerInfo(String @NonNull [] playerInfo) {
      this.playerInfo = playerInfo;
      return this;
    }

    public @NonNull Builder protocolText(@Nullable String protocolText) {
      this.protocolText = protocolText;
      return this;
    }

    public @NonNull SyncProxyMotd build() {
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
