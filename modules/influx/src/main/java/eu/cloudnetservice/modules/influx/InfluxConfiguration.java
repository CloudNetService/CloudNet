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

package eu.cloudnetservice.modules.influx;

import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import lombok.NonNull;

public record InfluxConfiguration(
  @NonNull HostAndPort databaseAddress,
  @NonNull String token,
  @NonNull String org,
  @NonNull String bucket,
  int publishDelaySeconds
) {

  public @NonNull String connectUrl() {
    // ignore ports < 0
    if (this.databaseAddress.validPort()) {
      return this.databaseAddress.toString();
    } else {
      return this.databaseAddress.host();
    }
  }
}
