package de.dytanic.cloudnet.ext.cloudflare.cloudflare;

import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSRecord;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class DnsRecordDetail {

    private final String id;
    private final DNSRecord dnsRecord;
    private final CloudflareConfigurationEntry configurationEntry;

    public DnsRecordDetail(String id, DNSRecord dnsRecord, CloudflareConfigurationEntry configurationEntry) {
        this.id = id;
        this.dnsRecord = dnsRecord;
        this.configurationEntry = configurationEntry;
    }

    public String getId() {
        return this.id;
    }

    public DNSRecord getDnsRecord() {
        return this.dnsRecord;
    }

    public CloudflareConfigurationEntry getConfigurationEntry() {
        return this.configurationEntry;
    }
}
