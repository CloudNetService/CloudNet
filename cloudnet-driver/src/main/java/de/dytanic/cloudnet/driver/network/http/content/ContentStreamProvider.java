package de.dytanic.cloudnet.driver.network.http.content;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface ContentStreamProvider {

    @Nullable StreamableContent provideStream(@NotNull String path);

    interface StreamableContent {

        @NotNull InputStream openStream() throws IOException;

        @NotNull String contentType();
    }
}
