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

package de.dytanic.cloudnet.ext.cloudflare;

import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CloudflareConfiguration {

  protected Collection<CloudflareConfigurationEntry> entries;

  public CloudflareConfiguration(Collection<CloudflareConfigurationEntry> entries) {
    this.entries = entries;
  }

  public Collection<CloudflareConfigurationEntry> entries() {
    return this.entries;
  }

  public void entries(Collection<CloudflareConfigurationEntry> entries) {
    this.entries = entries;
  }

}
