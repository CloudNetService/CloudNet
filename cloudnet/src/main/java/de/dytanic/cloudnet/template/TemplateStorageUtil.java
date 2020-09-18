package de.dytanic.cloudnet.template;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class TemplateStorageUtil {

    private TemplateStorageUtil() {
        throw new UnsupportedOperationException();
    }

    public static LocalTemplateStorage getLocalTemplateStorage() {
        return (LocalTemplateStorage) CloudNet.getInstance().getServicesRegistry()
                .getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);
    }

    public static File getFile(ServiceTemplate serviceTemplate, String path) {
        return new File(getLocalTemplateStorage().getStorageDirectory() + "/" + serviceTemplate.getTemplatePath(), path);
    }

    private static void prepareProxyTemplate(ITemplateStorage storage, ServiceTemplate template, byte[] buffer, String configPath, String defaultConfigPath) throws IOException {
        try (OutputStream outputStream = storage.newOutputStream(template, configPath);
             InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream(defaultConfigPath)) {
            if (inputStream != null) {
                FileUtils.copy(inputStream, outputStream, buffer);
            }
        }

        try (OutputStream outputStream = storage.newOutputStream(template, "server-icon.png");
             InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/server-icon.png")) {
            if (inputStream != null) {
                FileUtils.copy(inputStream, outputStream, buffer);
            }
        }
    }

    public static boolean createAndPrepareTemplate(ITemplateStorage storage, String prefix, String name, ServiceEnvironmentType environment) throws IOException {
        Preconditions.checkNotNull(storage);
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(environment);

        ServiceTemplate serviceTemplate = new ServiceTemplate(prefix, name, storage.getName());

        if (!storage.has(serviceTemplate)) {
            storage.create(serviceTemplate);

            storage.createDirectory(serviceTemplate, "plugins");
            byte[] buffer = new byte[3072];

            switch (environment) {
                case BUNGEECORD: {
                    prepareProxyTemplate(storage, serviceTemplate, buffer, "config.yml", "files/bungee/config.yml");
                }
                break;
                case WATERDOG: {
                    prepareProxyTemplate(storage, serviceTemplate, buffer, "config.yml", "files/waterdog/config.yml");
                }
                break;
                case VELOCITY: {
                    prepareProxyTemplate(storage, serviceTemplate, buffer, "velocity.toml", "files/velocity/velocity.toml");
                }
                break;
                case NUKKIT: {
                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "server.properties");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nukkit/server.properties")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "nukkit.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nukkit/nukkit.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }
                }
                break;
                case GO_MINT: {
                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "server.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/gomint/server.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }
                }
                break;
                case MINECRAFT_SERVER: {
                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "server.properties");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/server.properties")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "bukkit.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/bukkit.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "spigot.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/spigot.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "config/sponge/global.conf");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/global.conf")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }
                }
                break;
                case GLOWSTONE: {
                    try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, "config/glowstone.yml");
                         InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("files/glowstone/glowstone.yml")) {
                        if (inputStream != null) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }
                }
                break;
            }

            if (storage.shouldSyncInCluster()) {
                CloudNet.getInstance().deployTemplateInCluster(serviceTemplate, storage.zipTemplate(serviceTemplate));
            }
            return true;
        } else {
            return false;
        }
    }
}