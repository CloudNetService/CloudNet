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

package de.dytanic.cloudnet.driver.network.ssl;

import java.io.File;
import java.nio.file.Path;

public class SSLConfiguration {

  protected final boolean clientAuth;

  protected final Path trustCertificatePath;
  protected final Path certificatePath;
  protected final Path privateKeyPath;

  @Deprecated
  public SSLConfiguration(boolean clientAuth, File trustCertificatePath, File certificatePath, File privateKeyPath) {
    this(
      clientAuth,
      trustCertificatePath == null ? null : trustCertificatePath.toPath(),
      certificatePath == null ? null : certificatePath.toPath(),
      privateKeyPath == null ? null : privateKeyPath.toPath()
    );
  }

  public SSLConfiguration(boolean clientAuth, Path trustCertificatePath, Path certificatePath, Path privateKeyPath) {
    this.clientAuth = clientAuth;
    this.trustCertificatePath = trustCertificatePath;
    this.certificatePath = certificatePath;
    this.privateKeyPath = privateKeyPath;
  }

  public boolean isClientAuth() {
    return this.clientAuth;
  }

  @Deprecated
  public File getTrustCertificatePath() {
    return this.trustCertificatePath == null ? null : this.trustCertificatePath.toFile();
  }

  @Deprecated
  public File getCertificatePath() {
    return this.certificatePath == null ? null : this.certificatePath.toFile();
  }

  @Deprecated
  public File getPrivateKeyPath() {
    return this.privateKeyPath == null ? null : this.privateKeyPath.toFile();
  }

  public Path getTrustCertificate() {
    return this.trustCertificatePath;
  }

  public Path getCertificate() {
    return this.certificatePath;
  }

  public Path getPrivateKey() {
    return this.privateKeyPath;
  }
}
