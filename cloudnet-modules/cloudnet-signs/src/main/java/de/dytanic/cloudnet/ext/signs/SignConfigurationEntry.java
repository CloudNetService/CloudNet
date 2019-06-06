package de.dytanic.cloudnet.ext.signs;

import java.util.Collection;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SignConfigurationEntry)) return false;
        final SignConfigurationEntry other = (SignConfigurationEntry) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$targetGroup = this.getTargetGroup();
        final Object other$targetGroup = other.getTargetGroup();
        if (this$targetGroup == null ? other$targetGroup != null : !this$targetGroup.equals(other$targetGroup))
            return false;
        if (this.isSwitchToSearchingWhenServiceIsFull() != other.isSwitchToSearchingWhenServiceIsFull()) return false;
        final Object this$taskLayouts = this.getTaskLayouts();
        final Object other$taskLayouts = other.getTaskLayouts();
        if (this$taskLayouts == null ? other$taskLayouts != null : !this$taskLayouts.equals(other$taskLayouts))
            return false;
        final Object this$defaultOnlineLayout = this.getDefaultOnlineLayout();
        final Object other$defaultOnlineLayout = other.getDefaultOnlineLayout();
        if (this$defaultOnlineLayout == null ? other$defaultOnlineLayout != null : !this$defaultOnlineLayout.equals(other$defaultOnlineLayout))
            return false;
        final Object this$defaultEmptyLayout = this.getDefaultEmptyLayout();
        final Object other$defaultEmptyLayout = other.getDefaultEmptyLayout();
        if (this$defaultEmptyLayout == null ? other$defaultEmptyLayout != null : !this$defaultEmptyLayout.equals(other$defaultEmptyLayout))
            return false;
        final Object this$defaultFullLayout = this.getDefaultFullLayout();
        final Object other$defaultFullLayout = other.getDefaultFullLayout();
        if (this$defaultFullLayout == null ? other$defaultFullLayout != null : !this$defaultFullLayout.equals(other$defaultFullLayout))
            return false;
        final Object this$startingLayouts = this.getStartingLayouts();
        final Object other$startingLayouts = other.getStartingLayouts();
        if (this$startingLayouts == null ? other$startingLayouts != null : !this$startingLayouts.equals(other$startingLayouts))
            return false;
        final Object this$searchLayouts = this.getSearchLayouts();
        final Object other$searchLayouts = other.getSearchLayouts();
        if (this$searchLayouts == null ? other$searchLayouts != null : !this$searchLayouts.equals(other$searchLayouts))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SignConfigurationEntry;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $targetGroup = this.getTargetGroup();
        result = result * PRIME + ($targetGroup == null ? 43 : $targetGroup.hashCode());
        result = result * PRIME + (this.isSwitchToSearchingWhenServiceIsFull() ? 79 : 97);
        final Object $taskLayouts = this.getTaskLayouts();
        result = result * PRIME + ($taskLayouts == null ? 43 : $taskLayouts.hashCode());
        final Object $defaultOnlineLayout = this.getDefaultOnlineLayout();
        result = result * PRIME + ($defaultOnlineLayout == null ? 43 : $defaultOnlineLayout.hashCode());
        final Object $defaultEmptyLayout = this.getDefaultEmptyLayout();
        result = result * PRIME + ($defaultEmptyLayout == null ? 43 : $defaultEmptyLayout.hashCode());
        final Object $defaultFullLayout = this.getDefaultFullLayout();
        result = result * PRIME + ($defaultFullLayout == null ? 43 : $defaultFullLayout.hashCode());
        final Object $startingLayouts = this.getStartingLayouts();
        result = result * PRIME + ($startingLayouts == null ? 43 : $startingLayouts.hashCode());
        final Object $searchLayouts = this.getSearchLayouts();
        result = result * PRIME + ($searchLayouts == null ? 43 : $searchLayouts.hashCode());
        return result;
    }

    public String toString() {
        return "SignConfigurationEntry(targetGroup=" + this.getTargetGroup() + ", switchToSearchingWhenServiceIsFull=" + this.isSwitchToSearchingWhenServiceIsFull() + ", taskLayouts=" + this.getTaskLayouts() + ", defaultOnlineLayout=" + this.getDefaultOnlineLayout() + ", defaultEmptyLayout=" + this.getDefaultEmptyLayout() + ", defaultFullLayout=" + this.getDefaultFullLayout() + ", startingLayouts=" + this.getStartingLayouts() + ", searchLayouts=" + this.getSearchLayouts() + ")";
    }
}