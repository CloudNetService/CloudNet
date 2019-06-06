package de.dytanic.cloudnet.launcher.util;

public final class CloudNetModule {

    protected final String name;

    protected final String fileName;

    public CloudNetModule(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
    }

    public String getName() {
        return this.name;
    }

    public String getFileName() {
        return this.fileName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CloudNetModule)) return false;
        final CloudNetModule other = (CloudNetModule) o;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$fileName = this.getFileName();
        final Object other$fileName = other.getFileName();
        if (this$fileName == null ? other$fileName != null : !this$fileName.equals(other$fileName)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $fileName = this.getFileName();
        result = result * PRIME + ($fileName == null ? 43 : $fileName.hashCode());
        return result;
    }

    public String toString() {
        return "CloudNetModule(name=" + this.getName() + ", fileName=" + this.getFileName() + ")";
    }
}