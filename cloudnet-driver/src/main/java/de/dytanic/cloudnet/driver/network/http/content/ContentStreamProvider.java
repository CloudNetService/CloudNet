package de.dytanic.cloudnet.driver.network.http.content;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@FunctionalInterface
public interface ContentStreamProvider {

    static @NotNull ContentStreamProvider allOf(@NonNls ContentStreamProvider... providers) {
        return new MultipleContentStreamProvider(providers);
    }

    static @NotNull ContentStreamProvider fileTree(@NotNull Path path) {
        return new FileContentStreamProvider(path);
    }

    static @NotNull ContentStreamProvider classLoader(@NotNull ClassLoader classLoader) {
        return new ClassLoaderContentStreamProvider("", classLoader);
    }

    static @NotNull ContentStreamProvider classLoader(@NotNull ClassLoader classLoader, @NotNull String pathPrefix) {
        return new ClassLoaderContentStreamProvider(pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/", classLoader);
    }

    @Nullable StreamableContent provideContent(@NotNull String path);

    interface StreamableContent {

        @NotNull InputStream openStream() throws IOException;

        @NotNull String contentType();
    }
}
