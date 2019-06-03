package de.dytanic.cloudnet.driver.network.http;

public interface IMethodHttpHandler extends IHttpHandler {

    @Override
    default void handle(String path, IHttpContext context) throws Exception {
        switch (context.request().method().toUpperCase()) {
            case "GET":
                this.handleGet(path, context);
                break;
            case "POST":
                this.handlePost(path, context);
                break;
            case "PATCH":
                this.handlePatch(path, context);
                break;
            case "PUT":
                this.handlePut(path, context);
                break;
            case "DELETE":
                this.handleDelete(path, context);
                break;
            case "HEAD":
                this.handleHead(path, context);
                break;
            case "TRACE":
                this.handleTrace(path, context);
                break;
            case "OPTIONS":
                this.handleOptions(path, context);
                break;
            case "CONNECT":
                this.handleConnect(path, context);
                break;
        }
    }

    void handlePost(String path, IHttpContext context) throws Exception;

    void handleGet(String path, IHttpContext context) throws Exception;

    void handlePut(String path, IHttpContext context) throws Exception;

    void handleHead(String path, IHttpContext context) throws Exception;

    void handleDelete(String path, IHttpContext context) throws Exception;

    void handlePatch(String path, IHttpContext context) throws Exception;

    void handleTrace(String path, IHttpContext context) throws Exception;

    void handleOptions(String path, IHttpContext context) throws Exception;

    void handleConnect(String path, IHttpContext context) throws Exception;

}