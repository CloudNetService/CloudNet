package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.conf.ConfigurationOptionSSL;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;

import java.util.HashMap;
import java.util.Map;

public class SSLHandler extends ReportHandler {
    @Override
    public String load(IHttpContext context) {
        Map<String, Object> replacements = new HashMap<>();

        Map<String, ConfigurationOptionSSL> sslConfigs = new HashMap<>();
        sslConfigs.put("server", CloudNet.getInstance().getConfig().getServerSslConfig());
        sslConfigs.put("client", CloudNet.getInstance().getConfig().getClientSslConfig());
        sslConfigs.put("web", CloudNet.getInstance().getConfig().getWebSslConfig());

        sslConfigs.forEach((key, value) -> {
            String prefix = "node.ssl." + key;
            replacements.put(prefix + ".enabled", value.isEnabled() ? "yes" : "no");
            replacements.put(prefix + ".clientAuth", value.isClientAuth() ? "yes" : "no");
            replacements.put(prefix + ".trustCertificatePath", value.getTrustCertificatePath());
            replacements.put(prefix + ".certificatePath", value.getCertificatePath());
            replacements.put(prefix + ".privateKeyPath", value.getPrivateKeyPath());
        });

        String file = super.loadFile("ssl.html");
        return super.replace(file, replacements);
    }
}
