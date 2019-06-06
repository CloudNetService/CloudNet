package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;

import java.io.File;

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
                clientAuth,
                trustCertificatePath == null ? null : new File(trustCertificatePath),
                certificatePath == null ? null : new File(certificatePath),
                privateKeyPath == null ? null : new File(privateKeyPath)
        );
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isClientAuth() {
        return this.clientAuth;
    }

    public String getTrustCertificatePath() {
        return this.trustCertificatePath;
    }

    public String getCertificatePath() {
        return this.certificatePath;
    }

    public String getPrivateKeyPath() {
        return this.privateKeyPath;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    public void setTrustCertificatePath(String trustCertificatePath) {
        this.trustCertificatePath = trustCertificatePath;
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ConfigurationOptionSSL)) return false;
        final ConfigurationOptionSSL other = (ConfigurationOptionSSL) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isEnabled() != other.isEnabled()) return false;
        if (this.isClientAuth() != other.isClientAuth()) return false;
        final Object this$trustCertificatePath = this.getTrustCertificatePath();
        final Object other$trustCertificatePath = other.getTrustCertificatePath();
        if (this$trustCertificatePath == null ? other$trustCertificatePath != null : !this$trustCertificatePath.equals(other$trustCertificatePath))
            return false;
        final Object this$certificatePath = this.getCertificatePath();
        final Object other$certificatePath = other.getCertificatePath();
        if (this$certificatePath == null ? other$certificatePath != null : !this$certificatePath.equals(other$certificatePath))
            return false;
        final Object this$privateKeyPath = this.getPrivateKeyPath();
        final Object other$privateKeyPath = other.getPrivateKeyPath();
        if (this$privateKeyPath == null ? other$privateKeyPath != null : !this$privateKeyPath.equals(other$privateKeyPath))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ConfigurationOptionSSL;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isEnabled() ? 79 : 97);
        result = result * PRIME + (this.isClientAuth() ? 79 : 97);
        final Object $trustCertificatePath = this.getTrustCertificatePath();
        result = result * PRIME + ($trustCertificatePath == null ? 43 : $trustCertificatePath.hashCode());
        final Object $certificatePath = this.getCertificatePath();
        result = result * PRIME + ($certificatePath == null ? 43 : $certificatePath.hashCode());
        final Object $privateKeyPath = this.getPrivateKeyPath();
        result = result * PRIME + ($privateKeyPath == null ? 43 : $privateKeyPath.hashCode());
        return result;
    }

    public String toString() {
        return "ConfigurationOptionSSL(enabled=" + this.isEnabled() + ", clientAuth=" + this.isClientAuth() + ", trustCertificatePath=" + this.getTrustCertificatePath() + ", certificatePath=" + this.getCertificatePath() + ", privateKeyPath=" + this.getPrivateKeyPath() + ")";
    }
}