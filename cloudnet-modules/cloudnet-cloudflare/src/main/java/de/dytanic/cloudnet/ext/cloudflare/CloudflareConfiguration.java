package de.dytanic.cloudnet.ext.cloudflare;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CloudflareConfiguration {

  public static final Type TYPE = new TypeToken<CloudflareConfiguration>() {
  }.getType();

  protected Collection<CloudflareConfigurationEntry> entries;

  public CloudflareConfiguration(Collection<CloudflareConfigurationEntry> entries) {
    this.entries = entries;
  }

  public CloudflareConfiguration() {
  }

  public Collection<CloudflareConfigurationEntry> getEntries() {
    return this.entries;
  }

  public void setEntries(Collection<CloudflareConfigurationEntry> entries) {
    this.entries = entries;
  }

}
