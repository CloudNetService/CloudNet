package de.dytanic.cloudnet.ext.cloudflare;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CloudflareGroupConfiguration {

    protected String name;
    protected String sub;

    protected int priority;
    protected int weight;

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

    public void setName(String name) {
        this.name = name;
    }

    public String getSub() {
        return this.sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getWeight() {
        return this.weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

}