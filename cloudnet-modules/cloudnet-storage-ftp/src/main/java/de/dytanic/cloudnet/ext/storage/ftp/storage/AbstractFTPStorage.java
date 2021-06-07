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

package de.dytanic.cloudnet.ext.storage.ftp.storage;

import de.dytanic.cloudnet.driver.template.defaults.DefaultSyncTemplateStorage;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;

public abstract class AbstractFTPStorage extends DefaultSyncTemplateStorage {

  protected final FTPCredentials credentials;
  protected final FTPType ftpType;
  protected final String baseDirectory;
  private final String name;

  AbstractFTPStorage(String name, FTPCredentials credentials, FTPType ftpType) {
    this.name = name;
    this.credentials = credentials;
    this.ftpType = ftpType;

    String baseDirectory = credentials.getBaseDirectory();

    this.baseDirectory =
      baseDirectory.endsWith("/") ? baseDirectory.substring(0, baseDirectory.length() - 1) : baseDirectory;
  }

  public abstract boolean connect();

  public abstract boolean isAvailable();

  public abstract void completeDataTransfer();

  @Override
  public String getName() {
    return this.name;
  }

  public FTPCredentials getCredentials() {
    return this.credentials;
  }

  public FTPType getFtpType() {
    return this.ftpType;
  }

  public String getBaseDirectory() {
    return this.baseDirectory;
  }

}
