package de.dytanic.cloudnet.driver.network.http;

public interface MethodHttpHandler extends HttpHandler {

    @Override
    default void handle(String path, HttpContext context) throws Exception {
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

    void handlePost(String path, HttpContext context) throws Exception;

    void handleGet(String path, HttpContext context) throws Exception;

    void handlePut(String path, HttpContext context);

    void handleHead(String path, HttpContext context);

    void handleDelete(String path, HttpContext context);

    void handlePatch(String path, HttpContext context);

    void handleTrace(String path, HttpContext context);

    void handleOptions(String path, HttpContext context);

    void handleConnect(String path, HttpContext context);

}