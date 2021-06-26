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

package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import org.jetbrains.annotations.NotNull;

public class SignGroupConfiguration implements Cloneable, SerializableObject {

  protected String targetGroup;

  protected SignLayoutsHolder emptyLayout;
  protected SignLayoutsHolder onlineLayout;
  protected SignLayoutsHolder fullLayout;

  public SignGroupConfiguration() {
  }

  public SignGroupConfiguration(String targetGroup, SignLayoutsHolder emptyLayout, SignLayoutsHolder onlineLayout,
    SignLayoutsHolder fullLayout) {
    this.targetGroup = targetGroup;
    this.emptyLayout = emptyLayout;
    this.onlineLayout = onlineLayout;
    this.fullLayout = fullLayout;
  }

  public String getTargetGroup() {
    return targetGroup;
  }

  public SignLayoutsHolder getEmptyLayout() {
    return emptyLayout;
  }

  public SignLayoutsHolder getOnlineLayout() {
    return onlineLayout;
  }

  public SignLayoutsHolder getFullLayout() {
    return fullLayout;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.targetGroup);
    buffer.writeObject(this.emptyLayout);
    buffer.writeObject(this.onlineLayout);
    buffer.writeObject(this.fullLayout);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.targetGroup = buffer.readString();
    this.emptyLayout = buffer.readObject(SignLayoutsHolder.class);
    this.onlineLayout = buffer.readObject(SignLayoutsHolder.class);
    this.fullLayout = buffer.readObject(SignLayoutsHolder.class);
  }
}
