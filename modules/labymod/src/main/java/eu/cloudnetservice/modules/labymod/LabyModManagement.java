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

package eu.cloudnetservice.modules.labymod;

import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import lombok.NonNull;

public interface LabyModManagement {

  String LABYMOD_CLIENT_CHANNEL = "labymod3:main";
  String LABYMOD_MODULE_CHANNEL = "labymod_internal";
  String LABYMOD_UPDATE_CONFIG = "update_labymod_config";

  @NonNull
  LabyModConfiguration configuration();

  void configuration(@NonNull LabyModConfiguration configuration);
}
