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
import org.jetbrains.annotations.Nullable;

public class SSLConfiguration {

  protected final boolean enabled;
  protected final boolean clientAuth;

  protected final Path trustCertificatePath;
  protected final Path certificatePath;
  protected final Path privateKeyPath;

  public SSLConfiguration(
    boolean enabled,
    boolean clientAuth,
    @Nullable Path trustCertificatePath,
    @Nullable Path certificatePath,
    @Nullable Path privateKeyPath
  ) {
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

  public @Nullable Path getTrustCertificatePath() {
    return this.trustCertificatePath;
  }

  public @Nullable Path getCertificatePath() {
    return this.certificatePath;
  }

  public @Nullable Path getPrivateKeyPath() {
    return this.privateKeyPath;
  }
}
