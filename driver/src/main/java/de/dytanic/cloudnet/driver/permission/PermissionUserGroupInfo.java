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

package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUserGroupInfo extends JsonDocPropertyHolder {

  protected String group;
  protected long timeOutMillis;

  public PermissionUserGroupInfo(@NonNull String group, long timeOutMillis) {
    this.group = group;
    this.timeOutMillis = timeOutMillis;
  }

  public PermissionUserGroupInfo(@NonNull String group, long timeOutMillis, @NonNull JsonDocument properties) {
    this.group = group;
    this.timeOutMillis = timeOutMillis;
    this.properties = properties;
  }

  public @NonNull String group() {
    return this.group;
  }

  public void group(@NonNull String group) {
    this.group = group;
  }

  public long timeOutMillis() {
    return this.timeOutMillis;
  }

  public void timeOutMillis(long timeOutMillis) {
    this.timeOutMillis = timeOutMillis;
  }
}
