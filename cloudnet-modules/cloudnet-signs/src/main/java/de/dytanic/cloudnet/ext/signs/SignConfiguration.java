package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

@ToString
@EqualsAndHashCode
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

    public void setConfigurations(Collection<SignConfigurationEntry> configurations) {
        this.configurations = configurations;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

}