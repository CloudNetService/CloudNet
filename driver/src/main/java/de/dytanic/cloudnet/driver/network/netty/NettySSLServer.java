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

package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.nio.file.Files;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public abstract class NettySSLServer {

  protected final SSLConfiguration sslConfiguration;

  public SslContext sslContext;

  public NettySSLServer(SSLConfiguration sslConfiguration) {
    this.sslConfiguration = sslConfiguration;
  }

  protected void init() throws Exception {
    if (this.sslConfiguration != null && this.sslConfiguration.enabled()) {
      if (this.sslConfiguration.certificatePath() != null && this.sslConfiguration.privateKeyPath() != null) {
        try (var cert = Files.newInputStream(this.sslConfiguration.certificatePath());
          var privateKey = Files.newInputStream(this.sslConfiguration.privateKeyPath())) {
          var builder = SslContextBuilder.forServer(cert, privateKey);

          if (this.sslConfiguration.trustCertificatePath() != null) {
            try (var stream = Files.newInputStream(this.sslConfiguration.trustCertificatePath())) {
              builder.trustManager(stream);
            }
          } else {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
          }

          this.sslContext = builder
            .clientAuth(this.sslConfiguration.clientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
            .build();
        }
      } else {
        var selfSignedCertificate = new SelfSignedCertificate();
        this.sslContext = SslContextBuilder
          .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build();
      }
    }
  }
}
