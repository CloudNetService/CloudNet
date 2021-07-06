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

package eu.cloudnetservice.cloudnet.ext.signs.node.configuration;

import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;

public enum SignConfigurationType {
  JAVA {
    public SignConfigurationEntry createEntry(String targetGroup) {
      return SignConfigurationEntry
        .createDefault(targetGroup, "GOLD_BLOCK", "EMERALD_BLOCK", "BEDROCK", "REDSTONE_BLOCK");
    }
  },
  BEDROCK {
    public SignConfigurationEntry createEntry(String targetGroup) {
      return SignConfigurationEntry.createDefault(targetGroup, "41", "133", "7", "152");
    }
  };

  public abstract SignConfigurationEntry createEntry(String targetGroup);
}
