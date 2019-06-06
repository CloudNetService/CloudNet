package de.dytanic.cloudnet.ext.cloudflare;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CloudflareGroupConfiguration)) return false;
        final CloudflareGroupConfiguration other = (CloudflareGroupConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$sub = this.getSub();
        final Object other$sub = other.getSub();
        if (this$sub == null ? other$sub != null : !this$sub.equals(other$sub)) return false;
        if (this.getPriority() != other.getPriority()) return false;
        if (this.getWeight() != other.getWeight()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CloudflareGroupConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $sub = this.getSub();
        result = result * PRIME + ($sub == null ? 43 : $sub.hashCode());
        result = result * PRIME + this.getPriority();
        result = result * PRIME + this.getWeight();
        return result;
    }

    public String toString() {
        return "CloudflareGroupConfiguration(name=" + this.getName() + ", sub=" + this.getSub() + ", priority=" + this.getPriority() + ", weight=" + this.getWeight() + ")";
    }
}