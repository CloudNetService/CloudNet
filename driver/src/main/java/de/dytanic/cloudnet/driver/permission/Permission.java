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

import de.dytanic.cloudnet.common.Nameable;
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Contract;

@ToString
@EqualsAndHashCode
public final class Permission implements Nameable, Comparable<Permission> {

  private final String name;

  private int potency;
  private long timeOutMillis;

  public Permission(@NonNull String name, int potency) {
    this.name = name;
    this.potency = potency;
  }

  public Permission(@NonNull String name, int potency, long time, @NonNull TimeUnit timeUnit) {
    this.name = name;
    this.potency = potency;
    this.timeOutMillis = System.currentTimeMillis() + timeUnit.toMillis(time);
  }

  public Permission(@NonNull String name) {
    this.name = name;
  }

  public Permission(@NonNull String name, int potency, long timeOutMillis) {
    this.name = name;
    this.potency = potency;
    this.timeOutMillis = timeOutMillis;
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NonNull Permission of(@NonNull String name) {
    return new Permission(name);
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  public int potency() {
    return this.potency;
  }

  public void potency(int potency) {
    this.potency = potency;
  }

  public long timeOutMillis() {
    return this.timeOutMillis;
  }

  public void timeOutMillis(long timeOutMillis) {
    this.timeOutMillis = timeOutMillis;
  }

  @Override
  public int compareTo(@NonNull Permission o) {
    return Integer.compare(Math.abs(this.potency()), Math.abs(o.potency()));
  }
}
