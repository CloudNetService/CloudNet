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

package eu.cloudnetservice.driver.ap.registry;

import java.io.DataOutput;
import java.io.IOException;
import javax.lang.model.element.Name;
import lombok.NonNull;

record AutoServiceMapping(
  @NonNull Name serviceType,
  @NonNull Name implementationType,
  @NonNull String serviceName,
  boolean singletonService,
  boolean markServiceAsDefault
) {

  public void serialize(@NonNull DataOutput dataOutput) throws IOException {
    dataOutput.writeByte(0x01); // version
    dataOutput.writeUTF(this.serviceType.toString());
    dataOutput.writeUTF(this.implementationType.toString());
    dataOutput.writeUTF(this.serviceName);
    dataOutput.writeBoolean(this.singletonService);
    dataOutput.writeBoolean(this.markServiceAsDefault);
  }
}
