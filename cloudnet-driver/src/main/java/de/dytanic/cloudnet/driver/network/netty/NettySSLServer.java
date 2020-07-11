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
        if (this.sslConfiguration != null) {
            if (this.sslConfiguration.getCertificatePath() != null &&
                    this.sslConfiguration.getPrivateKeyPath() != null) {
                SslContextBuilder builder = SslContextBuilder.forServer(this.sslConfiguration.getCertificatePath(), this.sslConfiguration.getPrivateKeyPath());

                if (this.sslConfiguration.getTrustCertificatePath() != null) {
                    builder.trustManager(this.sslConfiguration.getTrustCertificatePath());
                } else {
                    builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                }

                this.sslContext = builder
                        .clientAuth(this.sslConfiguration.isClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
                        .build();
            } else {
                SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
                this.sslContext = SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            }
        }
    }
}