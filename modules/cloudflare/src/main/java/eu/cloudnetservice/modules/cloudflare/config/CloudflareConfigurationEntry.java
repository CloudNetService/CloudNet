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

package eu.cloudnetservice.modules.cloudflare.config;

import eu.cloudnetservice.common.StringUtil;
import java.util.Collection;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record CloudflareConfigurationEntry(
  boolean enabled,
  @NonNull AuthenticationMethod authenticationMethod,
  @Nullable String entryName,
  @NonNull String hostAddress,
  @NonNull String email,
  @NonNull String apiToken,
  @NonNull String zoneId,
  @NonNull String domainName,
  @NonNull Collection<CloudflareGroupConfiguration> groups
) {

  public CloudflareConfigurationEntry {
    // put in a random entry name if no name is given
    if (entryName == null) {
      entryName = StringUtil.generateRandomString(7);
    }
  }

  public static boolean mightEqual(
    @NonNull CloudflareConfigurationEntry left,
    @NonNull CloudflareConfigurationEntry right
  ) {
    return Objects.equals(left.entryName(), right.entryName()) && Objects.equals(left.zoneId(), right.zoneId());
  }

  public enum AuthenticationMethod {
    GLOBAL_KEY,
    BEARER_TOKEN
  }
}
