package de.dytanic.cloudnet.ext.cloudflare;

import java.util.Collection;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class CloudflareConfigurationEntry {

  protected boolean enabled;

  protected AuthenticationMethod authenticationMethod = AuthenticationMethod.GLOBAL_KEY;

  protected String hostAddress;
  protected String email;
  protected String apiToken;
  protected String zoneId;
  protected String domainName;

  protected Collection<CloudflareGroupConfiguration> groups;

  public CloudflareConfigurationEntry(boolean enabled, String hostAddress, String email, String apiToken, String zoneId,
    String domainName, Collection<CloudflareGroupConfiguration> groups) {
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

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public AuthenticationMethod getAuthenticationMethod() {
    return authenticationMethod;
  }

  public void setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
  }

  public String getHostAddress() {
    return this.hostAddress;
  }

  public void setHostAddress(String hostAddress) {
    this.hostAddress = hostAddress;
  }

  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getApiToken() {
    return this.apiToken;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public String getZoneId() {
    return this.zoneId;
  }

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }

  public String getDomainName() {
    return this.domainName;
  }

  public void setDomainName(String domainName) {
    this.domainName = domainName;
  }

  public Collection<CloudflareGroupConfiguration> getGroups() {
    return this.groups;
  }

  public void setGroups(Collection<CloudflareGroupConfiguration> groups) {
    this.groups = groups;
  }

  @Override
  public String toString() {
    return "CloudflareConfigurationEntry(enabled=" + this.isEnabled()
      + ", authenticationMethod=" + this.getAuthenticationMethod()
      + ", hostAddress=" + this.getHostAddress()
      + ", domainName=" + this.getDomainName()
      + ", groups=" + this.getGroups()
      + ")";
  }

  public enum AuthenticationMethod {
    GLOBAL_KEY,
    BEARER_TOKEN
  }
}
