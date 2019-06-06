package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class SignConfiguration {

    public static final Type TYPE = new TypeToken<SignConfiguration>() {
    }.getType();

    protected Collection<SignConfigurationEntry> configurations;

    protected Map<String, String> messages;

    public SignConfiguration(Collection<SignConfigurationEntry> configurations, Map<String, String> messages) {
        this.configurations = configurations;
        this.messages = messages;
    }

    public SignConfiguration() {
    }

    public Collection<SignConfigurationEntry> getConfigurations() {
        return this.configurations;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setConfigurations(Collection<SignConfigurationEntry> configurations) {
        this.configurations = configurations;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SignConfiguration)) return false;
        final SignConfiguration other = (SignConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$configurations = this.getConfigurations();
        final Object other$configurations = other.getConfigurations();
        if (this$configurations == null ? other$configurations != null : !this$configurations.equals(other$configurations))
            return false;
        final Object this$messages = this.getMessages();
        final Object other$messages = other.getMessages();
        if (this$messages == null ? other$messages != null : !this$messages.equals(other$messages)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SignConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $configurations = this.getConfigurations();
        result = result * PRIME + ($configurations == null ? 43 : $configurations.hashCode());
        final Object $messages = this.getMessages();
        result = result * PRIME + ($messages == null ? 43 : $messages.hashCode());
        return result;
    }

    public String toString() {
        return "SignConfiguration(configurations=" + this.getConfigurations() + ", messages=" + this.getMessages() + ")";
    }
}