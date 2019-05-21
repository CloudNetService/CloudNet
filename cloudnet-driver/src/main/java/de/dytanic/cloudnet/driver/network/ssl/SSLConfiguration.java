package de.dytanic.cloudnet.driver.network.ssl;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@Getter
@AllArgsConstructor
public class SSLConfiguration {

    protected final boolean clientAuth;

    protected final File trustCertificatePath, certificatePath, privateKeyPath;

}