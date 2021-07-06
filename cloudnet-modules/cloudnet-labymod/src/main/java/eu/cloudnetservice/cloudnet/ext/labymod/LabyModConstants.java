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

package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.Arrays;
import java.util.Collection;

public interface LabyModConstants {

  String LMC_CHANNEL_NAME = "labymod3:main";
  String GET_CONFIGURATION = "get_cloudnet_labymod_config";
  String GET_PLAYER_JOIN_SECRET = "get_player_by_join_secret";
  String GET_PLAYER_SPECTATE_SECRET = "get_player_by_spectate_secret";
  String CLOUDNET_CHANNEL_NAME = "cloudnet_labymod_module";

  Collection<ServiceEnvironmentType> SUPPORTED_ENVIRONMENTS = Arrays
    .asList(ServiceEnvironmentType.BUNGEECORD, ServiceEnvironmentType.VELOCITY);
}
