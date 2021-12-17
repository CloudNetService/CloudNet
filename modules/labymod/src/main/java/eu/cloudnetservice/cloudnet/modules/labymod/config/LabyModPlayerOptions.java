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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record LabyModPlayerOptions(
  @NonNull String version,
  long creationTime,
  @Nullable UUID joinSecret,
  long lastJoinSecretRedeem,
  @Nullable UUID spectateSecret,
  long lastSpectateSecretRedeem
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull LabyModPlayerOptions playerOptions) {
    return builder()
      .version(playerOptions.version())
      .creationTime(playerOptions.creationTime())
      .joinSecret(playerOptions.joinSecret())
      .joinRedeemTime(playerOptions.lastJoinSecretRedeem())
      .spectateSecret(playerOptions.spectateSecret())
      .spectateRedeemTime(playerOptions.lastSpectateSecretRedeem());
  }

  public static class Builder {

    private String version;
    private long creationTime = -1;
    private UUID joinSecret;
    private long joinRedeemTime = -1;
    private UUID spectateSecret;
    private long spectateRedeemTime = -1;

    public @NonNull Builder version(@NonNull String version) {
      this.version = version;
      return this;
    }

    public @NonNull Builder creationTime(long creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    public @NonNull Builder joinSecret(@Nullable UUID joinSecret) {
      this.joinSecret = joinSecret;
      return this;
    }

    public @NonNull Builder joinRedeemTime(long time) {
      this.joinRedeemTime = time;
      return this;
    }

    public @NonNull Builder spectateSecret(@Nullable UUID spectateSecret) {
      this.spectateSecret = spectateSecret;
      return this;
    }

    public @NonNull Builder spectateRedeemTime(long time) {
      this.spectateRedeemTime = time;
      return this;
    }

    public @NonNull LabyModPlayerOptions build() {
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
