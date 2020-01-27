package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;

public final class EmptyGroupConfiguration extends GroupConfiguration {

    public EmptyGroupConfiguration(String name) {
        super(name);

        super.includes = Iterables.newArrayList();
        super.templates = Iterables.newArrayList();
        super.deployments = Iterables.newArrayList();
    }

}