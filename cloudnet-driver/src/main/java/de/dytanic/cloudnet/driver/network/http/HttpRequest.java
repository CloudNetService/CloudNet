package de.dytanic.cloudnet.driver.network.http;

import java.util.List;
import java.util.Map;

public interface HttpRequest extends HttpMessage<HttpRequest> {

    Map<String, String> pathParameters();

    String path();

    String uri();

    String method();

    Map<String, List<String>> queryParameters();

}