package de.dytanic.cloudnet.driver.network.ssl;

import java.io.File;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SSLConfiguration {

  protected final boolean clientAuth;

  protected final File trustCertificatePath, certificatePath, privateKeyPath;

}