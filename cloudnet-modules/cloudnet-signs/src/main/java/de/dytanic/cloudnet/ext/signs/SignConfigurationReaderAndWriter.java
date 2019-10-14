package de.dytanic.cloudnet.ext.signs;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.util.Arrays;
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
                    Collections.singletonList(new SignConfigurationEntry(
                            "Lobby",
                            true,
                            Collections.singletonList(new SignConfigurationTaskEntry(
                                    "Template_Group_Layout",
                                    new SignLayout(
                                            new String[]{
                                                    "&eLobby &0- &e%task_id%",
                                                    "&8[&eLOBBY&8]",
                                                    "%online_players% / %max_players%",
                                                    "%motd%"
                                            },
                                            "GOLD_BLOCK",
                                            0
                                    ),
                                    new SignLayout(
                                            new String[]{
                                                    "&7Lobby &0- &7%task_id%",
                                                    "&8[&7LOBBY&8]",
                                                    "%online_players% / %max_players%",
                                                    "%motd%"
                                            },
                                            "GOLD_BLOCK",
                                            0
                                    ),
                                    new SignLayout(
                                            new String[]{
                                                    "&6Lobby &0- &6%task_id%",
                                                    "&8[&6PRIME&8]",
                                                    "%online_players% / %max_players%",
                                                    "%motd%"
                                            },
                                            "EMERALD_BLOCK",
                                            0
                                    )
                            )),
                            new SignLayout(
                                    new String[]{
                                            "-* %name% *-",
                                            "&8[&eLOBBY&8]",
                                            "%online_players% / %max_players%",
                                            "%motd%"
                                    },
                                    "GOLD_BLOCK",
                                    0
                            ),
                            new SignLayout(
                                    new String[]{
                                            "-* %name% *-",
                                            "&8[&7LOBBY&8]",
                                            "%online_players% / %max_players%",
                                            "%motd%"
                                    },
                                    "GOLD_BLOCK",
                                    0
                            ),
                            new SignLayout(
                                    new String[]{
                                            "-* %name% *-",
                                            "&8[&6PRIME&8]",
                                            "%online_players% / %max_players%",
                                            "%motd%"
                                    },
                                    "EMERALD_BLOCK",
                                    0
                            ),
                            new SignLayoutConfiguration(
                                    Arrays.asList(
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Server",
                                                            "starting",
                                                            ""
                                                    },
                                                    "BEDROCK",
                                                    0
                                            ),
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Server",
                                                            "starting .",
                                                            ""
                                                    },
                                                    "BEDROCK",
                                                    0
                                            ),
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Server",
                                                            "starting ..",
                                                            ""
                                                    },
                                                    "BEDROCK",
                                                    0
                                            ),
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Server",
                                                            "starting ...",
                                                            ""
                                                    },
                                                    "BEDROCK",
                                                    0
                                            )
                                    ),
                                    2
                            ),
                            new SignLayoutConfiguration(
                                    Arrays.asList(
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Waiting for",
                                                            "server",
                                                            ""
                                                    },
                                                    "REDSTONE_BLOCK",
                                                    0
                                            ),
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Waiting for",
                                                            "server .",
                                                            ""
                                                    },
                                                    "REDSTONE_BLOCK",
                                                    0
                                            ),
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Waiting for",
                                                            "server ..",
                                                            ""
                                                    },
                                                    "REDSTONE_BLOCK",
                                                    0
                                            ),
                                            new SignLayout(
                                                    new String[]{
                                                            "",
                                                            "Waiting for",
                                                            "server ...",
                                                            ""
                                                    },
                                                    "REDSTONE_BLOCK",
                                                    0
                                            )
                                    ),
                                    2
                            )
                    )),
                    Maps.of(
                            new Pair<>("server-connecting-message", "&7You will send to &c%server%&7..."),
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