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

package de.dytanic.cloudnet.ext.bridge;

import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ProxyFallback implements Comparable<ProxyFallback> {

  protected String task;
  protected String permission;

  protected Collection<String> availableOnGroups = new ArrayList<>();

  protected String forcedHost;
  private int priority;

  public ProxyFallback(String task, String permission, int priority) {
    this.task = task;
    this.permission = permission;
    this.priority = priority;
  }

  public ProxyFallback() {
  }

  @Override
  public int compareTo(ProxyFallback o) {
    return Integer.compare(o.priority, this.priority);
  }

  public String getTask() {
    return this.task;
  }

  public void setTask(String task) {
    this.task = task;
  }

  public String getPermission() {
    return this.permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public Collection<String> getAvailableOnGroups() {
    return this.availableOnGroups;
  }

  public void setAvailableOnGroups(Collection<String> availableOnGroups) {
    this.availableOnGroups = availableOnGroups;
  }

  public String getForcedHost() {
    return this.forcedHost;
  }

  public void setForcedHost(String forcedHost) {
    this.forcedHost = forcedHost;
  }

  public int getPriority() {
    return this.priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

}
