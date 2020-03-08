package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.driver.network.http.IHttpContext;

import java.util.Collection;
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
        HANDLERS.put("groups.html", new GroupsHandler());
    }

    public static String loadTypeContents(String file) {
        ReportHandler handler = HANDLERS.get(file);
        return handler != null ? handler.replace(handler.loadFile(file), handler.load()) : null;
    }

    public static Map<String, Object> loadReplacements(String file) {
        ReportHandler handler = HANDLERS.get(file);
        return handler != null ? handler.load() : null;
    }

    public static Collection<String> getFiles() {
        return HANDLERS.keySet();
    }

}
