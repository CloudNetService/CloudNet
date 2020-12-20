package de.dytanic.cloudnet.ext.syncproxy.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SyncProxyConfigurationWriterAndReader {

    private static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>(ImmutableMap.of(
            "player-login-not-whitelisted", "&cThe network is currently in maintenance!",
            "player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network",
            "service-start", "&7The service &e%service% &7is &astarting &7on node &e%node%&7...",
            "service-stop", "&7The service &e%service% &7is &cstopping &7on node &e%node%&7..."
    ));

    private SyncProxyConfigurationWriterAndReader() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static void write(SyncProxyConfiguration syncProxyConfiguration, File file) {
        write(syncProxyConfiguration, file.toPath());
    }

    public static void write(SyncProxyConfiguration syncProxyConfiguration, Path file) {
        Preconditions.checkNotNull(syncProxyConfiguration);
        Preconditions.checkNotNull(file);

        JsonDocument.newDocument("config", syncProxyConfiguration).write(file);
    }

    @Deprecated
    public static SyncProxyConfiguration read(File file) {
        return read(file.toPath());
    }

    public static SyncProxyConfiguration read(Path file) {
        Preconditions.checkNotNull(file);

        JsonDocument document = JsonDocument.newDocument(file);
        if (!document.contains("config")) {
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
                                " \n&b&lCloud&f&lNet &6Earthquake &8■ &7next &bgeneration &7network &8➜ &f%online_players%&8/&f%max_players%&f\n &8► &7Current server &8● &b%server% &8◄ \n ",
                                " \n &7Sponsored by &8» &fOpusX.io &8▎ &7Discord &8» &fdiscord.gg/CPCWr7w \n &7powered by &8» &b&b&lCloud&f&lNet \n "
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
                        "&b&lCloud&f&lNet &6Earthquake &8■ &7next &bgeneration &7cloud system",
                        "&7Sponsored by &8» &bOpusX.io &8▎ &8» &c%proxy%",
                        false,
                        1,
                        new String[]{
                                " ",
                                "&b&lCloud&f&lNet &8× &7your &bfree &7cloudsystem",
                                "&7Sponsored by &8» &bOpusX.io",
                                "&7Discord &8» &fdiscord.gg/CPCWr7w",
                                " "
                        },
                        null
                )),
                Collections.singletonList(new SyncProxyMotd(
                        "&b&lCloud&f&lNet &6Earthquake &8■ &7next &bgeneration &7cloud system",
                        "      &bMaintenance &8» &7We are still in &bmaintenance",
                        false,
                        1,
                        new String[]{
                                " ",
                                "&b&lCloud&f&lNet &8× &7your &bfree &7cloudsystem",
                                "&7Discord &8» &fdiscord.gg/CPCWr7w",
                                " "
                        },
                        "&8➜ &bMaintenance &8&l【&c✘&8&l】"
                ))
        );
    }

}
