package de.dytanic.cloudnet.ext.signs.configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntryType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public final class SignConfigurationReaderAndWriter {

    private SignConfigurationReaderAndWriter() {
        throw new UnsupportedOperationException();
    }

    public static void write(SignConfiguration signConfiguration, File file) {
        Preconditions.checkNotNull(signConfiguration);
        Preconditions.checkNotNull(file);

        file.getParentFile().mkdirs();
        new JsonDocument("config", signConfiguration).write(file);
    }

    public static SignConfiguration read(File file) {
        Preconditions.checkNotNull(file);

        JsonDocument document = JsonDocument.newDocument(file);

        if (!document.contains("config")) {
            SignConfiguration signConfiguration = new SignConfiguration(
                    new ArrayList<>(Collections.singletonList(SignConfigurationEntryType.BUKKIT.createEntry("Lobby"))),
                    new HashMap<>(ImmutableMap.of(
                            "server-connecting-message", "&7You will be send to &c%server%&7...",
                            "command-cloudsign-create-success", "&7The target sign with the target group &6%group% &7is successfully created.",
                            "command-cloudsign-remove-success", "&7The target sign will removed! Please wait...",
                            "command-cloudsign-sign-already-exist", "&7The sign is already set. If you want to remove that, use the /cloudsign remove command",
                            "command-cloudsign-cleanup-success", "&7Non-existing signs were removed successfully"
                    ))
            );

            write(signConfiguration, file);
            return signConfiguration;
        }

        SignConfiguration signConfiguration = document.get("config", SignConfiguration.TYPE);

        if (!signConfiguration.getMessages().containsKey("command-cloudsign-cleanup-success")) {
            signConfiguration.getMessages().put("command-cloudsign-cleanup-success", "&7Non-existing signs were removed successfully");
        }

        // new properties in the configuration will be saved
        document.append("config", signConfiguration);
        document.write(file);

        return signConfiguration;
    }
}