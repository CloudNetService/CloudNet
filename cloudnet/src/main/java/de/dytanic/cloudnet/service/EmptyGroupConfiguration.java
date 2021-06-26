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

package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import java.util.ArrayList;

public final class EmptyGroupConfiguration extends GroupConfiguration {

  public EmptyGroupConfiguration(String name) {
    super(name);

    super.includes = new ArrayList<>();
    super.templates = new ArrayList<>();
    super.deployments = new ArrayList<>();
  }

}
