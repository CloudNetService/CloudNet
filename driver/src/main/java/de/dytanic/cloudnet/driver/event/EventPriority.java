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

package de.dytanic.cloudnet.driver.event;

import java.util.Comparator;

public enum EventPriority implements Comparator<EventPriority> {

  HIGHEST(128),
  HIGH(64),
  NORMAL(32),
  LOW(16),
  LOWEST(8);

  private final int value;

  EventPriority(int value) {
    this.value = value;
  }

  @Override
  public int compare(EventPriority o1, EventPriority o2) {
    return Integer.compare(o1.value, o2.value);
  }

  public int getValue() {
    return this.value;
  }
}
