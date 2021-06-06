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

package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUserGroupInfo extends SerializableJsonDocPropertyable implements SerializableObject {

  protected String group;

  protected long timeOutMillis;

  public PermissionUserGroupInfo(@NotNull String group, long timeOutMillis) {
    this.group = group;
    this.timeOutMillis = timeOutMillis;
  }

  public PermissionUserGroupInfo() {
  }

  @NotNull
  public String getGroup() {
    return this.group;
  }

  public void setGroup(@NotNull String group) {
    this.group = group;
  }

  public long getTimeOutMillis() {
    return this.timeOutMillis;
  }

  public void setTimeOutMillis(long timeOutMillis) {
    this.timeOutMillis = timeOutMillis;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.group);
    buffer.writeLong(this.timeOutMillis);
    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.group = buffer.readString();
    this.timeOutMillis = buffer.readLong();
    super.read(buffer);
  }
}
