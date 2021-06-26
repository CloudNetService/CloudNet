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

package de.dytanic.cloudnet.ext.storage.ftp.client;

import de.dytanic.cloudnet.ext.storage.ftp.storage.AbstractFTPStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.FTPTemplateStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.SFTPTemplateStorage;

public enum FTPType {
  SFTP {
    @Override
    public AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
      return new SFTPTemplateStorage(storage, credentials);
    }
  },
  FTP {
    @Override
    public AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
      return new FTPTemplateStorage(storage, credentials, false);
    }
  },
  FTPS {
    @Override
    public AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
      return new FTPTemplateStorage(storage, credentials, true);
    }
  };


  public abstract AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials);

}
