/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    return this.authenticationMethod;
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
