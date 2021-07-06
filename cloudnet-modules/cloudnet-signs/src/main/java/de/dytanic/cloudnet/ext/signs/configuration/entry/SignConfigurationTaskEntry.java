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

package de.dytanic.cloudnet.ext.signs.configuration.entry;

import de.dytanic.cloudnet.ext.signs.SignLayout;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignConfigurationTaskEntry {

  protected String task;

  protected SignLayout onlineLayout;
  protected SignLayout emptyLayout;
  protected SignLayout fullLayout;

  public SignConfigurationTaskEntry(String task, SignLayout onlineLayout, SignLayout emptyLayout,
    SignLayout fullLayout) {
    this.task = task;
    this.onlineLayout = onlineLayout;
    this.emptyLayout = emptyLayout;
    this.fullLayout = fullLayout;
  }

  public SignConfigurationTaskEntry() {
  }

  public String getTask() {
    return this.task;
  }

  public void setTask(String task) {
    this.task = task;
  }

  public SignLayout getOnlineLayout() {
    return this.onlineLayout;
  }

  public void setOnlineLayout(SignLayout onlineLayout) {
    this.onlineLayout = onlineLayout;
  }

  public SignLayout getEmptyLayout() {
    return this.emptyLayout;
  }

  public void setEmptyLayout(SignLayout emptyLayout) {
    this.emptyLayout = emptyLayout;
  }

  public SignLayout getFullLayout() {
    return this.fullLayout;
  }

  public void setFullLayout(SignLayout fullLayout) {
    this.fullLayout = fullLayout;
  }

}
