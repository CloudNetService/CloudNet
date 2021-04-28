package de.dytanic.cloudnet.ext.cloudflare.dns;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareGroupConfiguration;

/**
 * A representation of an SRV DNS record
 */
public class SRVRecord extends DNSRecord {

    public SRVRecord(String name, String content, String service, String proto, String name_, int priority, int weight, int port, String target) {
        super(
                DNSType.SRV.name(),
                name,
                content,
                1,
                false,
                new JsonDocument()
                        .append("service", service)
                        .append("proto", proto)
                        .append("name", name_)
                        .append("priority", priority)
                        .append("weight", weight)
                        .append("port", port)
                        .append("target", target)
                        .toJsonObject()
        );
    }

    public static SRVRecord forConfiguration(CloudflareConfigurationEntry entry, CloudflareGroupConfiguration configuration, int port) {
        return new SRVRecord(
                String.format("_minecraft._tcp.%s", entry.getDomainName()),
                String.format(
                        "SRV %s %s %s %s.%s",
                        configuration.getPriority(),
                        configuration.getWeight(),
                        port,
                        CloudNet.getInstance().getConfig().getIdentity().getUniqueId(),
                        entry.getDomainName()
                ),
                "_minecraft",
                "_tcp",
                configuration.getSub().equals("@") ? entry.getDomainName() : configuration.getSub(),
                configuration.getPriority(),
                configuration.getWeight(),
                port,
                CloudNet.getInstance().getConfig().getIdentity().getUniqueId() + "." + entry.getDomainName()
        );
    }
}
