package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.ICloudServiceManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

public abstract class DefaultMinecraftCloudService extends DefaultTemplateCloudService {
    public DefaultMinecraftCloudService(String runtime, ICloudServiceManager cloudServiceManager, ServiceConfiguration serviceConfiguration) {
        super(runtime, cloudServiceManager, serviceConfiguration);
    }

    @Override
    protected void preStart() {
        super.preStart();
        try {
            this.configureServiceEnvironment();
        } catch (IOException exception) {
            throw new Error(exception);
        }
    }

    private void rewriteBungeeConfig(File config) throws IOException {
        this.rewriteServiceConfigurationFile(config, line -> {
            if (line.startsWith("    host: ")) {
                line = "    host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration().getPort();
            } else if (line.startsWith("  host: ")) {
                line = "  host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration().getPort();
            }

            return line;
        });
    }

    private void configureServiceEnvironment() throws IOException {
        switch (this.getServiceConfiguration().getProcessConfig().getEnvironment()) {
            case BUNGEECORD: {
                File file = new File(this.getDirectory(), "config.yml");
                this.copyDefaultFile("files/bungee/config.yml", file);

                this.rewriteBungeeConfig(file);
            }
            break;
            case WATERDOG: {
                File file = new File(this.getDirectory(), "config.yml");
                this.copyDefaultFile("files/waterdog/config.yml", file);

                this.rewriteBungeeConfig(file);
            }
            break;
            case VELOCITY: {
                File file = new File(this.getDirectory(), "velocity.toml");
                this.copyDefaultFile("files/velocity/velocity.toml", file);

                AtomicBoolean reference = new AtomicBoolean(true);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (reference.get() && line.startsWith("bind =")) {
                        reference.set(false);
                        return "bind = \"" + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration().getPort() + "\"";
                    }

                    return line;
                });
            }
            break;
            case MINECRAFT_SERVER: {
                File file = new File(this.getDirectory(), "server.properties");
                this.copyDefaultFile("files/nms/server.properties", file);

                Properties properties = new Properties();

                try (InputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }

                properties.setProperty("server-name", this.getServiceId().getName());
                properties.setProperty("server-port", String.valueOf(this.getServiceConfiguration().getPort()));
                properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Edit by CloudNet");
                }

                properties = new Properties();

                file = new File(this.getDirectory(), "eula.txt");
                if (file.exists() || file.createNewFile()) {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        properties.load(inputStream);
                    }
                }

                properties.setProperty("eula", "true");

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(outputStreamWriter, "Auto Eula agreement by CloudNet");
                }
            }
            break;
            case NUKKIT: {
                File file = new File(this.getDirectory(), "server.properties");
                this.copyDefaultFile("files/nukkit/server.properties", file);

                Properties properties = new Properties();

                try (InputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }

                properties.setProperty("server-port", String.valueOf(this.getServiceConfiguration().getPort()));
                properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Edit by CloudNet");
                }
            }
            break;
            case GLOWSTONE: {
                File file = new File(this.getDirectory(), "config/glowstone.yml");
                file.getParentFile().mkdirs();

                this.copyDefaultFile("files/glowstone/glowstone.yml", file);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (line.startsWith("    ip: ")) {
                        line = "    ip: '" + CloudNet.getInstance().getConfig().getHostAddress() + "'";
                    }

                    if (line.startsWith("    port: ")) {
                        line = "    port: " + this.getServiceConfiguration().getPort();
                    }

                    return line;
                });
            }
            break;
            default:
                break;
        }
    }

    private void copyDefaultFile(String from, File target) throws IOException {
        if (!target.exists() && target.createNewFile()) {
            try (InputStream inputStream = JVMCloudService.class.getClassLoader().getResourceAsStream(from);
                 OutputStream outputStream = new FileOutputStream(target)) {
                if (inputStream != null) {
                    FileUtils.copy(inputStream, outputStream);
                }
            }
        }
    }

    private void rewriteServiceConfigurationFile(File file, UnaryOperator<String> unaryOperator) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        List<String> replacedLines = new ArrayList<>(lines.size());

        for (String line : lines) {
            replacedLines.add(unaryOperator.apply(line));
        }

        try (OutputStream outputStream = new FileOutputStream(file);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             PrintWriter printWriter = new PrintWriter(outputStreamWriter, true)) {
            for (String replacedLine : replacedLines) {
                printWriter.write(replacedLine + "\n");
                printWriter.flush();
            }
        }
    }

    protected void postConfigureServiceEnvironmentStartParameters(List<String> commandArguments) {
        switch (this.getServiceConfiguration().getProcessConfig().getEnvironment()) {
            case MINECRAFT_SERVER:
                commandArguments.add("nogui");
                break;
            case NUKKIT:
                commandArguments.add("disable-ansi");
                break;
        }
    }

}
