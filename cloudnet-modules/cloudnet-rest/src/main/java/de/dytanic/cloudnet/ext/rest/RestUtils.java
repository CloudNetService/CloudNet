package de.dytanic.cloudnet.ext.rest;

import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;

import java.util.ArrayList;

public class RestUtils {

    public static void replaceNulls(ServiceConfigurationBase configuration) {
        if (configuration.getTemplates() == null) {
            configuration.setTemplates(new ArrayList<>());
        }
        if (configuration.getIncludes() == null) {
            configuration.setIncludes(new ArrayList<>());
        }
        if (configuration.getDeployments() == null) {
            configuration.setDeployments(new ArrayList<>());
        }
    }

}
