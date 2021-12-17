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

import de.dytanic.cloudnet.common.INameable;
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public final class Permission implements INameable, Comparable<Permission> {

  private final String name;

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

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull Permission of(@NotNull String name) {
    return new Permission(name);
  }

  @Override
  public @NotNull String name() {
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
  public int compareTo(@NotNull Permission o) {
    return Integer.compare(Math.abs(this.potency()), Math.abs(o.potency()));
  }
}
