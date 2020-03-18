package de.dytanic.cloudnet.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public class ServiceListCommandEvent extends Event {

    private Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots;
    private Collection<Function<ServiceInfoSnapshot, String>> additionalParameters;
    private Collection<String> additionalSummary;

    public ServiceListCommandEvent(Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots, Collection<Function<ServiceInfoSnapshot, String>> additionalParameters, Collection<String> additionalSummary) {
        this.targetServiceInfoSnapshots = targetServiceInfoSnapshots;
        this.additionalParameters = additionalParameters;
        this.additionalSummary = additionalSummary;
    }

    public ServiceListCommandEvent(Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots) {
        this(targetServiceInfoSnapshots, new ArrayList<>(), new ArrayList<>());
    }

    public Collection<ServiceInfoSnapshot> getTargetServiceInfoSnapshots() {
        return this.targetServiceInfoSnapshots;
    }

    public Collection<Function<ServiceInfoSnapshot, String>> getAdditionalParameters() {
        return this.additionalParameters;
    }

    public Collection<String> getAdditionalSummary() {
        return this.additionalSummary;
    }

    public void addParameter(Function<ServiceInfoSnapshot, String> function) {
        this.additionalParameters.add(function);
    }

    public void addSummaryParameter(String parameter) {
        this.additionalSummary.add(parameter);
    }

}
