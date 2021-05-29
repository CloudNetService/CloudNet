package de.dytanic.cloudnet.driver.network.http.content;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class MultipleContentStreamProvider implements ContentStreamProvider {

    private final ContentStreamProvider[] streamProviders;

    public MultipleContentStreamProvider(ContentStreamProvider... streamProviders) {
        this.streamProviders = streamProviders;
    }

    @Override
    public @Nullable StreamableContent provideContent(@NotNull String path) {
        for (ContentStreamProvider provider : this.streamProviders) {
            StreamableContent content = provider.provideContent(path);
            if (content != null) {
                return content;
            }
        }
        return null;
    }
}
