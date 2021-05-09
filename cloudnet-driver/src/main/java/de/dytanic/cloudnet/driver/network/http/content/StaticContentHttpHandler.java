package de.dytanic.cloudnet.driver.network.http.content;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;

public class StaticContentHttpHandler implements IHttpHandler {

    private final ContentStreamProvider provider;

    public StaticContentHttpHandler(ContentStreamProvider provider) {
        this.provider = provider;
    }

    @Override
    public void handle(String path, IHttpContext context) throws Exception {
        path = path.replaceFirst(context.pathPrefix(), "");
        if (path.endsWith("/") || path.isEmpty()) {
            path += "index.html";
        }

        ContentStreamProvider.StreamableContent content = this.provider.provideStream(path);
        if (content != null) {
            context
                    .closeAfter(true)
                    .cancelNext(true)
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", content.contentType())
                    .body(content.openStream());
        }
    }
}
