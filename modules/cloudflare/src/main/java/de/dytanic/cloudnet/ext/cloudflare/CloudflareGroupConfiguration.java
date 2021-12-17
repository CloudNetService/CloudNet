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

package de.dytanic.cloudnet.ext.cloudflare;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CloudflareGroupConfiguration {

  protected String name;
  protected String sub;

  protected int priority;
  protected int weight;

  public CloudflareGroupConfiguration(String name, String sub, int priority, int weight) {
    this.name = name;
    this.sub = sub;
    this.priority = priority;
    this.weight = weight;
  }

  public String name() {
    return this.name;
  }

  public void name(String name) {
    this.name = name;
  }

  public String sub() {
    return this.sub;
  }

  public void sub(String sub) {
    this.sub = sub;
  }

  public int priority() {
    return this.priority;
  }

  public void priority(int priority) {
    this.priority = priority;
  }

  public int weight() {
    return this.weight;
  }

  public void weight(int weight) {
    this.weight = weight;
  }

}
