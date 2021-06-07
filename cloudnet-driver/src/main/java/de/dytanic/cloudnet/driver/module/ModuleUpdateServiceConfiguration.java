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

package de.dytanic.cloudnet.driver.module;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ModuleUpdateServiceConfiguration {

  protected boolean autoInstall;

  protected String url;
  protected String currentVersion;
  protected String infoMessage;

  public ModuleUpdateServiceConfiguration(boolean autoInstall, String url, String currentVersion, String infoMessage) {
    this.autoInstall = autoInstall;
    this.url = url;
    this.currentVersion = currentVersion;
    this.infoMessage = infoMessage;
  }

  public ModuleUpdateServiceConfiguration() {
  }

  public boolean isAutoInstall() {
    return this.autoInstall;
  }

  public void setAutoInstall(boolean autoInstall) {
    this.autoInstall = autoInstall;
  }

  public String getUrl() {
    return this.url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getCurrentVersion() {
    return this.currentVersion;
  }

  public void setCurrentVersion(String currentVersion) {
    this.currentVersion = currentVersion;
  }

  public String getInfoMessage() {
    return this.infoMessage;
  }

  public void setInfoMessage(String infoMessage) {
    this.infoMessage = infoMessage;
  }

}
