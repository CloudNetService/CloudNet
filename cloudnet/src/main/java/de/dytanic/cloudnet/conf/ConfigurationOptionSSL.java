package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.File;

@ToString
@EqualsAndHashCode
public class ConfigurationOptionSSL {

    private boolean enabled, clientAuth;

    private String trustCertificatePath, certificatePath, privateKeyPath;

    public ConfigurationOptionSSL(boolean enabled, boolean clientAuth, String trustCertificatePath, String certificatePath, String privateKeyPath) {
        this.enabled = enabled;
        this.clientAuth = clientAuth;
        this.trustCertificatePath = trustCertificatePath;
        this.certificatePath = certificatePath;
        this.privateKeyPath = privateKeyPath;
    }

    public ConfigurationOptionSSL() {
    }

    public SSLConfiguration toSslConfiguration() {
        return new SSLConfiguration(
                this.clientAuth,
                this.trustCertificatePath == null ? null : new File(this.trustCertificatePath),
                this.certificatePath == null ? null : new File(this.certificatePath),
                this.privateKeyPath == null ? null : new File(this.privateKeyPath)
        );
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isClientAuth() {
        return this.clientAuth;
    }

    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    public String getTrustCertificatePath() {
        return this.trustCertificatePath;
    }

    public void setTrustCertificatePath(String trustCertificatePath) {
        this.trustCertificatePath = trustCertificatePath;
    }

    public String getCertificatePath() {
        return this.certificatePath;
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    public String getPrivateKeyPath() {
        return this.privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

}