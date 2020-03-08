package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class ReportHandler {

    public abstract Map<String, Object> load();

    protected final String loadFile(String path) {
        try (InputStream inputStream = CloudNetReportModule.class.getClassLoader().getResourceAsStream("web/" + path)) {
            if (inputStream == null) {
                return null;
            }

            return new String(FileUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    protected final String replace(String input, Map<String, Object> replacements) {
        for (Map.Entry<String, Object> entry : replacements.entrySet()) {
            String value = entry.getValue() == null || (entry.getValue() instanceof String && ((String) entry.getValue()).isEmpty()) ?
                    "not available" :
                    String.valueOf(entry.getValue());
            input = input.replace("${" + entry.getKey() + "}", value);
        }
        return input;
    }

}
