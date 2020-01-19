package de.dytanic.cloudnet.eula;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class MinecraftEULA {

    private static final Path DEFAULT_PATH = Paths.get("eula.txt");

    private final Path path;

    public MinecraftEULA() {
        this(DEFAULT_PATH);
    }

    public MinecraftEULA(Path path) {
        this.path = path;
    }

    public boolean isAccepted() {
        if (!Files.exists(this.path)) {
            this.setAccepted(false);
            return false;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(this.path)) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Boolean.parseBoolean(properties.getProperty("eula"));
    }

    public void setAccepted(boolean accepted) {
        Properties properties = new Properties();
        properties.setProperty("eula", String.valueOf(accepted));
        try (OutputStream outputStream = Files.newOutputStream(this.path)) {
            properties.store(outputStream, "By changing the setting below to TRUE you are indicating your agreement to Mojang's EULA (https://account.mojang.com/documents/minecraft_eula).");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
