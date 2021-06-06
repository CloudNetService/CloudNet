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

package de.dytanic.cloudnet.ext.smart;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class CloudNetServiceSmartProfile extends BasicJsonDocPropertyable {

  private final UUID uniqueId;

  private final AtomicInteger autoStopCount;

  public CloudNetServiceSmartProfile(UUID uniqueId, AtomicInteger autoStopCount) {
    this.uniqueId = uniqueId;
    this.autoStopCount = autoStopCount;
  }

  public UUID getUniqueId() {
    return this.uniqueId;
  }

  public AtomicInteger getAutoStopCount() {
    return this.autoStopCount;
  }

}
