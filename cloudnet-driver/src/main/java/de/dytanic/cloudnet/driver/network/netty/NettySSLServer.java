package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

abstract class NettySSLServer {

  protected final SSLConfiguration sslConfiguration;

  protected SslContext sslContext;

  public NettySSLServer(SSLConfiguration sslConfiguration) {
    this.sslConfiguration = sslConfiguration;
  }

  protected void init() throws Exception {
    if (sslConfiguration != null) {
      if (sslConfiguration.getCertificatePath() != null &&
        sslConfiguration.getPrivateKeyPath() != null) {
        SslContextBuilder builder = SslContextBuilder
          .forServer(sslConfiguration.getCertificatePath(),
            sslConfiguration.getPrivateKeyPath());

        if (sslConfiguration.getTrustCertificatePath() != null) {
          builder.trustManager(sslConfiguration.getTrustCertificatePath());
        } else {
          builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }

        this.sslContext = builder
          .clientAuth(sslConfiguration.isClientAuth() ? ClientAuth.REQUIRE
            : ClientAuth.OPTIONAL)
          .build();
      } else {
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        this.sslContext = SslContextBuilder
          .forServer(selfSignedCertificate.certificate(),
            selfSignedCertificate.privateKey())
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build();
      }
    }
  }
}