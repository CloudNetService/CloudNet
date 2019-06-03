package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationOptionSSL {

    private boolean enabled, clientAuth;

    private String trustCertificatePath, certificatePath, privateKeyPath;

    public SSLConfiguration toSslConfiguration() {
        return new SSLConfiguration(
                clientAuth,
                trustCertificatePath == null ? null : new File(trustCertificatePath),
                certificatePath == null ? null : new File(certificatePath),
                privateKeyPath == null ? null : new File(privateKeyPath)
        );
    }
}