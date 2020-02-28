package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.driver.network.http.IHttpContext;

import java.util.HashMap;
import java.util.Map;

public class ReportHandlers {

    private static final Map<String, ReportHandler> HANDLERS = new HashMap<>();

    static {
        HANDLERS.put("index.html", new IndexHandler());
        HANDLERS.put("ssl.html", new SSLHandler());
        HANDLERS.put("modules.html", new ModuleHandler());
        HANDLERS.put("services.html", new ServicesHandler());
        HANDLERS.put("tasks.html", new TasksHandler());
        HANDLERS.put("nodelog.html", new NodeLogHandler());
        // TODO Add groups.html with a list of all groups
    }

    public static String loadTypeContents(String type, IHttpContext context) {
        ReportHandler handler = HANDLERS.get(type);
        return handler != null ? handler.load(context) : null;
    }

}
