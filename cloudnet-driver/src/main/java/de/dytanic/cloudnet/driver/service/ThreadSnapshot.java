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

package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * The information of a thread running on any process in the Cloud.
 */
@ToString
@EqualsAndHashCode
public class ThreadSnapshot implements SerializableObject {

  private long id;

  private String name;

  private Thread.State threadState;

  private boolean daemon;

  private int priority;

  public ThreadSnapshot(long id, String name, Thread.State threadState, boolean daemon, int priority) {
    this.id = id;
    this.name = name;
    this.threadState = threadState;
    this.daemon = daemon;
    this.priority = priority;
  }

  public ThreadSnapshot() {
  }

  public long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public Thread.State getThreadState() {
    return this.threadState;
  }

  public boolean isDaemon() {
    return this.daemon;
  }

  public int getPriority() {
    return this.priority;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeLong(this.id);
    buffer.writeString(this.name);
    buffer.writeEnumConstant(this.threadState);
    buffer.writeBoolean(this.daemon);
    buffer.writeVarInt(this.priority);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.id = buffer.readLong();
    this.name = buffer.readString();
    this.threadState = buffer.readEnumConstant(Thread.State.class);
    this.daemon = buffer.readBoolean();
    this.priority = buffer.readVarInt();
  }
}
