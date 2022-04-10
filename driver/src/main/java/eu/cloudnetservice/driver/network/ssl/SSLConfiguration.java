/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.ssl;

import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the configuration option for ssl. A ssl configuration can be enabled without any certificates given,
 * meaning that the server/client should use self-signed certificates and an insecure trust store.
 *
 * @param enabled              if the ssl configuration is enabled.
 * @param clientAuth           if client authorization should be required.
 * @param trustCertificatePath the trusted certificate for remote certificate validation.
 * @param certificatePath      the path to an X509 certificate chain in the pem format.
 * @param privateKeyPath       the path to a PKCS#8 private key in PEM format.
 * @since 4.0
 */
public record SSLConfiguration(
  boolean enabled,
  boolean clientAuth,
  @Nullable Path trustCertificatePath,
  @Nullable Path certificatePath,
  @Nullable Path privateKeyPath
) {

}
