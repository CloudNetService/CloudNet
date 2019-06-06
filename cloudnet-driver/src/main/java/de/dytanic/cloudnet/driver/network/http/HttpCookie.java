package de.dytanic.cloudnet.driver.network.http;

public class HttpCookie {

    protected String name, value, domain, path;

    protected long maxAge;

    public HttpCookie(String name, String value, String domain, String path, long maxAge) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.maxAge = maxAge;
    }

    public HttpCookie() {
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public String getDomain() {
        return this.domain;
    }

    public String getPath() {
        return this.path;
    }

    public long getMaxAge() {
        return this.maxAge;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HttpCookie)) return false;
        final HttpCookie other = (HttpCookie) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        final Object this$domain = this.getDomain();
        final Object other$domain = other.getDomain();
        if (this$domain == null ? other$domain != null : !this$domain.equals(other$domain)) return false;
        final Object this$path = this.getPath();
        final Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        if (this.getMaxAge() != other.getMaxAge()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HttpCookie;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        final Object $domain = this.getDomain();
        result = result * PRIME + ($domain == null ? 43 : $domain.hashCode());
        final Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final long $maxAge = this.getMaxAge();
        result = result * PRIME + (int) ($maxAge >>> 32 ^ $maxAge);
        return result;
    }

    public String toString() {
        return "HttpCookie(name=" + this.getName() + ", value=" + this.getValue() + ", domain=" + this.getDomain() + ", path=" + this.getPath() + ", maxAge=" + this.getMaxAge() + ")";
    }
}