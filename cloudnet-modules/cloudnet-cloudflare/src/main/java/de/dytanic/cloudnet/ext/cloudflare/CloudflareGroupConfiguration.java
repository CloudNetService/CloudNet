package de.dytanic.cloudnet.ext.cloudflare;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CloudflareGroupConfiguration {

    protected String name, sub;

    protected int priority, weight;

    public CloudflareGroupConfiguration(String name, String sub, int priority, int weight) {
        this.name = name;
        this.sub = sub;
        this.priority = priority;
        this.weight = weight;
    }

    public CloudflareGroupConfiguration() {
    }

    public String getName() {
        return this.name;
    }

    public String getSub() {
        return this.sub;
    }

    public int getPriority() {
        return this.priority;
    }

    public int getWeight() {
        return this.weight;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

}