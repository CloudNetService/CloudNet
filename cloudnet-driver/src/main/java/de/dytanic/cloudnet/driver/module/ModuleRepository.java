package de.dytanic.cloudnet.driver.module;

public class ModuleRepository {

    private String name, url;

    public ModuleRepository(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public ModuleRepository() {
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ModuleRepository)) return false;
        final ModuleRepository other = (ModuleRepository) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$url = this.getUrl();
        final Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ModuleRepository;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        return result;
    }

    public String toString() {
        return "ModuleRepository(name=" + this.getName() + ", url=" + this.getUrl() + ")";
    }
}