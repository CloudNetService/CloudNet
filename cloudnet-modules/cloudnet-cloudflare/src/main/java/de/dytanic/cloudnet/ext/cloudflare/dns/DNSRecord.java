/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.ext.cloudflare.dns;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class DNSRecord {

    protected String type, name, content;

    protected int ttl;

    protected boolean proxied;

    protected JsonObject data;

    public DNSRecord(String type, String name, String content, int ttl, boolean proxied, JsonObject data) {
        this.type = type;
        this.name = name;
        this.content = content;
        this.ttl = ttl;
        this.proxied = proxied;
        this.data = data;
    }

    public DNSRecord() {
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getContent() {
        return this.content;
    }

    public int getTtl() {
        return this.ttl;
    }

    public boolean isProxied() {
        return this.proxied;
    }

    public JsonObject getData() {
        return this.data;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public void setProxied(boolean proxied) {
        this.proxied = proxied;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

}
