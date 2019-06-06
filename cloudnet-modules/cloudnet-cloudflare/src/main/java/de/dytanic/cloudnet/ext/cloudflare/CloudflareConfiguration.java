package de.dytanic.cloudnet.ext.cloudflare;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CloudflareConfiguration)) return false;
        final CloudflareConfiguration other = (CloudflareConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$entries = this.getEntries();
        final Object other$entries = other.getEntries();
        if (this$entries == null ? other$entries != null : !this$entries.equals(other$entries)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CloudflareConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $entries = this.getEntries();
        result = result * PRIME + ($entries == null ? 43 : $entries.hashCode());
        return result;
    }

    public String toString() {
        return "CloudflareConfiguration(entries=" + this.getEntries() + ")";
    }
}