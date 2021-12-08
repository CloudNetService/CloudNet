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

package eu.cloudnetservice.cloudnet.modules.labymod.config;

import com.google.common.base.Verify;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LabyModPlayerOptions {

  protected final String version;
  protected final long creationTime;
  protected final UUID joinSecret;
  protected final long lastJoinSecretRedeem;
  protected final UUID spectateSecret;
  protected final long lastSpectateSecretRedeem;

  protected LabyModPlayerOptions(
    @NotNull String version,
    long creationTime,
    @Nullable UUID joinSecret,
    long lastJoinSecretRedeem,
    @Nullable UUID spectateSecret,
    long lastSpectateSecretRedeem
  ) {
    this.version = version;
    this.creationTime = creationTime;
    this.joinSecret = joinSecret;
    this.lastJoinSecretRedeem = lastJoinSecretRedeem;
    this.spectateSecret = spectateSecret;
    this.lastSpectateSecretRedeem = lastSpectateSecretRedeem;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull LabyModPlayerOptions playerOptions) {
    return builder()
      .version(playerOptions.getVersion())
      .creationTime(playerOptions.getCreationTime())
      .joinSecret(playerOptions.getJoinSecret())
      .joinRedeemTime(playerOptions.getLastJoinSecretRedeem())
      .spectateSecret(playerOptions.getSpectateSecret())
      .spectateRedeemTime(playerOptions.getLastSpectateSecretRedeem());
  }

  public @NotNull String getVersion() {
    return this.version;
  }

  public long getCreationTime() {
    return this.creationTime;
  }

  public @Nullable UUID getJoinSecret() {
    return this.joinSecret;
  }

  public long getLastJoinSecretRedeem() {
    return this.lastJoinSecretRedeem;
  }

  public @Nullable UUID getSpectateSecret() {
    return this.spectateSecret;
  }

  public long getLastSpectateSecretRedeem() {
    return this.lastSpectateSecretRedeem;
  }

  public static class Builder {

    private String version;
    private long creationTime = -1;
    private UUID joinSecret;
    private long joinRedeemTime = -1;
    private UUID spectateSecret;
    private long spectateRedeemTime = -1;

    public @NotNull Builder version(@NotNull String version) {
      this.version = version;
      return this;
    }

    public @NotNull Builder creationTime(long creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    public @NotNull Builder joinSecret(@Nullable UUID joinSecret) {
      this.joinSecret = joinSecret;
      return this;
    }

    public @NotNull Builder joinRedeemTime(long time) {
      this.joinRedeemTime = time;
      return this;
    }

    public @NotNull Builder spectateSecret(@Nullable UUID spectateSecret) {
      this.spectateSecret = spectateSecret;
      return this;
    }

    public @NotNull Builder spectateRedeemTime(long time) {
      this.spectateRedeemTime = time;
      return this;
    }

    public @NotNull LabyModPlayerOptions build() {
      Verify.verifyNotNull(this.version, "Missing version");

      return new LabyModPlayerOptions(
        this.version,
        this.creationTime,
        this.joinSecret,
        this.joinRedeemTime,
        this.spectateSecret,
        this.spectateRedeemTime);
    }
  }
}
