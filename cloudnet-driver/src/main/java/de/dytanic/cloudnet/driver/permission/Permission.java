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
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public final class Permission implements SerializableObject, Comparable<Permission> {

  private String name;

  private int potency;

  private long timeOutMillis;

  public Permission(@NotNull String name, int potency) {
    this.name = name;
    this.potency = potency;
  }

  public Permission(@NotNull String name, int potency, long time, @NotNull TimeUnit timeUnit) {
    this.name = name;
    this.potency = potency;
    this.timeOutMillis = System.currentTimeMillis() + timeUnit.toMillis(time);
  }

  public Permission(@NotNull String name) {
    this.name = name;
  }

  public Permission(@NotNull String name, int potency, long timeOutMillis) {
    this.name = name;
    this.potency = potency;
    this.timeOutMillis = timeOutMillis;
  }

  public Permission() {
  }

  @NotNull
  public String getName() {
    return this.name;
  }

  public int getPotency() {
    return this.potency;
  }

  public void setPotency(int potency) {
    this.potency = potency;
  }

  public long getTimeOutMillis() {
    return this.timeOutMillis;
  }

  public void setTimeOutMillis(long timeOutMillis) {
    this.timeOutMillis = timeOutMillis;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.name);
    buffer.writeInt(this.potency);
    buffer.writeLong(this.timeOutMillis);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.name = buffer.readString();
    this.potency = buffer.readInt();
    this.timeOutMillis = buffer.readLong();
  }

  @Override
  public int compareTo(@NotNull Permission o) {
    return Integer.compare(Math.abs(this.getPotency()), Math.abs(o.getPotency()));
  }
}
