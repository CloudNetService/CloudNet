package de.dytanic.cloudnet.common.document;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This interface is interesting to persistence data, of the implement object
 */
public interface IPersistable {

    IPersistable write(OutputStream outputStream);

    IPersistable write(Writer writer);


    default IPersistable write(Path path) {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (OutputStream outputStream = new FileOutputStream(path.toFile())) {
            this.write(outputStream);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return this;
    }

    default IPersistable write(String path) {
        if (path == null) {
            return this;
        }

        return this.write(Paths.get(path));
    }

    default IPersistable write(String... paths) {
        if (paths == null) {
            return this;
        }
        for (String path : paths) {
            this.write(path);
        }

        return this;
    }

    default IPersistable write(File file) {
        if (file == null) {
            return this;
        }

        return this.write(file.toPath());
    }

    default IPersistable write(File... files) {
        if (files == null) {
            return this;
        }
        for (File file : files) {
            this.write(file);
        }

        return this;
    }

    default IPersistable write(Path... paths) {
        if (paths == null) {
            return this;
        }
        for (Path path : paths) {
            this.write(path);
        }

        return this;
    }
}