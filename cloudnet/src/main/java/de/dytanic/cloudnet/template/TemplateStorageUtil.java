package de.dytanic.cloudnet.template;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class TemplateStorageUtil {

    private TemplateStorageUtil() {
        throw new UnsupportedOperationException();
    }

    private static void prepareProxyTemplate(SpecificTemplateStorage storage, byte[] buffer, String configPath, String defaultConfigPath) throws IOException {
        try (OutputStream outputStream = storage.newOutputStream(configPath);
             InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream(defaultConfigPath)) {
            if (inputStream != null) {
                FileUtils.copy(inputStream, outputStream, buffer);
            }
        }

        try (OutputStream outputStream = storage.newOutputStream("server-icon.png");
             InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/server-icon.png")) {
            if (inputStream != null) {
                FileUtils.copy(inputStream, outputStream, buffer);
            }
        }
    }

    public static boolean createAndPrepareTemplate(ServiceTemplate template, ServiceEnvironmentType environment) throws IOException {
        return createAndPrepareTemplate(template.storage(), environment);
    }

    public static boolean createAndPrepareTemplate(SpecificTemplateStorage storage, ServiceEnvironmentType environment) throws IOException {
        Preconditions.checkNotNull(storage);
        Preconditions.checkNotNull(environment);

        ServiceTemplate serviceTemplate = storage.getTargetTemplate();

        if (!storage.exists()) {
            storage.create();

            storage.createDirectory("plugins");
            byte[] buffer = new byte[3072];

            switch (environment) {
                case BUNGEECORD: {
                    prepareProxyTemplate(storage, buffer, "config.yml", "files/bungee/config.yml");
                }
                break;
                case WATERDOG: {
                    prepareProxyTemplate(storage, buffer, "config.yml", "files/waterdog/config.yml");
                }
                break;
                case VELOCITY: {
                    prepareProxyTemplate(storage, buffer, "velocity.toml", "files/velocity/velocity.toml");
                }
                break;
                case NUKKIT: {
                    try (OutputStream outputStream = storage.newOutputStream("server.properties");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nukkit/server.properties")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream("nukkit.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nukkit/nukkit.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }
                }
                break;
                case MINECRAFT_SERVER: {
                    try (OutputStream outputStream = storage.newOutputStream("server.properties");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/server.properties")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream("bukkit.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/bukkit.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream("spigot.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/spigot.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream("config/sponge/global.conf");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/global.conf")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }
                }
                break;
                case GLOWSTONE: {
                    try (OutputStream outputStream = storage.newOutputStream("config/glowstone.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/glowstone/glowstone.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }
                }
                break;
            }

            if (storage.getWrappedStorage().shouldSyncInCluster()) {
                try (InputStream inputStream = storage.zipTemplate()) {
                    if (inputStream != null) {
                        CloudNet.getInstance().deployTemplateInCluster(serviceTemplate, inputStream);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }
}