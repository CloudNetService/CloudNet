/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.netty;

import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import io.netty5.handler.ssl.ClientAuth;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import java.nio.file.Files;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class for all netty based network servers to extends which simplifies creating an ssl context for the
 * current server, if enabled.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public abstract class NettySslServer {

  protected final SSLConfiguration sslConfiguration;

  public SslContext sslContext;

  /**
   * Constructs a new netty ssl server instance.
   *
   * @param sslConfiguration the ssl configuration of the server, or null if disabled.
   */
  public NettySslServer(@Nullable SSLConfiguration sslConfiguration) {
    this.sslConfiguration = sslConfiguration;
  }

  /**
   * Initializes the ssl context based on the given configuration if enabled. If no certificate paths are defined the
   * server will use self-signed certificates.
   *
   * @throws Exception if any exception occurs during reading of the certificates.
   */
  protected void init() throws Exception {
    if (this.sslConfiguration != null && this.sslConfiguration.enabled()) {
      if (this.sslConfiguration.certificatePath() != null && this.sslConfiguration.privateKeyPath() != null) {
        try (var cert = Files.newInputStream(this.sslConfiguration.certificatePath());
          var privateKey = Files.newInputStream(this.sslConfiguration.privateKeyPath())) {
          // begin building the new ssl context building based on the certificate and private key
          var builder = SslContextBuilder.forServer(cert, privateKey);

          // check if a trust certificate was given, if not just trusts all certificates
          if (this.sslConfiguration.trustCertificatePath() != null) {
            try (var stream = Files.newInputStream(this.sslConfiguration.trustCertificatePath())) {
              builder.trustManager(stream);
            }
          } else {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
          }

          // build the context
          this.sslContext = builder
            .clientAuth(this.sslConfiguration.clientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
            .build();
        }
      } else {
        // self-sign a certificate as no certificate was provided
        var selfSignedCertificate = new SelfSignedCertificate();
        this.sslContext = SslContextBuilder
          .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build();
      }
    }
  }
}
