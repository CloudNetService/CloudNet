package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.SignConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SignsConfiguration implements SerializableObject {

    protected Map<String, String> messages;
    protected Collection<SignConfigurationEntry> configurationEntries;

    public SignsConfiguration(Map<String, String> messages, Collection<SignConfigurationEntry> configurationEntries) {
        this.messages = messages;
        this.configurationEntries = configurationEntries;
    }

    public static SignsConfiguration createDefaultJava(@NotNull String group) {
        return new SignsConfiguration(
                new HashMap<>(ImmutableMap.of(
                        "server-connecting-message", "&7You will be moved to &c%server%&7...",
                        "command-cloudsign-create-success", "&7The target sign with the target group &6%group% &7is successfully created.",
                        "command-cloudsign-remove-success", "&7The target sign will removed! Please wait...",
                        "command-cloudsign-sign-already-exist", "&7The sign is already set. If you want to remove that, use the /cloudsign remove command",
                        "command-cloudsign-cleanup-success", "&7Non-existing signs were removed successfully"
                )),
                new ArrayList<>(Collections.singleton(SignConfigurationType.JAVA.createEntry(group)))
        );
    }

    public static SignsConfiguration createDefaultBedrock(@NotNull String group) {
        return new SignsConfiguration(
                new HashMap<>(ImmutableMap.of(
                        "server-connecting-message", "&7You will be moved to &c%server%&7...",
                        "command-cloudsign-create-success", "&7The target sign with the target group &6%group% &7is successfully created.",
                        "command-cloudsign-remove-success", "&7The target sign will removed! Please wait...",
                        "command-cloudsign-sign-already-exist", "&7The sign is already set. If you want to remove that, use the /cloudsign remove command",
                        "command-cloudsign-cleanup-success", "&7Non-existing signs were removed successfully"
                )),
                new ArrayList<>(Collections.singleton(SignConfigurationType.BEDROCK.createEntry(group)))
        );
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public Collection<SignConfigurationEntry> getConfigurationEntries() {
        return configurationEntries;
    }

    public void setConfigurationEntries(Collection<SignConfigurationEntry> configurationEntries) {
        this.configurationEntries = configurationEntries;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeStringMap(this.messages);
        buffer.writeObjectCollection(this.configurationEntries);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.messages = buffer.readStringMap();
        this.configurationEntries = buffer.readObjectCollection(SignConfigurationEntry.class);
    }
}
