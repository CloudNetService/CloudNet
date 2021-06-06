package de.dytanic.cloudnet.ext.cloudflare.dns;

import com.google.gson.JsonObject;

public class DefaultDNSRecord extends DNSRecord {

  public DefaultDNSRecord(DNSType type, String name, String content, JsonObject data) {
    super(type.name(), name, content, 1, false, data);
  }

}
