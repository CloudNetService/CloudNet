package de.dytanic.cloudnet.driver.network.http;

public class RedirectHttpHandler extends MethodHttpHandlerAdapter {

    private String redirectResponse;

    public RedirectHttpHandler(String redirectResponse) {
        this.redirectResponse = redirectResponse;
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        context.response()
                .statusCode(301)
                .header("Location", this.redirectResponse)
                .body(" ")
                .context()
                .cancelNext();
    }
}
