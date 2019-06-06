package de.dytanic.cloudnet.ext.signs;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
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

}