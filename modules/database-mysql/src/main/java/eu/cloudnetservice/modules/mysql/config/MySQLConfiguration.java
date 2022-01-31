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

package eu.cloudnetservice.modules.mysql.config;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;

public record MySQLConfiguration(
  @NonNull String username,
  @NonNull String password,
  @NonNull String databaseServiceName,
  @NonNull List<MySQLConnectionEndpoint> endpoints
) {

  public @NonNull MySQLConnectionEndpoint randomEndpoint() {
    // check if there are any endpoints
    if (this.endpoints.isEmpty()) {
      throw new IllegalStateException("No mysql connection endpoints available");
    }
    // return a random stream
    return this.endpoints.get(ThreadLocalRandom.current().nextInt(0, this.endpoints.size()));
  }
}
