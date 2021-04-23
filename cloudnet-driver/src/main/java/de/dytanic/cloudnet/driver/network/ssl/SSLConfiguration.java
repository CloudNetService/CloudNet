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