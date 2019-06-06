package de.dytanic.cloudnet.ext.signs;

public class SignConfigurationTaskEntry {

    protected String task;

    protected SignLayout onlineLayout, emptyLayout, fullLayout;

    public SignConfigurationTaskEntry(String task, SignLayout onlineLayout, SignLayout emptyLayout, SignLayout fullLayout) {
        this.task = task;
        this.onlineLayout = onlineLayout;
        this.emptyLayout = emptyLayout;
        this.fullLayout = fullLayout;
    }

    public SignConfigurationTaskEntry() {
    }

    public String getTask() {
        return this.task;
    }

    public SignLayout getOnlineLayout() {
        return this.onlineLayout;
    }

    public SignLayout getEmptyLayout() {
        return this.emptyLayout;
    }

    public SignLayout getFullLayout() {
        return this.fullLayout;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setOnlineLayout(SignLayout onlineLayout) {
        this.onlineLayout = onlineLayout;
    }

    public void setEmptyLayout(SignLayout emptyLayout) {
        this.emptyLayout = emptyLayout;
    }

    public void setFullLayout(SignLayout fullLayout) {
        this.fullLayout = fullLayout;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SignConfigurationTaskEntry)) return false;
        final SignConfigurationTaskEntry other = (SignConfigurationTaskEntry) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$task = this.getTask();
        final Object other$task = other.getTask();
        if (this$task == null ? other$task != null : !this$task.equals(other$task)) return false;
        final Object this$onlineLayout = this.getOnlineLayout();
        final Object other$onlineLayout = other.getOnlineLayout();
        if (this$onlineLayout == null ? other$onlineLayout != null : !this$onlineLayout.equals(other$onlineLayout))
            return false;
        final Object this$emptyLayout = this.getEmptyLayout();
        final Object other$emptyLayout = other.getEmptyLayout();
        if (this$emptyLayout == null ? other$emptyLayout != null : !this$emptyLayout.equals(other$emptyLayout))
            return false;
        final Object this$fullLayout = this.getFullLayout();
        final Object other$fullLayout = other.getFullLayout();
        if (this$fullLayout == null ? other$fullLayout != null : !this$fullLayout.equals(other$fullLayout))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SignConfigurationTaskEntry;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $task = this.getTask();
        result = result * PRIME + ($task == null ? 43 : $task.hashCode());
        final Object $onlineLayout = this.getOnlineLayout();
        result = result * PRIME + ($onlineLayout == null ? 43 : $onlineLayout.hashCode());
        final Object $emptyLayout = this.getEmptyLayout();
        result = result * PRIME + ($emptyLayout == null ? 43 : $emptyLayout.hashCode());
        final Object $fullLayout = this.getFullLayout();
        result = result * PRIME + ($fullLayout == null ? 43 : $fullLayout.hashCode());
        return result;
    }

    public String toString() {
        return "SignConfigurationTaskEntry(task=" + this.getTask() + ", onlineLayout=" + this.getOnlineLayout() + ", emptyLayout=" + this.getEmptyLayout() + ", fullLayout=" + this.getFullLayout() + ")";
    }
}