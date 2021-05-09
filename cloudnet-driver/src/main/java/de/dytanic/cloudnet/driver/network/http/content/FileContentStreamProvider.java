package de.dytanic.cloudnet.driver.network.http.content;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.util.FileMimeTypeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileContentStreamProvider implements ContentStreamProvider {

    private final Path workingDirectory;

    public FileContentStreamProvider(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public @Nullable StreamableContent provideStream(@NotNull String path) {
        Path targetPath = this.workingDirectory.resolve(path);
        if (Files.notExists(targetPath) || Files.isDirectory(targetPath)) {
            return null;
        } else {
            FileUtils.ensureChild(this.workingDirectory, targetPath);
            return new FileStreamableContent(targetPath);
        }
    }

    private static final class FileStreamableContent implements StreamableContent {

        private final Path path;
        private final String contentType;

        public FileStreamableContent(Path path) {
            this.path = path;
            this.contentType = FileMimeTypeHelper.getFileType(path) + "; charset=UTF-8";
        }

        @Override
        public @NotNull InputStream openStream() throws IOException {
            return Files.newInputStream(this.path);
        }

        @Override
        public @NotNull String contentType() {
            return this.contentType;
        }
    }
}
