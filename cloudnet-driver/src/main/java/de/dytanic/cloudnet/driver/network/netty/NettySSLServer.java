package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.InputStream;
import java.nio.file.Files;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class NettySSLServer {

  protected final SSLConfiguration sslConfiguration;

  public SslContext sslContext;

  public NettySSLServer(SSLConfiguration sslConfiguration) {
    this.sslConfiguration = sslConfiguration;
  }

  protected void init() throws Exception {
    if (this.sslConfiguration != null) {
      if (this.sslConfiguration.getCertificate() != null && this.sslConfiguration.getPrivateKey() != null) {
        try (InputStream cert = Files.newInputStream(this.sslConfiguration.getCertificate());
          InputStream privateKey = Files.newInputStream(this.sslConfiguration.getPrivateKey())) {
          SslContextBuilder builder = SslContextBuilder.forServer(cert, privateKey);

          if (this.sslConfiguration.getTrustCertificate() != null) {
            try (InputStream stream = Files.newInputStream(this.sslConfiguration.getTrustCertificate())) {
              builder.trustManager(stream);
            }
          } else {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
          }

          this.sslContext = builder
            .clientAuth(this.sslConfiguration.isClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
            .build();
        }
      } else {
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        this.sslContext = SslContextBuilder
          .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build();
      }
    }
  }
}
