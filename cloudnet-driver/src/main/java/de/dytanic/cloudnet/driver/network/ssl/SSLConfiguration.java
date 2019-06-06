package de.dytanic.cloudnet.driver.network.ssl;

import java.io.File;

public class SSLConfiguration {

    protected final boolean clientAuth;

    protected final File trustCertificatePath, certificatePath, privateKeyPath;

    public SSLConfiguration(boolean clientAuth, File trustCertificatePath, File certificatePath, File privateKeyPath) {
        this.clientAuth = clientAuth;
        this.trustCertificatePath = trustCertificatePath;
        this.certificatePath = certificatePath;
        this.privateKeyPath = privateKeyPath;
    }

    public boolean isClientAuth() {
        return this.clientAuth;
    }

    public File getTrustCertificatePath() {
        return this.trustCertificatePath;
    }

    public File getCertificatePath() {
        return this.certificatePath;
    }

    public File getPrivateKeyPath() {
        return this.privateKeyPath;
    }
}