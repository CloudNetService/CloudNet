/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.ext.cloudflare.dns;

import com.google.gson.JsonObject;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DNSRecord)) return false;
        final DNSRecord other = (DNSRecord) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$content = this.getContent();
        final Object other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
        if (this.getTtl() != other.getTtl()) return false;
        if (this.isProxied() != other.isProxied()) return false;
        final Object this$data = this.getData();
        final Object other$data = other.getData();
        if (this$data == null ? other$data != null : !this$data.equals(other$data)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof DNSRecord;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        result = result * PRIME + this.getTtl();
        result = result * PRIME + (this.isProxied() ? 79 : 97);
        final Object $data = this.getData();
        result = result * PRIME + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    public String toString() {
        return "DNSRecord(type=" + this.getType() + ", name=" + this.getName() + ", content=" + this.getContent() + ", ttl=" + this.getTtl() + ", proxied=" + this.isProxied() + ", data=" + this.getData() + ")";
    }
}
