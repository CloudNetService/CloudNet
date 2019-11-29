package de.dytanic.cloudnet.ext.signs.configuration;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntryType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public final class SignConfigurationReaderAndWriter {

    private SignConfigurationReaderAndWriter() {
        throw new UnsupportedOperationException();
    }

    public static void write(SignConfiguration signConfiguration, File file) {
        Validate.checkNotNull(signConfiguration);
        Validate.checkNotNull(file);

        file.getParentFile().mkdirs();
        new JsonDocument("config", signConfiguration).write(file);
    }

    public static SignConfiguration read(File file) {
        Validate.checkNotNull(file);

        JsonDocument document = JsonDocument.newDocument(file);

        if (!document.contains("config")) {
            SignConfiguration signConfiguration = new SignConfiguration(
                    new ArrayList<>(Collections.singletonList(SignConfigurationEntryType.BUKKIT.createEntry("Lobby"))),
                    Maps.of(
                            new Pair<>("server-connecting-message", "&7You will be send to &c%server%&7..."),
                            new Pair<>("command-cloudsign-create-success", "&7The target sign with the target group &6%group% &7is successfully created."),
                            new Pair<>("command-cloudsign-remove-success", "&7The target sign will removed! Please wait..."),
                            new Pair<>("command-cloudsign-sign-already-exist", "&7The sign is already set. If you want to remove that, use the /cloudsign remove command")
                    )
            );

            write(signConfiguration, file);
            return signConfiguration;
        }

        SignConfiguration signConfiguration = document.get("config", SignConfiguration.TYPE);

        // new properties in the configuration will be saved
        document.append("config", signConfiguration);
        document.write(file);

        return signConfiguration;
    }
}