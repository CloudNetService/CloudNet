package de.dytanic.cloudnet.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public class ServiceListCommandEvent extends Event {

    private Collection<Function<ServiceInfoSnapshot, String>> additionalParameters = new ArrayList<>();

    public ServiceListCommandEvent(Collection<Function<ServiceInfoSnapshot, String>> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public Collection<Function<ServiceInfoSnapshot, String>> getAdditionalParameters() {
        return this.additionalParameters;
    }

    public void addParameter(Function<ServiceInfoSnapshot, String> function) {
        this.additionalParameters.add(function);
    }

}
