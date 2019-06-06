package de.dytanic.cloudnet.driver.network.http;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
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

}