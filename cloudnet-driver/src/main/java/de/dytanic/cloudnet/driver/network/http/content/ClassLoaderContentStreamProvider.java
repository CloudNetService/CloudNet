package de.dytanic.cloudnet.driver.network.http.content;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.util.FileMimeTypeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

final class ClassLoaderContentStreamProvider implements ContentStreamProvider {

    private final String pathPrefix;
    private final ClassLoader contentSource;

    public ClassLoaderContentStreamProvider(String pathPrefix, ClassLoader contentSource) {
        this.pathPrefix = pathPrefix;
        this.contentSource = contentSource;
    }

    @Override
    public @Nullable StreamableContent provideContent(@NotNull String path) {
        String resourceLocation = this.pathPrefix + path;
        Preconditions.checkArgument(!resourceLocation.contains(".."), "File traversal for path " + path);

        URL contentLocationUrl = this.contentSource.getResource(resourceLocation);
        return contentLocationUrl == null
                ? null
                : new URLStreamableContent(FileMimeTypeHelper.getFileType(resourceLocation), contentLocationUrl);
    }

    private static final class URLStreamableContent implements StreamableContent {

        private final String contentType;
        private final URL contentLocationUrl;

        public URLStreamableContent(String contentType, URL contentLocationUrl) {
            this.contentType = contentType + "; charset=UTF-8";
            this.contentLocationUrl = contentLocationUrl;
        }

        @Override
        public @NotNull InputStream openStream() throws IOException {
            return this.contentLocationUrl.openStream();
        }

        @Override
        public @NotNull String contentType() {
            return this.contentType;
        }
    }
}
