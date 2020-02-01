package de.dytanic.cloudnet.ext.syncproxy;

import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SyncProxyConfigurationWriterAndReader {

    private static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>(ImmutableMap.of(
            "player-login-not-whitelisted", "&cThe network is currently in maintenance!",
            "player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network",
            "service-start", "&7The service &e%service% &7is &astarting...",
            "service-stop", "&7The service &e%service% &7is &cstopping..."
    ));

    private SyncProxyConfigurationWriterAndReader() {
        throw new UnsupportedOperationException();
    }

    public static void write(SyncProxyConfiguration syncProxyConfiguration, File file) {
        Validate.checkNotNull(syncProxyConfiguration);
        Validate.checkNotNull(file);

        file.getParentFile().mkdirs();
        new JsonDocument("config", syncProxyConfiguration).write(file);
    }

    public static SyncProxyConfiguration read(File file) {
        Validate.checkNotNull(file);

        JsonDocument document = JsonDocument.newDocument(file);
        if (document == null || !document.contains("config")) {
            write(new SyncProxyConfiguration(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    DEFAULT_MESSAGES,
                    true
            ), file);
            document = JsonDocument.newDocument(file);
        }

        SyncProxyConfiguration configuration = document.get("config", SyncProxyConfiguration.TYPE);
        if (configuration.getMessages() != null) {
            boolean edit = false;
            for (Map.Entry<String, String> entry : DEFAULT_MESSAGES.entrySet()) {
                if (!configuration.getMessages().containsKey(entry.getKey())) {
                    configuration.getMessages().put(entry.getKey(), entry.getValue());
                    edit = true;
                }
            }
            if (edit) {
                write(configuration, file);
            }
        } else {
            configuration.setMessages(new HashMap<>(DEFAULT_MESSAGES));
            write(configuration, file);
        }
        return configuration;
    }

    public static SyncProxyTabListConfiguration createDefaultTabListConfiguration(String targetGroup) {
        return new SyncProxyTabListConfiguration(
                targetGroup,
                Collections.singletonList(
                        new SyncProxyTabList(
                                " \n&b&lCloud&f&lNet &6Eruption &8■ &7next &bgeneration &7network &8➜ &f%online_players%&8/&f%max_players%&f\n &8► &7Current server &8● &b%server% &8◄ \n ",
                                " \n &7Sponsored by &8» &fEU-Hosting.ch &8▎ &7Discord &8» &fdiscord.gg/UNQ4wET \n &7powered by &8» &b&b&lCloud&f&lNet \n "
                        )
                ),
                1
        );
    }

    public static SyncProxyProxyLoginConfiguration createDefaultLoginConfiguration(String targetGroup) {
        return new SyncProxyProxyLoginConfiguration(
                targetGroup,
                false,
                100,
                new ArrayList<>(),
                Collections.singletonList(new SyncProxyMotd(
                        "&b&lCloud&f&lNet &6Eruption &8■ &7next &bgeneration &7cloud system",
                        "&7Sponsored by &8» &bEU-Hosting.ch &8▎ &8» &c%proxy%",
                        false,
                        1,
                        new String[]{
                                " ",
                                "&b&lCloud&f&lNet &8× &7your &bfree &7cloudsystem",
                                "&7Sponsored by &8» &bEU-Hosting.ch",
                                "&7Discord &8» &fdiscord.gg/UNQ4wET",
                                " "
                        },
                        null
                )),
                Collections.singletonList(new SyncProxyMotd(
                        "&b&lCloud&f&lNet &6Eruption &8■ &7next &bgeneration &7cloud system",
                        "      &bMaintenance &8» &7We are still in &bmaintenance",
                        false,
                        1,
                        new String[]{
                                " ",
                                "&b&lCloud&f&lNet &8× &7your &bfree &7cloudsystem",
                                "&7Discord &8» &fdiscord.gg/UNQ4wET",
                                " "
                        },
                        "&8➜ &bMaintenance &8&l【&c✘&8&l】"
                ))
        );
    }

}
