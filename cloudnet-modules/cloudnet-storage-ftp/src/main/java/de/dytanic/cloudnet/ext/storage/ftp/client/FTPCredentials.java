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

import de.dytanic.cloudnet.driver.network.HostAndPort;

public class FTPCredentials {

  private final HostAndPort address;
  private final String username;
  private final String password;
  private final String baseDirectory;

  public FTPCredentials(HostAndPort address, String username, String password, String baseDirectory) {
    this.address = address;
    this.username = username;
    this.password = password;
    this.baseDirectory = baseDirectory;
  }

  public String getPassword() {
    return this.password;
  }

  public HostAndPort getAddress() {
    return this.address;
  }

  public String getBaseDirectory() {
    return this.baseDirectory;
  }

  public String getUsername() {
    return this.username;
  }
}
