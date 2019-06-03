/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.ext.cloudflare.dns;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DNSRecord {

    protected String type, name, content;

    protected int ttl;

    protected boolean proxied;

    protected JsonObject data;

}
