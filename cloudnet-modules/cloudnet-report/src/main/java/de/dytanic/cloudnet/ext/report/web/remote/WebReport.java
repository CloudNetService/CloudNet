package de.dytanic.cloudnet.ext.report.web.remote;

import java.util.Map;

public class WebReport {

    private Map<String, Map<String, String>> replacements;

    public WebReport(Map<String, Map<String, String>> replacements) {
        this.replacements = replacements;
    }

    public Map<String, Map<String, String>> getReplacements() {
        return this.replacements;
    }
}
