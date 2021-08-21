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

import java.nio.file.Path;
import java.nio.file.Paths;

public class SSLConfiguration {

  protected final boolean enabled;
  protected final boolean clientAuth;

  protected final String trustCertificatePath;
  protected final String certificatePath;
  protected final String privateKeyPath;

  protected transient Path trustCertificatePathCached;
  protected transient Path certificatePathCached;
  protected transient Path privateKeyPathCached;

  public SSLConfiguration(boolean enabled, boolean clientAuth,
    String trustCertificatePath, String certificatePath, String privateKeyPath) {
    this.enabled = enabled;
    this.clientAuth = clientAuth;
    this.trustCertificatePath = trustCertificatePath;
    this.certificatePath = certificatePath;
    this.privateKeyPath = privateKeyPath;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public boolean isClientAuth() {
    return this.clientAuth;
  }

  public Path getTrustCertificate() {
    if (this.trustCertificatePathCached == null) {
      this.trustCertificatePathCached = Paths.get(this.trustCertificatePath);
    }
    return this.trustCertificatePathCached;
  }

  public Path getCertificate() {
    if (this.certificatePathCached == null) {
      this.certificatePathCached = Paths.get(certificatePath);
    }
    return this.certificatePathCached;
  }

  public Path getPrivateKey() {
    if (this.privateKeyPathCached == null) {
      this.privateKeyPathCached = Paths.get(this.privateKeyPath);
    }
    return this.privateKeyPathCached;
  }
}
