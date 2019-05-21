package de.dytanic.cloudnet.driver.network.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpCookie {

    protected String name, value, domain, path;

    protected long maxAge;
}