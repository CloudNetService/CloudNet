package de.dytanic.cloudnet.driver.network.http;

public interface MethodHttpComponent<T extends MethodHttpComponent<?>> extends IHttpComponent<T>, AutoCloseable {

    String GLOBAL_PATH = "/*";
    
    default T registerHandler(String path, String method, IHttpHandler handler) {
        return this.registerHandler(path, (handlerPath, context) -> {
            if (method == null || context.request().method().equalsIgnoreCase(method)) {
                handler.handle(handlerPath, context);
            }
        });
    }

    default T registerGlobalHandler(String method, IHttpHandler handler) {
        return this.registerHandler(GLOBAL_PATH, method, handler);
    }

    default T registerHandler(String path, String method, int priority, IHttpHandler handler) {
        return this.registerHandler(path, priority, (handlerPath, context) -> {
            if (method == null || context.request().method().equalsIgnoreCase(method)) {
                handler.handle(handlerPath, context);
            }
        });
    }

    default T registerGlobalHandler(String method, int priority, IHttpHandler handler) {
        return this.registerHandler(GLOBAL_PATH, method, priority, handler);
    }

    default T registerHandler(IHttpHandler handler) {
        return this.registerHandler(GLOBAL_PATH, null, handler);
    }

    default T before(String path, IHttpHandler handler) {
        return this.registerHandler(path, IHttpHandler.PRIORITY_HIGH, handler);
    }

    default T after(String path, IHttpHandler handler) {
        return this.registerHandler(path, IHttpHandler.PRIORITY_LOWEST, handler);
    }

    default T before(String path, String method, IHttpHandler handler) {
        return this.registerHandler(path, method, IHttpHandler.PRIORITY_HIGH, handler);
    }

    default T after(String path, String method, IHttpHandler handler) {
        return this.registerHandler(path, method, IHttpHandler.PRIORITY_LOWEST, handler);
    }

    default T beforeGlobal(IHttpHandler handler) {
        return this.before(GLOBAL_PATH, handler);
    }

    default T afterGlobal(IHttpHandler handler) {
        return this.before(GLOBAL_PATH, handler);
    }

    default T beforeGlobal(String method, IHttpHandler handler) {
        return this.before(GLOBAL_PATH, method, handler);
    }

    default T afterGlobal(String method, IHttpHandler handler) {
        return this.after(GLOBAL_PATH, method, handler);
    }

    default T before(IHttpHandler handler) {
        return this.registerHandler(GLOBAL_PATH, IHttpHandler.PRIORITY_HIGH, handler);
    }

    default T after(IHttpHandler handler) {
        return this.registerHandler(GLOBAL_PATH, IHttpHandler.PRIORITY_LOWEST, handler);
    }

    default T get(String path, IHttpHandler handler) {
        return this.registerHandler(path, "GET", handler);
    }

    default T getGlobal(IHttpHandler handler) {
        return this.get(GLOBAL_PATH, handler);
    }

    default T post(String path, IHttpHandler handler) {
        return this.registerHandler(path, "POST", handler);
    }

    default T postGlobal(IHttpHandler handler) {
        return this.post(GLOBAL_PATH, handler);
    }

    default T redirect(String path, String target) {
        return this.registerHandler(path, IHttpHandler.PRIORITY_HIGH, new RedirectHttpHandler(target));
    }
}
