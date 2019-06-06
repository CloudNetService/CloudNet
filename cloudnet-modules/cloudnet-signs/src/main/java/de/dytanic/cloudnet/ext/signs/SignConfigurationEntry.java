package de.dytanic.cloudnet.ext.signs;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@ToString
@EqualsAndHashCode
public class SignConfigurationEntry {

    protected String targetGroup;

    protected boolean switchToSearchingWhenServiceIsFull;

    protected Collection<SignConfigurationTaskEntry> taskLayouts;

    protected SignLayout defaultOnlineLayout, defaultEmptyLayout, defaultFullLayout;

    protected SignLayoutConfiguration startingLayouts, searchLayouts;

    public SignConfigurationEntry(String targetGroup, boolean switchToSearchingWhenServiceIsFull, Collection<SignConfigurationTaskEntry> taskLayouts, SignLayout defaultOnlineLayout, SignLayout defaultEmptyLayout, SignLayout defaultFullLayout, SignLayoutConfiguration startingLayouts, SignLayoutConfiguration searchLayouts) {
        this.targetGroup = targetGroup;
        this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
        this.taskLayouts = taskLayouts;
        this.defaultOnlineLayout = defaultOnlineLayout;
        this.defaultEmptyLayout = defaultEmptyLayout;
        this.defaultFullLayout = defaultFullLayout;
        this.startingLayouts = startingLayouts;
        this.searchLayouts = searchLayouts;
    }

    public SignConfigurationEntry() {
    }

    public String getTargetGroup() {
        return this.targetGroup;
    }

    public boolean isSwitchToSearchingWhenServiceIsFull() {
        return this.switchToSearchingWhenServiceIsFull;
    }

    public Collection<SignConfigurationTaskEntry> getTaskLayouts() {
        return this.taskLayouts;
    }

    public SignLayout getDefaultOnlineLayout() {
        return this.defaultOnlineLayout;
    }

    public SignLayout getDefaultEmptyLayout() {
        return this.defaultEmptyLayout;
    }

    public SignLayout getDefaultFullLayout() {
        return this.defaultFullLayout;
    }

    public SignLayoutConfiguration getStartingLayouts() {
        return this.startingLayouts;
    }

    public SignLayoutConfiguration getSearchLayouts() {
        return this.searchLayouts;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public void setSwitchToSearchingWhenServiceIsFull(boolean switchToSearchingWhenServiceIsFull) {
        this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
    }

    public void setTaskLayouts(Collection<SignConfigurationTaskEntry> taskLayouts) {
        this.taskLayouts = taskLayouts;
    }

    public void setDefaultOnlineLayout(SignLayout defaultOnlineLayout) {
        this.defaultOnlineLayout = defaultOnlineLayout;
    }

    public void setDefaultEmptyLayout(SignLayout defaultEmptyLayout) {
        this.defaultEmptyLayout = defaultEmptyLayout;
    }

    public void setDefaultFullLayout(SignLayout defaultFullLayout) {
        this.defaultFullLayout = defaultFullLayout;
    }

    public void setStartingLayouts(SignLayoutConfiguration startingLayouts) {
        this.startingLayouts = startingLayouts;
    }

    public void setSearchLayouts(SignLayoutConfiguration searchLayouts) {
        this.searchLayouts = searchLayouts;
    }

}