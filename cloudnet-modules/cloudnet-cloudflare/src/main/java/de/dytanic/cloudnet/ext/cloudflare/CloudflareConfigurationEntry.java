package de.dytanic.cloudnet.ext.cloudflare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudflareConfigurationEntry {

    protected boolean enabled;

    protected String hostAddress, email, apiToken, zoneId, domainName;

    protected Collection<CloudflareGroupConfiguration> groups;

}