/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.object;

import eu.cloudnetservice.driver.network.rpc.annotation.RPCFieldGetter;
import java.util.UUID;

public record ObjectWithSpecialGetter(
  @RPCFieldGetter("reallyNoAGetterIDontKnowWhatThisIsId") UUID id,
  String username
) {

  public UUID reallyNoAGetterIDontKnowWhatThisIsId() {
    return UUID.fromString("bcc582ed-494d-4b93-86cb-b58564651a26");
  }
}