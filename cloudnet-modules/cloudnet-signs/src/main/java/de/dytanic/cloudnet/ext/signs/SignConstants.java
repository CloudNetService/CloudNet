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

package de.dytanic.cloudnet.ext.signs;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;

public final class SignConstants {

  public static final Type COLLECTION_SIGNS = new TypeToken<Collection<Sign>>() {
  }.getType();

  public static final String SIGN_CLUSTER_CHANNEL_NAME = "cloudnet_cluster_signs_channel";
  public static final String SIGN_CHANNEL_NAME = "cloudnet_signs_channel";
  public static final String SIGN_CHANNEL_GET_SIGNS = "signs_get_signs_collection";
  public static final String SIGN_CHANNEL_GET_SIGNS_CONFIGURATION = "signs_get_signs_configuration";
  public static final String SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION = "update_sign_configuration";
  public static final String SIGN_CHANNEL_ADD_SIGN_MESSAGE = "add_sign";
  public static final String SIGN_CHANNEL_REMOVE_SIGN_MESSAGE = "remove_sign";

  private SignConstants() {
    throw new UnsupportedOperationException();
  }

}
