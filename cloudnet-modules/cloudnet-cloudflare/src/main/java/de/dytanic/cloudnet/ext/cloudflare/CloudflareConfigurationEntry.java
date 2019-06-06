package de.dytanic.cloudnet.ext.cloudflare;

import java.util.Collection;

public class CloudflareConfigurationEntry {

    protected boolean enabled;

    protected String hostAddress, email, apiToken, zoneId, domainName;

    protected Collection<CloudflareGroupConfiguration> groups;

    public CloudflareConfigurationEntry(boolean enabled, String hostAddress, String email, String apiToken, String zoneId, String domainName, Collection<CloudflareGroupConfiguration> groups) {
        this.enabled = enabled;
        this.hostAddress = hostAddress;
        this.email = email;
        this.apiToken = apiToken;
        this.zoneId = zoneId;
        this.domainName = domainName;
        this.groups = groups;
    }

    public CloudflareConfigurationEntry() {
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getHostAddress() {
        return this.hostAddress;
    }

    public String getEmail() {
        return this.email;
    }

    public String getApiToken() {
        return this.apiToken;
    }

    public String getZoneId() {
        return this.zoneId;
    }

    public String getDomainName() {
        return this.domainName;
    }

    public Collection<CloudflareGroupConfiguration> getGroups() {
        return this.groups;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setGroups(Collection<CloudflareGroupConfiguration> groups) {
        this.groups = groups;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CloudflareConfigurationEntry)) return false;
        final CloudflareConfigurationEntry other = (CloudflareConfigurationEntry) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isEnabled() != other.isEnabled()) return false;
        final Object this$hostAddress = this.getHostAddress();
        final Object other$hostAddress = other.getHostAddress();
        if (this$hostAddress == null ? other$hostAddress != null : !this$hostAddress.equals(other$hostAddress))
            return false;
        final Object this$email = this.getEmail();
        final Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final Object this$apiToken = this.getApiToken();
        final Object other$apiToken = other.getApiToken();
        if (this$apiToken == null ? other$apiToken != null : !this$apiToken.equals(other$apiToken)) return false;
        final Object this$zoneId = this.getZoneId();
        final Object other$zoneId = other.getZoneId();
        if (this$zoneId == null ? other$zoneId != null : !this$zoneId.equals(other$zoneId)) return false;
        final Object this$domainName = this.getDomainName();
        final Object other$domainName = other.getDomainName();
        if (this$domainName == null ? other$domainName != null : !this$domainName.equals(other$domainName))
            return false;
        final Object this$groups = this.getGroups();
        final Object other$groups = other.getGroups();
        if (this$groups == null ? other$groups != null : !this$groups.equals(other$groups)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CloudflareConfigurationEntry;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isEnabled() ? 79 : 97);
        final Object $hostAddress = this.getHostAddress();
        result = result * PRIME + ($hostAddress == null ? 43 : $hostAddress.hashCode());
        final Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final Object $apiToken = this.getApiToken();
        result = result * PRIME + ($apiToken == null ? 43 : $apiToken.hashCode());
        final Object $zoneId = this.getZoneId();
        result = result * PRIME + ($zoneId == null ? 43 : $zoneId.hashCode());
        final Object $domainName = this.getDomainName();
        result = result * PRIME + ($domainName == null ? 43 : $domainName.hashCode());
        final Object $groups = this.getGroups();
        result = result * PRIME + ($groups == null ? 43 : $groups.hashCode());
        return result;
    }

    public String toString() {
        return "CloudflareConfigurationEntry(enabled=" + this.isEnabled() + ", hostAddress=" + this.getHostAddress() + ", email=" + this.getEmail() + ", apiToken=" + this.getApiToken() + ", zoneId=" + this.getZoneId() + ", domainName=" + this.getDomainName() + ", groups=" + this.getGroups() + ")";
    }
}