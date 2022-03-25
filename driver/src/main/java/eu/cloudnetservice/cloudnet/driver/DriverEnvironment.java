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

package eu.cloudnetservice.cloudnet.driver;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import lombok.NonNull;

public final class DriverEnvironment extends JsonDocPropertyHolder {

  public static final DriverEnvironment NODE = new DriverEnvironment("node", JsonDocument.newDocument());
  public static final DriverEnvironment WRAPPER = new DriverEnvironment("wrapper", JsonDocument.newDocument());

  private final String name;

  public DriverEnvironment(@NonNull String name, @NonNull JsonDocument document) {
    super(document);
    this.name = name;
  }

  public @NonNull String name() {
    return this.name;
  }
}
