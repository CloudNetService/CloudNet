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

package de.dytanic.cloudnet.http;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public final class AccessControlConfiguration {

  private static AccessControlConfiguration defaultConfiguration = new AccessControlConfiguration(
    "strict-origin-when-cross-origin", 3600);

  private String corsPolicy;
  private int accessControlMaxAge;

  public AccessControlConfiguration(String corsPolicy, int accessControlMaxAge) {
    this.corsPolicy = corsPolicy;
    this.accessControlMaxAge = accessControlMaxAge;
  }

  public static @NotNull AccessControlConfiguration defaults() {
    return AccessControlConfiguration.defaultConfiguration;
  }

  public static void setDefaultConfiguration(@NotNull AccessControlConfiguration configuration) {
    AccessControlConfiguration.defaultConfiguration = configuration;
  }

  public String getCorsPolicy() {
    return this.corsPolicy;
  }

  public void setCorsPolicy(String corsPolicy) {
    this.corsPolicy = corsPolicy;
  }

  public int getAccessControlMaxAge() {
    return this.accessControlMaxAge;
  }

  public void setAccessControlMaxAge(int accessControlMaxAge) {
    this.accessControlMaxAge = accessControlMaxAge;
  }
}
